INSERT INTO henkilo VALUES (
 nextval('public.hibernate_sequence'),
 '1.2.246.562.10.99999999999',
 'VIRKAILIJA',
 'Testi Käyttäjä',
 'Testikäyttäjäinen',
 false,
 false,
 true,
 '010101A999X',
 'Testi',
 now()
);

INSERT INTO kayttajatiedot SELECT
 nextval('hibernate_sequence'),
 1,
 null,
 null,
 id,
 now(),
 false,
 'testuser'
FROM henkilo WHERE oidhenkilo = '1.2.246.562.10.99999999999';

