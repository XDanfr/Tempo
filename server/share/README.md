# Tempo share service

A tiny Cloudflare Worker that stores **end-to-end encrypted** timetable snapshots for share links (`https://tempo.xdan.cc/t/<id>#k=<key>`). It never receives keys or plaintext: the app encrypts with AES-256-GCM and the key stays in the link's `#fragment`. See [Social](../../docs/SOCIAL.md) for the privacy model and free-tier budget.

## What it stores

One D1 row per link: a random ID, revision, ciphertext (≤ 64 KB), size, a SHA-256 hash of the owner token, timestamps and an expiry 180 days after the last update. Deleting a link clears the ciphertext immediately and keeps a tombstone for 30 days so other devices learn it's gone. A daily cron removes expired rows. Nothing else is logged by the Worker.

## Routes

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/t` | Create a link from ciphertext; returns `id` and a one-time `ownerToken` |
| `GET`/`HEAD` | `/api/t/:id` | Ciphertext with an `ETag` revision; `304` when unchanged, `410` when deleted |
| `PUT` | `/api/t/:id` | New revision (owner token required) |
| `DELETE` | `/api/t/:id` | Stop sharing (owner token required) |
| `GET` | `/t/:id` | Read-only web viewer that decrypts in the browser |
| `GET` | `/.well-known/assetlinks.json` | Android App Links for `cc.xdan.tempo` |

## Deploy (Workers Free plan)

```sh
npx wrangler d1 create tempo-share            # copy the database_id into wrangler.toml
npx wrangler d1 execute tempo-share --remote --file=schema.sql
# Set ANDROID_CERT_FINGERPRINTS in wrangler.toml to the release certificate's SHA-256 fingerprint
npx wrangler deploy
```

The custom domain `tempo.xdan.cc` must be on the same Cloudflare account. On the Free plan, D1 stops at its daily limits instead of billing; the app shows "Sharing is busy" and keeps working offline.
