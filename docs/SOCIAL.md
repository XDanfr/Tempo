# Social Tempo — plan

Status: **ideas 1–4 shipped (share links in 0.7). Accounts step 1 is live on the server and in QA builds: passkey sign-up and sign-in, an encrypted profile, account-owned links and delete account. Cloud saving (idea 6, now tied to the account) is live: the collection and its share links are encrypted with a vault key unwrapped by the passkey's PRF secret, or by a recovery code where the provider lacks PRF. Next: friends, then groups.** This replaces the sharing and profile sections of [Feel & Flow](FEEL_AND_FLOW.md) and the link format in [Architecture](ARCHITECTURE.md#future-link-sharing). Every feature here stays **optional**: Tempo keeps working fully offline, without an account, and with no data leaving the device unless the person shares it.

## Ground rules

1. **End-to-end encrypted.** Timetables are encrypted on the phone before upload. The server stores ciphertext and can never read names, subjects, rooms or times.
2. **Keys never reach the server.** A share key lives only in the link's `#fragment`, which browsers and apps never send in requests, or on the devices it was shared with.
3. **Minimum metadata.** The server keeps only what it needs: an opaque ID, the ciphertext, a revision number, a size, an expiry and a hashed owner token. No names, emails, phone numbers or analytics. IP addresses aren't stored by the Worker; Cloudflare's own request logs are outside our control and are covered in the privacy notice.
4. **Owner control.** Whoever publishes a timetable can update it, stop sharing it and delete it at any time. Deletion removes the server copy straight away and tells other devices to remove their copy on their next check.
5. **Fits the free tier, with hard stops.** Use only products that stop working when a free limit is reached instead of billing, so Tempo can never cause an unexpected bill (see [Free-tier budget](#free-tier-budget)).
6. **Safe for students.** Rooms and notes are left out of shares by default. Names are display names the person picks, with no real-name requirement. There's no discovery, search or public directory: you can only see timetables someone gave you a link to.

## The ideas, split up

Each idea can ship on its own, in this order. Later ones build on earlier ones.

### 1. Profile name (on device only)

- A **display name** and optional emoji/colour avatar, asked once and editable in Settings. Stored locally.
- Included in anything you share, so others see "Alex" instead of a timetable called "College".
- **No server, no account.** It can ship straight away.

### 2. Proper comparison and "shared free" alerts (on device only)

- The existing Compare screen becomes **People**: your timetable alongside other people's saved on your device (from files today, links later).
- **Week tab overlay:** a toggle adds people's frees to the grid as soft coloured bands, and a summary appears: "You and Alex are both free: Tue 13:10–14:40, Thu 10:50–12:20".
- **Schedule:** when your current or next free overlaps someone's, the Now card and free gap say so: *"Alex also has a free until 14:40"*.
- **Optional notification**, off by default: "Alex also has a free at 13:10", sent once, a few minutes before a shared free starts. It's worked out on the phone from saved copies, with no server and no background network use.
- Reuses the existing comparison engine (shared free = every person has a calculated free in that interval).
- Gamification: an achievement for "first shared free", and a gentle "met up in a shared free" check-in later.

### 3. Share links: `tempo.xdan.cc/t/<id>#<key>`

- **Share** in the timetable switcher creates a link: Tempo encrypts a **share snapshot** (the person chooses what's included: rooms and notes are off by default, the name is on) with a fresh random 256-bit key (AES-256-GCM), uploads the ciphertext, and receives an opaque `id` plus a secret **owner token**.
- The link is `https://tempo.xdan.cc/t/<id>#k=<base64url key>`. The `#k=` part never leaves the device that opens it.
- Opening the link in Tempo (Android App Link) shows a preview, then **Add to People** (a read-only copy) or **Import as my own** (an independent copy, as with today's `.tempo.json`).
- Without Tempo installed, the Worker serves a small static page that decrypts in the browser (with the key from the fragment), shows a read-only week and offers to get the app. The page has no third-party scripts and no tracking.
- **Expiry:** links expire 180 days after their last update unless renewed by the owner's app, which happens automatically while the owner keeps sharing.
- Replaces the earlier `xdan.cc/tempo/share/<code>` design; the old checksum format isn't needed because IDs are random and links are verified by decryption (GCM authentication).

### 4. Live updates and remote removal

- Shared timetables carry a **revision**. When the owner edits the timetable, Tempo re-encrypts it and uploads a new revision under the same `id` with the same key (an owner-token-authenticated PUT).
- People who saved it check for updates when Tempo opens and at most every few hours in the background, using a cheap revision request (`HEAD`/ETag). They only download when something changed.
- **Stop sharing / delete:** the owner's app sends a DELETE with the owner token. The server deletes the ciphertext immediately and keeps an ID tombstone for 30 days. The next time other devices check, they get *gone* and **remove their saved copy automatically**, with a notice: "Alex stopped sharing their timetable".
- **Honest limit:** a device that never comes online again can't be reached, and someone could have screenshotted or exported a copy. The app says this plainly when sharing. Imported "my own" copies are independent by design and not removed; the share sheet makes that difference clear.

### 5. Groups for tracking frees

- A **group** is a named, encrypted list of member share links (for example "Study group").
- The creator generates a group key; the invite link `tempo.xdan.cc/g/<id>#k=<key>` lets members join. Each member adds their own share link to the encrypted member list.
- The Week tab and Schedule can show **"everyone free"** and **"most free"** times for a group, and alerts like *"3 of 4 in Study group are free at 13:10"*.
- Leaving a group removes your entry. The group owner can remove members, rotate the key and delete the group, and deletion propagates as in idea 4.
- Size cap: 30 members per group, to keep reads and payloads small.

### 6. Sync between your own devices (later, optional)

- Uses the same machinery as share links: your timetable collection is uploaded as an encrypted blob with a **sync key** that's shared between your devices by scanning a QR code. No account, email or password is needed.
- Conflict handling starts simple: last write wins per timetable, with the replaced revision kept locally for undo.
- Only after ideas 3–5 are solid.

### 7. Optional Tempo accounts (planned, replaces "not planned")

Accounts make the social side feel integrated: one identity for links, friends and groups, and one place to delete everything. They're **always optional**. Without an account, Tempo works exactly as it does now, share links included.

**What an account is**
- An identity based on a **passkey** (decided November 2026: free, no third party, nothing to remember; Google sign-in may be added later as an optional second button) plus a **profile**: display name, avatar colour/emoji, level and achievements you choose to show.
- **Ownership of your links**: links created while signed in belong to the account, so you can manage, update or delete them from any of your devices.
- **Friends**: when you add someone's link while signed in, you can send a friend request; friends see each other's profile and shared frees without passing links around again.
- **Groups**: an account-owned group with members, invites by link or code, and "everyone free" times (idea 5 becomes simpler: membership lives on the server, timetables stay encrypted).
- **Delete account**: one button removes the profile, friends, group memberships and every link the account owns, and tombstones them so other devices remove their copies on the next check.

**What doesn't change**
- Timetables stay **end-to-end encrypted** with per-link keys. The server stores ciphertext, never names of subjects, rooms or times. Accounts add *who* may read a link's key, not the ability for the server to read it.
- Keys reach friends through the existing link fragment, or wrapped to a friend's public key (each account device generates an X25519 key pair; the private key never leaves the device).
- No public directory, search, feeds or follower counts. You can only find people you already share with.

**Check-ins with friends (later)**
- Once accounts exist, friends can see your check-ins and streak (read-only: only you can change your own), which opens the door to a gentle "nudge" later. Frozen days stay private; friends only see that a day didn't count.

**Server additions (same Worker + D1, free tier)**
- Tables: `accounts` (id, provider subject hash, created_at), `profiles`, `sessions` (hashed tokens, expiry), `devices` (public keys), `friendships`, `groups`, `group_members`, `wrapped_keys` (link id → recipient device → wrapped key).
- Sign-in: passkeys through Android Credential Manager (WebAuthn). The Worker stores only each passkey's public key, verifies sign-in assertions with WebCrypto and issues a session token (stored hashed). Email or SMS codes are ruled out for now: they need a paid sending quota.
- Deletion and retention follow the existing rules: immediate deletion, 30-day tombstones, nothing kept after.

**App changes**
- A **Profile** page (from Home) with sign-in, profile, friends, groups, achievements and "Delete account".
- Link sharing stays where it is; when signed in, new links are account-owned automatically.

**Before building**
- Passkeys need the `assetlinks.json` already published at `tempo.xdan.cc` (relying party `tempo.xdan.cc`). Nothing else to set up.
- Order: sign-in + profile + account-owned links + delete account → friends → groups → device-to-device key wrapping and sync.

## Architecture

```
Tempo app ──HTTPS──▶ Worker (tempo.xdan.cc) ──▶ D1 (SQLite)
   │  encrypt/decrypt locally          │         shares(id, rev, ciphertext, size, owner_hash, expires_at, deleted_at)
   │  key only in #fragment / device   │         groups(id, rev, ciphertext, owner_hash, expires_at, deleted_at)
   └──────────────────────────────────┘
```

- **Worker routes:** `POST /api/t` (create, returns id and owner token), `GET /api/t/:id` (ciphertext and revision, with ETag), `PUT /api/t/:id` (new revision, needs the owner token), `DELETE /api/t/:id` (needs the owner token), with the same set under `/api/g` for groups. `GET /t/:id` serves the static web viewer.
- **Storage: D1.** Ciphertext is small (an encrypted timetable is typically 2–30 KB; the cap is 64 KB) and fits comfortably in a row. D1 on the Workers Free plan **stops at its limits instead of billing**. R2 is not used, because its free tier sits inside a paid product.
- **Owner token:** 256-bit random value returned once at creation. The server stores only its SHA-256 hash. It's kept in the owner's app, and a lost token means the share simply expires.
- **Abuse limits:** 64 KB body cap, per-IP creation limits with Cloudflare's Workers rate-limiting binding, and IDs of 128 random bits so links can't be guessed.
- **Retention:** a daily Cron Trigger deletes expired rows and tombstones older than 30 days. The privacy notice states the exact retention periods.
- **Deletion requests:** besides in-app deletion, a "Delete everything I've shared" button sends DELETE for every owner token on the device. Because the server has no way to identify a person, a request by email can only be honoured with the share link or owner token, which the in-app button handles automatically.

## Free-tier budget

Limits checked against Cloudflare's docs (November 2026), Workers Free plan:

| Resource | Free limit | Tempo's use | Headroom |
| --- | --- | --- | --- |
| Worker requests | 100,000 / day | ~1 update check per opened app per saved person or group (each check reuses an ETag) | ~10k daily active users each following ~5 timetables and opening twice a day; shared GETs are cached at the edge with the Cache API |
| D1 rows read | 5 million / day | 1 row per check | Not the bottleneck |
| D1 rows written | 100,000 / day | 1 row per create, update or delete; edits are batched to at most one upload per 10 minutes per timetable | Comfortable |
| D1 storage | 5 GB total | ~30 KB per share | ~150k live shares |

When a limit is hit, the app shows "Sharing is busy, try again later" and keeps working offline with the copies it has. Nothing is ever billed. If Tempo outgrows this, the first step is a longer update interval, then the $5 paid plan as a deliberate decision.

## Delivery order

1. Profile name (device only).
2. People and shared-free alerts, using imported `.tempo.json` files (device only).
3. Worker and D1 deployed to `tempo.xdan.cc`, with share links, preview and web viewer.
4. Live updates and remote removal.
5. Groups.
6. Device sync.

Nothing is deployed to Cloudflare until the Worker code, privacy notice and retention settings have been reviewed.
