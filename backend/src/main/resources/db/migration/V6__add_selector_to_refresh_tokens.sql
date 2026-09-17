-- The RefreshTokenService now uses a selector/verifier split.
-- Old tokens are unusable with this scheme, so clear them out.
DELETE FROM refresh_tokens;

-- Add selector column for O(1) indexed lookup
ALTER TABLE refresh_tokens ADD COLUMN selector VARCHAR(255) NOT NULL;
CREATE UNIQUE INDEX uk_refresh_tokens_selector ON refresh_tokens(selector);
