--
-- KJHH-1870: extend service users access rights to 2099
--
UPDATE
    myonnetty_kayttooikeusryhma_tapahtuma mkt
SET
    voimassaloppupvm = '2099-12-31'
FROM
    organisaatiohenkilo oh,
    kayttajatiedot k,
    henkilo h
WHERE
        mkt.organisaatiohenkilo_id = oh.id
  AND oh.henkilo_id = k.henkiloid
  AND k.henkiloid = h.id
  AND h.henkilotyyppi = 'PALVELU';
