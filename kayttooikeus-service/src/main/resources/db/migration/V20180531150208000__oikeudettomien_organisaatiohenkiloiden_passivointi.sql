update organisaatiohenkilo oh
  set oh.passivoitu = true
  where oh.passivoitu = false
    and not exists
      (select *
       from myonnetty_kayttooikeusryhma_tapahtuma mkt
       where mkt.organisaatiohenkilo_id = oh.id);


