delete from kayttajatiedot where henkiloid in (select id from henkilo where passivoitu_cached = true);
