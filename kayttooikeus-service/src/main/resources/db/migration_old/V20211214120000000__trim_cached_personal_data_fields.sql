--
-- Some fields from ONR are cached in access right service to be able to run
-- various searches. For some reason these cached fields may contain leading/trailing
-- whitespaces which break the search.
-- Fix data by trimming these particular fields.
--
UPDATE henkilo SET etunimet_cached = trim(etunimet_cached) WHERE etunimet_cached LIKE ' %' OR etunimet_cached LIKE '% ';
UPDATE henkilo SET sukunimi_cached = trim(sukunimi_cached) WHERE sukunimi_cached LIKE ' %' OR sukunimi_cached LIKE '% ';
UPDATE henkilo SET kutsumanimi_cached = trim(kutsumanimi_cached) WHERE kutsumanimi_cached LIKE ' %' OR kutsumanimi_cached LIKE '% ';
UPDATE henkilo SET hetu_cached = trim(hetu_cached) WHERE hetu_cached LIKE ' %' OR hetu_cached LIKE '% ';

