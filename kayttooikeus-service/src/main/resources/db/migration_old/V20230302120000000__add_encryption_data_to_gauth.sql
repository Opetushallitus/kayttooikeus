ALTER TABLE google_auth_token
  ADD COLUMN salt text NOT NULL,
  ADD COLUMN iv text NOT NULL;
