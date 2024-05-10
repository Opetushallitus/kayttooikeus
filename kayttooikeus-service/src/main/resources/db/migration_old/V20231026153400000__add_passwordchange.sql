ALTER TABLE kayttajatiedot ADD COLUMN passwordchange timestamp without time zone DEFAULT null;
ALTER TABLE kayttajatiedot ALTER COLUMN passwordchange SET DEFAULT current_timestamp;