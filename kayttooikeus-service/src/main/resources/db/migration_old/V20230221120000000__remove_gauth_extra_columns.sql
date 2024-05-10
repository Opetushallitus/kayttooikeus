ALTER TABLE google_auth_token
  DROP COLUMN name,
  DROP COLUMN scratch_codes,
  DROP COLUMN validation_code,
  ALTER COLUMN registration_date DROP NOT NULL,
  ALTER COLUMN registration_date SET DEFAULT NULL;
