--
-- KJHH-2251: Rename some VARDA related access rights
--

-- Step 1: Update access right role literals
UPDATE
    kayttooikeus ko
SET
    rooli = 'HENKILOSTO_VUOKRATTU_KATSELIJA'
FROM
    palvelu p
WHERE
        p.name = 'VARDA' AND
        ko.palvelu_id = p.id AND
        ko.rooli = 'HENKILOSTO_TILAPAISET_KATSELIJA';

UPDATE
    kayttooikeus ko
SET
    rooli = 'HENKILOSTO_VUOKRATTU_TALLENTAJA'
FROM
    palvelu p
WHERE
        p.name = 'VARDA' AND
        ko.palvelu_id = p.id AND
        ko.rooli = 'HENKILOSTO_TILAPAISET_TALLENTAJA';


-- Step 2: Update related localizations
UPDATE
    text t
SET
    text = 'Henkilöstö-vuokrattu-katselija'
FROM
    kayttooikeus ko,
    palvelu p
WHERE
    p.name = 'VARDA' AND
    ko.palvelu_id = p.id AND
    ko.textgroup_id = t.textgroup_id AND
    ko.rooli = 'HENKILOSTO_VUOKRATTU_KATSELIJA';

UPDATE
    text t
SET
    text = 'Henkilöstö-vuokrattu-tallentaja'
FROM
    kayttooikeus ko,
    palvelu p
WHERE
        p.name = 'VARDA' AND
        ko.palvelu_id = p.id AND
        ko.textgroup_id = t.textgroup_id AND
        ko.rooli = 'HENKILOSTO_VUOKRATTU_TALLENTAJA';
