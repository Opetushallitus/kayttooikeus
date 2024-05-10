UPDATE text
SET text = 'Organisaatioiden rekisteröityminen -palvelu'
FROM text_group
JOIN palvelu on text_group.id = palvelu.textgroup_id AND palvelu.name = 'ORGANISAATIOIDEN_REKISTEROITYMINEN'
WHERE text.textgroup_id = text_group.id;
