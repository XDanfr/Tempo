// Tempo share Worker. Stores opaque ciphertext; keys live only in link fragments and on devices.
const MAX_BODY = 64 * 1024;
const DAY = 86_400_000;
const LIFETIME = 180 * DAY;
const TOMBSTONE = 30 * DAY;
const ID = /^[A-Za-z0-9_-]{22}$/;

const b64url = (bytes) => btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const random = (n) => b64url(crypto.getRandomValues(new Uint8Array(n)));
async function sha256(text) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}
const json = (data, status = 200, headers = {}) => new Response(JSON.stringify(data), {
  status, headers: { "content-type": "application/json", "cache-control": "no-store", ...headers } });
const problem = (status, message) => json({ error: message }, status);

async function readBody(request) {
  const length = Number(request.headers.get("content-length") || 0);
  if (length > MAX_BODY) return null;
  const body = new Uint8Array(await request.arrayBuffer());
  return body.length > 0 && body.length <= MAX_BODY ? body : null;
}

async function owner(env, request, id) {
  const token = (request.headers.get("authorization") || "").replace(/^Bearer /, "");
  const row = await env.DB.prepare("SELECT owner_hash, rev, deleted_at, account_id FROM shares WHERE id = ?").bind(id).first();
  if (!row || row.deleted_at) return null;
  if (token && row.owner_hash === await sha256(token)) return row;
  // Account-owned links can also be managed with the account's session.
  const signedIn = row.account_id ? await account(env, request) : null;
  return signedIn && signedIn === row.account_id ? row : null;
}

// ---- Passkey accounts (WebAuthn). Only public keys, encrypted profiles and hashed session tokens are stored. ----
const RP_ID = "tempo.xdan.cc";
const SESSION_LIFETIME = 365 * DAY;
const CHALLENGE_LIFETIME = 5 * 60_000;
const fromB64url = (text) => Uint8Array.from(atob(text.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((text.length + 3) % 4)), (c) => c.charCodeAt(0));
const concat = (...parts) => { const out = new Uint8Array(parts.reduce((n, p) => n + p.length, 0)); let i = 0; for (const p of parts) { out.set(p, i); i += p.length; } return out; };
const digest = async (bytes) => new Uint8Array(await crypto.subtle.digest("SHA-256", bytes));
const equal = (a, b) => a.length === b.length && a.every((v, i) => v === b[i]);

/** Minimal CBOR decoder: enough for WebAuthn attestation objects and COSE keys. */
function cbor(bytes) {
  let i = 0;
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const length = (info) => {
    if (info < 24) return info;
    if (info === 24) return bytes[i++];
    if (info === 25) { const v = view.getUint16(i); i += 2; return v; }
    if (info === 26) { const v = view.getUint32(i); i += 4; return v; }
    if (info === 27) { const v = Number(view.getBigUint64(i)); i += 8; return v; }
    throw new Error("Unsupported CBOR length");
  };
  const item = () => {
    const head = bytes[i++], major = head >> 5, info = head & 31;
    if (major === 7) { if (info === 20) return false; if (info === 21) return true; if (info === 22) return null; throw new Error("Unsupported CBOR simple value"); }
    const n = length(info);
    switch (major) {
      case 0: return n;
      case 1: return -1 - n;
      case 2: { const v = bytes.slice(i, i + n); i += n; return v; }
      case 3: { const v = new TextDecoder().decode(bytes.slice(i, i + n)); i += n; return v; }
      case 4: return Array.from({ length: n }, item);
      case 5: { const m = new Map(); for (let k = 0; k < n; k++) m.set(item(), item()); return m; }
      default: throw new Error("Unsupported CBOR type");
    }
  };
  const value = item();
  return { value, end: i };
}

/** Parses authenticator data; with attested credential data when present. */
function authData(bytes) {
  const flags = bytes[32];
  const result = { rpIdHash: bytes.slice(0, 32), up: !!(flags & 1), uv: !!(flags & 4), signCount: new DataView(bytes.buffer, bytes.byteOffset).getUint32(33) };
  if (flags & 64) {
    const idLength = (bytes[53] << 8) | bytes[54];
    result.credentialId = bytes.slice(55, 55 + idLength);
    result.coseKey = cbor(bytes.slice(55 + idLength)).value;
  }
  return result;
}

/** COSE public key → JWK and algorithm. ES256 and RS256 only. */
function coseToJwk(key) {
  const alg = key.get(3);
  if (alg === -7 && key.get(1) === 2 && key.get(-1) === 1) return { alg, jwk: { kty: "EC", crv: "P-256", x: b64url(key.get(-2)), y: b64url(key.get(-3)), ext: true } };
  if (alg === -257 && key.get(1) === 3) return { alg, jwk: { kty: "RSA", n: b64url(key.get(-1)), e: b64url(key.get(-2)), alg: "RS256", ext: true } };
  throw new Error("Unsupported passkey algorithm");
}

/** DER ECDSA signature → raw r||s, as WebCrypto expects. */
function derToRaw(der) {
  let i = 2;
  const part = () => { i++; let len = der[i++]; let v = der.slice(i, i + len); i += len; while (v.length > 32 && v[0] === 0) v = v.slice(1); const out = new Uint8Array(32); out.set(v, 32 - v.length); return out; };
  return concat(part(), part());
}

/** Origins allowed to use the passkey: the Android app(s), by signing certificate, and the website. */
function allowedOrigins(env) {
  const prints = (env.ANDROID_CERT_FINGERPRINTS || "").split(",").concat((env.QA_CERT_FINGERPRINTS || "").split(",")).map((s) => s.trim()).filter(Boolean);
  return new Set([`https://${RP_ID}`, ...prints.map((p) => "android:apk-key-hash:" + b64url(Uint8Array.from(p.split(":"), (h) => parseInt(h, 16))))]);
}

async function takeChallenge(env, id) {
  const row = await env.DB.prepare("SELECT challenge, user_id, expires_at FROM challenges WHERE id = ?").bind(id || "").first();
  if (!row) return null;
  await env.DB.prepare("DELETE FROM challenges WHERE id = ?").bind(id).run();
  return row.expires_at < Date.now() ? null : row;
}

/** Checks clientDataJSON: type, single-use challenge and origin. Returns its SHA-256. */
async function checkClientData(env, encoded, type, challenge) {
  const raw = fromB64url(encoded);
  const data = JSON.parse(new TextDecoder().decode(raw));
  if (data.type !== type) throw new Error("Wrong ceremony");
  if (data.challenge !== challenge) throw new Error("Challenge mismatch");
  if (!allowedOrigins(env).has(data.origin)) throw new Error("Origin not allowed: " + data.origin);
  return digest(raw);
}

async function newSession(env, accountId) {
  const token = random(32);
  await env.DB.prepare("INSERT INTO sessions (token_hash, account_id, expires_at) VALUES (?, ?, ?)").bind(await sha256(token), accountId, Date.now() + SESSION_LIFETIME).run();
  return token;
}

/** The signed-in account for a request, from its session token. */
async function account(env, request) {
  const token = request.headers.get("x-tempo-session");
  if (!token) return null;
  const row = await env.DB.prepare("SELECT account_id, expires_at FROM sessions WHERE token_hash = ?").bind(await sha256(token)).first();
  return row && row.expires_at > Date.now() ? row.account_id : null;
}

async function auth(request, env, step) {
  const now = Date.now();
  if (request.method !== "POST") return problem(405, "Method not allowed");
  const body = await request.json().catch(() => ({}));
  if (step === "challenge") {
    const id = random(16), challenge = random(32), userId = body.kind === "register" ? random(16) : null;
    await env.DB.batch([
      env.DB.prepare("DELETE FROM challenges WHERE expires_at < ?").bind(now),
      env.DB.prepare("INSERT INTO challenges (id, challenge, user_id, expires_at) VALUES (?, ?, ?, ?)").bind(id, challenge, userId, now + CHALLENGE_LIFETIME),
    ]);
    return json({ challengeId: id, challenge, rpId: RP_ID, userId });
  }
  const pending = await takeChallenge(env, body.challengeId);
  if (!pending) return problem(400, "Sign-in took too long. Try again.");
  const response = body.response || {};
  try {
    if (step === "register" && pending.user_id) {
      await checkClientData(env, response.clientDataJSON, "webauthn.create", pending.challenge);
      const attestation = cbor(fromB64url(response.attestationObject)).value;
      const data = authData(attestation.get("authData"));
      if (!equal(data.rpIdHash, await digest(new TextEncoder().encode(RP_ID))) || !data.up || !data.credentialId) throw new Error("Bad authenticator data");
      const { alg, jwk } = coseToJwk(data.coseKey);
      await env.DB.batch([
        env.DB.prepare("INSERT INTO accounts (id, created_at) VALUES (?, ?)").bind(pending.user_id, now),
        env.DB.prepare("INSERT INTO passkeys (id, account_id, public_key, alg, sign_count, created_at) VALUES (?, ?, ?, ?, ?, ?)")
          .bind(b64url(data.credentialId), pending.user_id, JSON.stringify(jwk), alg, data.signCount, now),
      ]);
      return json({ accountId: pending.user_id, session: await newSession(env, pending.user_id) }, 201);
    }
    if (step === "login" && !pending.user_id) {
      const key = await env.DB.prepare("SELECT account_id, public_key, alg, sign_count FROM passkeys WHERE id = ?").bind(body.credentialId || response.id || "").first();
      if (!key) return problem(404, "This passkey isn't linked to a Tempo account any more.");
      const clientHash = await checkClientData(env, response.clientDataJSON, "webauthn.get", pending.challenge);
      const raw = fromB64url(response.authenticatorData);
      const data = authData(raw);
      if (!equal(data.rpIdHash, await digest(new TextEncoder().encode(RP_ID))) || !data.up) throw new Error("Bad authenticator data");
      const es = key.alg === -7;
      const publicKey = await crypto.subtle.importKey("jwk", JSON.parse(key.public_key), es ? { name: "ECDSA", namedCurve: "P-256" } : { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
      const signature = fromB64url(response.signature);
      const ok = await crypto.subtle.verify(es ? { name: "ECDSA", hash: "SHA-256" } : { name: "RSASSA-PKCS1-v1_5" }, publicKey, es ? derToRaw(signature) : signature, concat(raw, clientHash));
      if (!ok) throw new Error("Signature check failed");
      if (data.signCount > 0 && data.signCount <= key.sign_count) throw new Error("Passkey counter went backwards");
      await env.DB.prepare("UPDATE passkeys SET sign_count = ? WHERE id = ?").bind(data.signCount, body.credentialId || response.id).run();
      return json({ accountId: key.account_id, session: await newSession(env, key.account_id) });
    }
  } catch (e) {
    return problem(400, "Couldn't verify the passkey. " + e.message);
  }
  return problem(400, "Unknown step");
}

async function me(request, env, part) {
  const id = await account(env, request);
  if (!id) return problem(401, "Signed out");
  const now = Date.now();
  if (!part && request.method === "GET") {
    const row = await env.DB.prepare("SELECT profile, profile_rev, created_at FROM accounts WHERE id = ?").bind(id).first();
    if (!row) return problem(401, "Signed out");
    const shares = await env.DB.prepare("SELECT id FROM shares WHERE account_id = ? AND deleted_at IS NULL").bind(id).all();
    return json({ accountId: id, createdAt: row.created_at, profileRev: row.profile_rev,
      profile: row.profile ? b64url(new Uint8Array(row.profile)) : null, shares: shares.results.map((r) => r.id) });
  }
  if (part === "profile" && request.method === "PUT") {
    const body = await readBody(request);
    if (!body || body.length > 8 * 1024) return problem(413, "Profile missing or too large");
    await env.DB.prepare("UPDATE accounts SET profile = ?, profile_rev = profile_rev + 1 WHERE id = ?").bind(body, id).run();
    return new Response(null, { status: 204 });
  }
  if (part === "session" && request.method === "DELETE") {
    await env.DB.prepare("DELETE FROM sessions WHERE token_hash = ?").bind(await sha256(request.headers.get("x-tempo-session"))).run();
    return new Response(null, { status: 204 });
  }
  if (!part && request.method === "DELETE") {
    // Everything goes now; owned links become tombstones so other devices remove their copies.
    await env.DB.batch([
      env.DB.prepare("UPDATE shares SET body = NULL, size = 0, deleted_at = ?, account_id = NULL WHERE account_id = ? AND deleted_at IS NULL").bind(now, id),
      env.DB.prepare("DELETE FROM passkeys WHERE account_id = ?").bind(id),
      env.DB.prepare("DELETE FROM sessions WHERE account_id = ?").bind(id),
      env.DB.prepare("DELETE FROM accounts WHERE id = ?").bind(id),
    ]);
    return new Response(null, { status: 204 });
  }
  return problem(405, "Method not allowed");
}

async function api(request, env, url) {
  const parts = url.pathname.split("/").filter(Boolean); // ["api", "t", id?]
  if (parts[1] === "auth") return auth(request, env, parts[2]);
  if (parts[1] === "me") return me(request, env, parts[2]);
  const id = parts[2];
  if (parts[1] !== "t") return problem(404, "Not found");
  const now = Date.now();

  if (!id && request.method === "POST") {
    const body = await readBody(request);
    if (!body) return problem(413, "Snapshot missing or larger than 64 KB");
    const newId = random(16), token = random(32);
    // Signed in: the link also belongs to the account, so any of its devices can manage it.
    await env.DB.prepare("INSERT INTO shares (id, rev, body, size, owner_hash, created_at, updated_at, expires_at, account_id) VALUES (?, 1, ?, ?, ?, ?, ?, ?, ?)")
      .bind(newId, body, body.length, await sha256(token), now, now, now + LIFETIME, await account(env, request)).run();
    return json({ id: newId, ownerToken: token, rev: 1 }, 201);
  }
  if (!id || !ID.test(id)) return problem(404, "Not found");

  if (request.method === "GET" || request.method === "HEAD") {
    const row = await env.DB.prepare("SELECT rev, body, deleted_at, expires_at FROM shares WHERE id = ?").bind(id).first();
    if (!row || (!row.deleted_at && row.expires_at < now)) return problem(404, "This link has expired or never existed");
    if (row.deleted_at) return problem(410, "The owner stopped sharing this timetable");
    const etag = `"${row.rev}"`;
    if (request.headers.get("if-none-match") === etag) return new Response(null, { status: 304, headers: { etag } });
    // D1 returns BLOBs as an array of numbers or an ArrayBuffer depending on the runtime; send real bytes.
    return new Response(request.method === "HEAD" ? null : new Uint8Array(row.body), {
      headers: { "content-type": "application/octet-stream", etag, "x-tempo-rev": String(row.rev), "cache-control": "no-cache" } });
  }
  if (request.method === "PUT") {
    const row = await owner(env, request, id);
    if (!row) return problem(403, "Not the owner of this link");
    const body = await readBody(request);
    if (!body) return problem(413, "Snapshot missing or larger than 64 KB");
    const rev = row.rev + 1;
    await env.DB.prepare("UPDATE shares SET body = ?, size = ?, rev = ?, updated_at = ?, expires_at = ? WHERE id = ?")
      .bind(body, body.length, rev, now, now + LIFETIME, id).run();
    return json({ id, rev });
  }
  if (request.method === "DELETE") {
    if (!await owner(env, request, id)) return problem(403, "Not the owner of this link");
    await env.DB.prepare("UPDATE shares SET body = NULL, size = 0, deleted_at = ? WHERE id = ?").bind(now, id).run();
    return new Response(null, { status: 204 });
  }
  return problem(405, "Method not allowed");
}

function assetLinks(env) {
  const list = (v) => (v || "").split(",").map((s) => s.trim()).filter(Boolean);
  // App links and passkeys (get_login_creds) for the release app; passkeys only for QA builds.
  const links = [{ relation: ["delegate_permission/common.handle_all_urls", "delegate_permission/common.get_login_creds"],
    target: { namespace: "android_app", package_name: "cc.xdan.tempo", sha256_cert_fingerprints: list(env.ANDROID_CERT_FINGERPRINTS) } }];
  if (list(env.QA_CERT_FINGERPRINTS).length) links.push({ relation: ["delegate_permission/common.get_login_creds"],
    target: { namespace: "android_app", package_name: "cc.xdan.tempo.qa", sha256_cert_fingerprints: list(env.QA_CERT_FINGERPRINTS) } });
  return json(links, 200, { "cache-control": "max-age=3600" });
}

// Read-only viewer in the xdan.cc/tempo style. Decrypts in the browser with the key from the #fragment;
// the only other request is the Outfit font from xdan.cc.
const LOGO = "data:image/svg+xml,%3csvg%20xmlns='http://www.w3.org/2000/svg'%20viewBox='0%200%20108%20108'%3e%3cpath%20fill='%23142718'%20d='M24%200h60q24%200%2024%2024v60q0%2024-24%2024H24Q0%20108%200%2084V24Q0%200%2024%200Z'/%3e%3cpath%20fill='%23B7F397'%20d='M35%2030h4q5%200%205%205v38q0%205-5%205h-4q-5%200-5-5V35q0-5%205-5Z'/%3e%3cpath%20fill='%23B7F397'%20d='M52%2030h4q5%200%205%205v11q0%205-5%205h-4q-5%200-5-5V35q0-5%205-5Z'/%3e%3cpath%20fill='%23B7F397'%20d='M69%2030h4q5%200%205%205v24q0%205-5%205h-4q-5%200-5-5V35q0-5%205-5Z'/%3e%3cpath%20fill='%2379B89D'%20d='M52%2055h4q5%200%205%205v13q0%205-5%205h-4q-5%200-5-5V60q0-5%205-5Z'/%3e%3cpath%20fill='%2379B89D'%20d='M69%2068h4q5%200%205%205v0q0%205-5%205h-4q-5%200-5-5v0q0-5%205-5Z'/%3e%3c/svg%3e";
const INSTALL = "https://xdan.cc/tempo/releases/";
const viewer = (id) => `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="referrer" content="no-referrer"><meta name="robots" content="noindex"><meta name="theme-color" content="#0b0f0a"><title>Tempo timetable</title>
<link rel="icon" href="${LOGO}">
<style>@font-face{font-family:Outfit;font-style:normal;font-weight:100 900;font-display:swap;src:url(https://xdan.cc/assets/outfit-variable-RlK29BP0.woff2) format("woff2")}
:root{color-scheme:dark;--bg:#0b0f0a;--surface:#11160f;--container:#1a2117;--high:#232b20;--fg:#e2e5dc;--muted:#a9b0a3;--green:#b7f397;--on-green:#0f2a06;--mint:#79b89d;--spring:cubic-bezier(.34,1.56,.64,1)}
*{box-sizing:border-box}html{background:var(--bg)}body{margin:0;font-family:Outfit,system-ui,sans-serif;color:var(--fg);background:var(--bg);-webkit-font-smoothing:antialiased}
main{max-width:640px;margin:0 auto;padding:16px}
.hero{position:relative;overflow:hidden;padding:36px 28px 32px;border:1px solid var(--container);border-radius:40px;background:var(--surface);animation:rise .7s var(--spring) both}
.shape{position:absolute;right:-60px;top:-60px;width:220px;height:220px;color:var(--container);animation:spin 28s linear infinite;pointer-events:none}
.brand{position:relative;display:flex;gap:14px;align-items:center}.brand img{width:56px;height:56px;border-radius:16px}
.brand b{display:block;font-size:26px;font-weight:600;letter-spacing:-.04em}.brand small{color:var(--muted);font-size:13px}
h1{position:relative;margin:32px 0 14px;font-size:clamp(40px,11vw,64px);font-weight:500;line-height:1;letter-spacing:-.055em}h1 span{color:var(--green)}
.lead{position:relative;margin:0;color:var(--muted);font-size:17px;line-height:1.55}
.actions{position:relative;display:flex;flex-wrap:wrap;gap:10px;margin-top:24px}
.btn{display:inline-flex;align-items:center;gap:8px;height:52px;padding:0 24px;border-radius:999px;font:600 16px Outfit,system-ui,sans-serif;text-decoration:none;transition:transform .35s var(--spring),border-radius .35s var(--spring)}
.btn:active{transform:scale(.95);border-radius:16px}.btn.filled{background:var(--green);color:var(--on-green)}.btn.outlined{border:1px solid var(--high);color:var(--green)}
.btn svg{width:20px;height:20px;fill:currentColor}.small{position:relative;margin:14px 0 0;color:var(--muted);font-size:13px}
.week{margin-top:12px;padding:8px 20px 20px;border-radius:32px;background:var(--surface);border:1px solid var(--container)}
.day{margin:20px 4px 10px;font-size:15px;font-weight:600;letter-spacing:.02em;color:var(--muted);text-transform:uppercase}
.b{display:flex;justify-content:space-between;gap:12px;align-items:center;padding:14px 18px;border-radius:22px;margin:8px 0;color:#14200f;animation:rise .6s var(--spring) both}
.b strong{font-weight:600;font-size:17px}.t{font-variant-numeric:tabular-nums;font-size:14px;opacity:.8;white-space:nowrap}
.wave{display:block;width:100%;height:12px;margin:22px 0 6px;color:var(--green)}.wave path{animation:wave 1.4s linear infinite}
.empty{color:var(--muted);text-align:center;padding:24px}
footer{margin:22px 0 32px;text-align:center;color:var(--muted);font-size:13px}footer a{color:var(--green)}
@keyframes spin{to{transform:rotate(360deg)}}@keyframes rise{from{opacity:0;transform:translateY(16px) scale(.98)}}@keyframes wave{to{transform:translateX(-24px)}}
@media(prefers-reduced-motion:reduce){*{animation:none!important;transition:none!important}}</style></head>
<body><main>
<section class="hero"><svg class="shape" viewBox="0 0 100 100" aria-hidden="true"><path fill="currentColor" d="M50 2l7.6 9.3 11.6-3.4 2.7 11.8 11.8 2.7-3.4 11.6L98 42.4 90.7 50l7.3 7.6-9.3 7.6 3.4 11.6-11.8 2.7-2.7 11.8-11.6-3.4L50 98l-7.6-9.3-11.6 3.4-2.7-11.8-11.8-2.7 3.4-11.6L2 57.6 9.3 50 2 42.4l9.3-7.6-3.4-11.6 11.8-2.7 2.7-11.8 11.6 3.4z"/></svg>
<div class="brand"><img src="${LOGO}" alt=""><div><b>Tempo</b><small>Made by XDan</small></div></div>
<h1 id="title">Opening a<br><span>timetable…</span></h1>
<p class="lead" id="sub">It's end-to-end encrypted, so it's being decrypted here in your browser.</p>
<div class="actions"><a class="btn filled" id="open" href="tempo://t/${id}"><svg viewBox="0 0 24 24"><path d="M19 19H5V5h7V3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7h-2v7ZM14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7Z"/></svg>Open in Tempo</a>
<a class="btn outlined" href="${INSTALL}"><svg viewBox="0 0 24 24"><path d="M12 16 7 11l1.4-1.45 2.6 2.6V4h2v8.15l2.6-2.6L17 11l-5 5Zm-6 4q-.83 0-1.41-.59Q4 18.83 4 18v-3h2v3h12v-3h2v3q0 .83-.59 1.41Q18.83 20 18 20Z"/></svg>Install Tempo</a></div>
<p class="small">Free on Android 8.0+ · Your timetables stay private</p></section>
<section class="week" id="week" aria-live="polite"><svg class="wave" viewBox="0 0 600 12" preserveAspectRatio="none" aria-hidden="true"><path fill="none" stroke="currentColor" stroke-width="4" stroke-linecap="round" d="M0 6q6-6 12 0t12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0 12 0"/></svg></section>
<footer>Shared privately with <a href="https://xdan.cc/tempo">Tempo</a>. Tempo's server can't read it.</footer></main>
<script>(async()=>{const id=${JSON.stringify(id)};const k=new URLSearchParams(location.hash.slice(1)).get("k");
const title=document.getElementById("title"),sub=document.getElementById("sub"),week=document.getElementById("week");document.getElementById("open").href="tempo://t/"+id+location.hash;
const heading=(a,b)=>{title.textContent="";title.append(a);title.append(document.createElement("br"));const s=document.createElement("span");s.textContent=b;title.append(s)};
const fail=(m)=>{heading("Can't show","this timetable");sub.textContent=m;week.innerHTML='<p class="empty">Ask the person who shared it for a new link.</p>'};
if(!k)return fail("This link is missing its key. Ask for the full link, including the part after #.");
const res=await fetch("/api/t/"+id);if(!res.ok)return fail(res.status===410?"The owner stopped sharing this timetable.":"This link has expired or doesn't exist.");
const data=new Uint8Array(await res.arrayBuffer());const raw=Uint8Array.from(atob(k.replace(/-/g,"+").replace(/_/g,"/")),c=>c.charCodeAt(0));
let s;try{const key=await crypto.subtle.importKey("raw",raw,"AES-GCM",false,["decrypt"]);
s=JSON.parse(new TextDecoder().decode(await crypto.subtle.decrypt({name:"AES-GCM",iv:data.slice(0,12)},key,data.slice(12))))}catch(e){return fail("This link's key doesn't match. Ask for the full link.")}
const t=s.timetable.timetable||s.timetable;heading(s.sharer?s.sharer+"'s":"Shared",t.name);document.title=(s.sharer?s.sharer+"'s ":"")+t.name+" · Tempo";
sub.textContent="Open it in Tempo to see when you're both free, or install Tempo to get started.";week.innerHTML="";
const subj=Object.fromEntries((t.subjects||[]).map(x=>[x.id,x]));const days=["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"];
const hm=m=>String(Math.floor(m/60)).padStart(2,"0")+":"+String(m%60).padStart(2,"0");const hex=c=>"#"+((Number(c)>>>0)&0xffffff).toString(16).padStart(6,"0");
let n=0;for(let d=1;d<=7;d++){const ss=(t.sessions||[]).filter(x=>x.day===d).sort((a,b)=>a.time.start-b.time.start);if(!ss.length)continue;
const h=document.createElement("div");h.className="day";h.textContent=days[d-1];week.append(h);for(const x of ss){const sj=subj[x.subjectId]||{name:"Session",colour:0xffb7f397};
const e=document.createElement("div");e.className="b";e.style.background=hex(sj.colour);e.style.animationDelay=(n++*40)+"ms";const nm=document.createElement("strong");nm.textContent=sj.name;
const tm=document.createElement("span");tm.className="t";tm.textContent=hm(x.time.start)+"–"+hm(x.time.end)+(x.cycleWeek?" · week "+x.cycleWeek:"");e.append(nm,tm);week.append(e)}}
if(!n)week.innerHTML='<p class="empty">No sessions in this timetable yet.</p>'})()</script></body></html>`;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname.startsWith("/api/")) {
      try { return await api(request, env, url); } catch (e) { return problem(503, "Sharing is busy. Try again later."); }
    }
    if (url.pathname === "/.well-known/assetlinks.json") return assetLinks(env);
    const view = url.pathname.match(/^\/t\/([A-Za-z0-9_-]{22})$/);
    if (view) return new Response(viewer(view[1]), { headers: { "content-type": "text/html; charset=utf-8",
      "content-security-policy": "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; font-src https://xdan.cc; connect-src 'self'; base-uri 'none'; form-action 'none'",
      "referrer-policy": "no-referrer", "x-content-type-options": "nosniff" } });
    if (url.pathname === "/") return Response.redirect("https://xdan.cc/tempo", 302);
    return new Response("Tempo shares timetables privately. Get Tempo at https://xdan.cc/tempo", { status: 404 });
  },
  // Daily: remove expired snapshots and old tombstones.
  async scheduled(event, env) {
    const now = Date.now();
    await env.DB.batch([
      env.DB.prepare("DELETE FROM shares WHERE (deleted_at IS NULL AND expires_at < ?) OR (deleted_at IS NOT NULL AND deleted_at < ?)").bind(now, now - TOMBSTONE),
      env.DB.prepare("DELETE FROM sessions WHERE expires_at < ?").bind(now),
      env.DB.prepare("DELETE FROM challenges WHERE expires_at < ?").bind(now),
    ]);
  },
};
