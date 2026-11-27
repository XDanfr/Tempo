-- One row per shared snapshot. The server never sees keys or plaintext.
CREATE TABLE IF NOT EXISTS shares (
  id TEXT PRIMARY KEY,          -- 128 random bits, base64url
  rev INTEGER NOT NULL,         -- increases on every owner update
  body BLOB,                    -- AES-256-GCM ciphertext (null once deleted)
  size INTEGER NOT NULL,
  owner_hash TEXT NOT NULL,     -- SHA-256 of the owner token
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  expires_at INTEGER NOT NULL,  -- 180 days after the last update
  deleted_at INTEGER            -- tombstone kept 30 days so devices learn it's gone
);
CREATE INDEX IF NOT EXISTS shares_expiry ON shares (expires_at);
CREATE INDEX IF NOT EXISTS shares_deleted ON shares (deleted_at);

-- Optional passkey accounts. Profiles are stored encrypted; the server never sees names.
ALTER TABLE shares ADD COLUMN account_id TEXT;
CREATE INDEX IF NOT EXISTS shares_account ON shares (account_id);
CREATE TABLE IF NOT EXISTS accounts (
  id TEXT PRIMARY KEY,          -- 128 random bits, also the WebAuthn user handle
  profile BLOB,                 -- AES-256-GCM ciphertext of the profile, key stays on devices
  profile_rev INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS passkeys (
  id TEXT PRIMARY KEY,          -- credential ID, base64url
  account_id TEXT NOT NULL,
  public_key TEXT NOT NULL,     -- JWK of the credential's public key
  alg INTEGER NOT NULL,         -- COSE algorithm: -7 (ES256) or -257 (RS256)
  sign_count INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS passkeys_account ON passkeys (account_id);
CREATE TABLE IF NOT EXISTS sessions (
  token_hash TEXT PRIMARY KEY,  -- SHA-256 of the session token
  account_id TEXT NOT NULL,
  expires_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS sessions_account ON sessions (account_id);
CREATE TABLE IF NOT EXISTS challenges (
  id TEXT PRIMARY KEY,
  challenge TEXT NOT NULL,      -- base64url, single use
  user_id TEXT,                 -- set for sign-up
  expires_at INTEGER NOT NULL
);

-- Encrypted cloud copy of each account's timetables. The vault key is wrapped per passkey (via the
-- passkey's PRF secret) or by a recovery code; the server can unwrap neither.
ALTER TABLE passkeys ADD COLUMN wrapped_vault TEXT;
ALTER TABLE accounts ADD COLUMN recovery_vault TEXT;
CREATE TABLE IF NOT EXISTS vaults (
  account_id TEXT PRIMARY KEY,
  rev INTEGER NOT NULL,
  body BLOB NOT NULL,           -- AES-256-GCM ciphertext of the collection
  updated_at INTEGER NOT NULL
);

-- Friends. Invites are links whose #fragment carries the inviter's profile key; requests carry the requester's
-- profile key encrypted to the inviter. The server sees only account IDs and ciphertext.
CREATE TABLE IF NOT EXISTS invites (
  id TEXT PRIMARY KEY,
  account_id TEXT NOT NULL,
  expires_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS friend_requests (
  id TEXT PRIMARY KEY,
  from_account TEXT NOT NULL,
  to_account TEXT NOT NULL,
  payload TEXT NOT NULL,        -- requester's profile key, encrypted with the inviter's profile key
  created_at INTEGER NOT NULL,
  UNIQUE (from_account, to_account)
);
CREATE TABLE IF NOT EXISTS friendships (
  account_id TEXT NOT NULL,
  friend_id TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  PRIMARY KEY (account_id, friend_id)
);
