insert into kayttooikeusryhma_kayttooikeus (kayttooikeusryhma_id, kayttooikeus_id)
select kor.id as kayttooikeusryhma_id, ko_uusi.id as kayttooikeus_id
from kayttooikeusryhma kor, kayttooikeusryhma_kayttooikeus kor_ko,
     kayttooikeus ko_vanha, palvelu p_vanha,
     kayttooikeus ko_uusi, palvelu p_uusi
where kor.id = kor_ko.kayttooikeusryhma_id
and ko_vanha.id = kor_ko.kayttooikeus_id and p_vanha.id = ko_vanha.palvelu_id
and p_uusi.id = ko_uusi.palvelu_id
and p_vanha.name = 'HENKILONHALLINTA' and ko_vanha.rooli = 'VASTUUKAYTTAJAT'
and p_uusi.name = 'KAYTTOOIKEUS' and ko_uusi.rooli = 'VASTUUKAYTTAJAT'
on conflict do nothing;
