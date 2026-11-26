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
