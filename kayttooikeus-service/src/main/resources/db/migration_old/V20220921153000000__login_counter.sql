--
-- There is a need to know which user accounts have been in use in given
-- timeframe. This table is used for keeping track of amount of logins
-- and time of the last login.
--
CREATE TABLE public.login_counter (
    id bigint PRIMARY KEY,
    userid bigint UNIQUE,
    login_count bigint,
    last_login timestamp without time zone,
    FOREIGN KEY(userid) REFERENCES kayttajatiedot(id) ON DELETE CASCADE
);
