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
  if (!token) return null;
  const row = await env.DB.prepare("SELECT owner_hash, rev, deleted_at FROM shares WHERE id = ?").bind(id).first();
  if (!row || row.deleted_at) return null;
  return row.owner_hash === await sha256(token) ? row : null;
}

async function api(request, env, url) {
  const parts = url.pathname.split("/").filter(Boolean); // ["api", "t", id?]
  const id = parts[2];
  if (parts[1] !== "t") return problem(404, "Not found");
  const now = Date.now();

  if (!id && request.method === "POST") {
    const body = await readBody(request);
    if (!body) return problem(413, "Snapshot missing or larger than 64 KB");
    const newId = random(16), token = random(32);
    await env.DB.prepare("INSERT INTO shares (id, rev, body, size, owner_hash, created_at, updated_at, expires_at) VALUES (?, 1, ?, ?, ?, ?, ?, ?)")
      .bind(newId, body, body.length, await sha256(token), now, now, now + LIFETIME).run();
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
  const prints = (env.ANDROID_CERT_FINGERPRINTS || "").split(",").map((s) => s.trim()).filter(Boolean);
  return json([{ relation: ["delegate_permission/common.handle_all_urls"],
    target: { namespace: "android_app", package_name: "cc.xdan.tempo", sha256_cert_fingerprints: prints } }], 200, { "cache-control": "max-age=3600" });
}

// Read-only viewer. Decrypts in the browser with the key from the #fragment; no third-party scripts.
const viewer = (id) => `<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="referrer" content="no-referrer"><meta name="robots" content="noindex"><title>Tempo timetable</title>
<style>:root{color-scheme:light dark;--bg:#f6faf3;--fg:#1a1c19;--muted:#5c6158;--card:#e6efe0;--accent:#386a25}
@media(prefers-color-scheme:dark){:root{--bg:#121510;--fg:#e2e5dc;--muted:#a9b0a3;--card:#1f261c;--accent:#b7f397}}
body{margin:0;font:16px/1.4 system-ui,sans-serif;background:var(--bg);color:var(--fg)}main{max-width:720px;margin:auto;padding:24px 16px}
h1{margin:0 0 4px}p{color:var(--muted)}.day{margin:20px 0 8px;font-weight:700}.b{display:flex;gap:12px;align-items:center;padding:12px 14px;border-radius:18px;margin:6px 0;color:#1a1c19}
.t{font-variant-numeric:tabular-nums;font-size:14px;opacity:.85}a.btn{display:inline-block;margin-top:12px;padding:12px 20px;border-radius:999px;background:var(--accent);color:var(--bg);text-decoration:none;font-weight:600}</style></head>
<body><main><h1 id="title">Opening timetable…</h1><p id="sub">This timetable is end-to-end encrypted. It's decrypted here in your browser.</p>
<a class="btn" id="open" href="tempo://t/${id}">Open in Tempo</a><div id="week"></div></main>
<script>(async()=>{const id=${JSON.stringify(id)};const k=new URLSearchParams(location.hash.slice(1)).get("k");
const title=document.getElementById("title"),sub=document.getElementById("sub");document.getElementById("open").href="tempo://t/"+id+location.hash;
const fail=(m)=>{title.textContent="Can't show this timetable";sub.textContent=m};if(!k)return fail("This link is missing its key. Ask for the full link.");
const res=await fetch("/api/t/"+id);if(!res.ok)return fail(res.status===410?"The owner stopped sharing this timetable.":"This link has expired or doesn't exist.");
const data=new Uint8Array(await res.arrayBuffer());const raw=Uint8Array.from(atob(k.replace(/-/g,"+").replace(/_/g,"/")),c=>c.charCodeAt(0));
let s;try{const key=await crypto.subtle.importKey("raw",raw,"AES-GCM",false,["decrypt"]);
s=JSON.parse(new TextDecoder().decode(await crypto.subtle.decrypt({name:"AES-GCM",iv:data.slice(0,12)},key,data.slice(12))))}catch(e){return fail("This link's key doesn't match. Ask for the full link.")}
const t=s.timetable.timetable||s.timetable;title.textContent=(s.sharer?s.sharer+"'s ":"")+t.name;sub.textContent="Shared from Tempo. Open it in the app to compare frees.";
const subj=Object.fromEntries((t.subjects||[]).map(x=>[x.id,x]));const days=["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"];
const hm=m=>String(Math.floor(m/60)).padStart(2,"0")+":"+String(m%60).padStart(2,"0");const hex=c=>"#"+((Number(c)>>>0)&0xffffff).toString(16).padStart(6,"0");
const week=document.getElementById("week");for(let d=1;d<=7;d++){const ss=(t.sessions||[]).filter(x=>x.day===d).sort((a,b)=>a.time.start-b.time.start);if(!ss.length)continue;
const h=document.createElement("div");h.className="day";h.textContent=days[d-1];week.append(h);for(const x of ss){const sj=subj[x.subjectId]||{name:"Session",colour:0xffb7f397};
const e=document.createElement("div");e.className="b";e.style.background=hex(sj.colour);const n=document.createElement("strong");n.textContent=sj.name;
const tm=document.createElement("span");tm.className="t";tm.textContent=hm(x.time.start)+"–"+hm(x.time.end)+(x.cycleWeek?" · week "+x.cycleWeek:"");e.append(n,tm);week.append(e)}}})()</script></body></html>`;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname.startsWith("/api/")) {
      try { return await api(request, env, url); } catch (e) { return problem(503, "Sharing is busy. Try again later."); }
    }
    if (url.pathname === "/.well-known/assetlinks.json") return assetLinks(env);
    const view = url.pathname.match(/^\/t\/([A-Za-z0-9_-]{22})$/);
    if (view) return new Response(viewer(view[1]), { headers: { "content-type": "text/html; charset=utf-8",
      "content-security-policy": "default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; base-uri 'none'; form-action 'none'",
      "referrer-policy": "no-referrer", "x-content-type-options": "nosniff" } });
    return new Response("Tempo shares timetables privately. Get Tempo at https://github.com/XDanfr/Tempo", { status: 404 });
  },
  // Daily: remove expired snapshots and old tombstones.
  async scheduled(event, env) {
    const now = Date.now();
    await env.DB.prepare("DELETE FROM shares WHERE (deleted_at IS NULL AND expires_at < ?) OR (deleted_at IS NOT NULL AND deleted_at < ?)").bind(now, now - TOMBSTONE).run();
  },
};
