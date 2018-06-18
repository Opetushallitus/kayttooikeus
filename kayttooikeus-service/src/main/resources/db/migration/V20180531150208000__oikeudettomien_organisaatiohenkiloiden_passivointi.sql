update organisaatiohenkilo oh
  set passivoitu = true
  where passivoitu = false
    and not exists
      (select *
       from myonnetty_kayttooikeusryhma_tapahtuma mkt
       where mkt.organisaatiohenkilo_id = oh.id);


