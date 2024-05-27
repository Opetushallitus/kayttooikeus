CREATE SEQUENCE hibernate_sequence;

CREATE TABLE henkilo
(
    id                            bigint       NOT NULL
        CONSTRAINT henkilo_pkey
            PRIMARY KEY,
    oidhenkilo                    varchar(255) NOT NULL
        CONSTRAINT henkilo_oidhenkilo_key
            UNIQUE,
    henkilotyyppi                 varchar(255),
    etunimet_cached               varchar(255),
    sukunimi_cached               varchar(255),
    passivoitu_cached             boolean,
    duplicate_cached              boolean,
    vahvasti_tunnistettu          boolean DEFAULT FALSE,
    hetu_cached                   varchar(255),
    kutsumanimi_cached            varchar(255),
    sahkopostivarmennus_aikaleima timestamp
)
;

CREATE TABLE anomus
(
    id                     bigint       NOT NULL
        CONSTRAINT anomus_pkey
            PRIMARY KEY,
    version                bigint       NOT NULL,
    anomuksentila          varchar(255),
    anomustilatapahtumapvm timestamp,
    anomustyyppi           varchar(255),
    anottupvm              timestamp,
    matkapuhelinnumero     varchar(255),
    organisaatiooid        varchar(255),
    perustelut             varchar(255),
    puhelinnumero          varchar(255),
    sahkopostiosoite       varchar(255) NOT NULL,
    tehtavanimike          varchar(255),
    henkilo_id             bigint
        CONSTRAINT fk752d36c9620670b2
            REFERENCES henkilo,
    kasittelija_henkilo_id bigint
        CONSTRAINT fk752d36c9e20ca904
            REFERENCES henkilo,
    hylkaamisperuste       varchar(255)
)
;

CREATE TABLE anomus_myonnettykayttooikeus
(
    kayttooikeus_id      bigint NOT NULL
        CONSTRAINT fk68b0d63f48dedded
            REFERENCES anomus,
    kayttooikeusryhma_id bigint NOT NULL,
    CONSTRAINT anomus_myonnettykayttooikeus_pkey
        PRIMARY KEY (kayttooikeus_id, kayttooikeusryhma_id)
);

CREATE TABLE anomus_myonnettykayttooikeusryhmas
(
    anomus_id                     bigint NOT NULL
        CONSTRAINT fk31a855dd562181e2
            REFERENCES anomus,
    myonnettykayttooikeusryhma_id bigint NOT NULL,
    CONSTRAINT anomus_myonnettykayttooikeusryhmas_pkey
        PRIMARY KEY (anomus_id, myonnettykayttooikeusryhma_id)
);

CREATE INDEX henkilo_etunimet_cached_idx
    ON henkilo (lower(etunimet_cached::text) text_pattern_ops);

CREATE INDEX henkilo_hetu_cached_idx
    ON henkilo (hetu_cached);

CREATE INDEX henkilo_kutsumanimi_cached_idx
    ON henkilo (lower(kutsumanimi_cached::text) text_pattern_ops);

CREATE INDEX henkilo_oid_idx
    ON henkilo (oidhenkilo);

CREATE INDEX henkilo_sukunimi_cached_idx
    ON henkilo (lower(sukunimi_cached::text) text_pattern_ops);

CREATE TABLE henkilo_bu
(
    id                 bigint,
    version            bigint,
    etunimet           varchar(255),
    hetu               varchar(255),
    kayttajatunnus     varchar(255),
    kotikunta          varchar(255),
    kutsumanimi        varchar(255),
    oidhenkilo         varchar(255),
    sukunimi           varchar(255),
    sukupuoli          varchar(255),
    turvakielto        boolean,
    henkilotyyppi      varchar(255),
    eisuomalaistahetua boolean,
    asiointikieli_id   bigint,
    passivoitu         boolean,
    yksiloity          boolean,
    oppijanumero       varchar(255)
);

CREATE TABLE henkilo_varmentaja_suhde
(
    id                       bigint  NOT NULL
        CONSTRAINT henkilo_varmentaja_suhde_pkey
            PRIMARY KEY,
    version                  bigint  NOT NULL,
    varmennettava_henkilo_id bigint  NOT NULL
        CONSTRAINT henkilo_varmentaja_suhde_varmennettava_henkilo_id_fkey
            REFERENCES henkilo,
    varmentava_henkilo_id    bigint  NOT NULL
        CONSTRAINT henkilo_varmentaja_suhde_varmentava_henkilo_id_fkey
            REFERENCES henkilo,
    tila                     boolean NOT NULL,
    aikaleima                timestamp
);

CREATE TABLE identification
(
    id                 bigint       NOT NULL
        CONSTRAINT identification_pkey
            PRIMARY KEY,
    version            bigint       NOT NULL,
    authtoken          varchar(255),
    email              varchar(255),
    identifier         varchar(255) NOT NULL,
    idpentityid        varchar(255) NOT NULL,
    henkilo_id         bigint       NOT NULL
        CONSTRAINT fk187d426e620670b2
            REFERENCES henkilo,
    expiration_date    timestamp,
    auth_token_created timestamp
)
;

CREATE INDEX identification_henkilo_id_idx
    ON identification (henkilo_id);

CREATE INDEX identifier_idx
    ON identification (identifier);

CREATE TABLE kayttajatiedot
(
    id             bigint                  NOT NULL
        CONSTRAINT password_pkey
            PRIMARY KEY,
    version        bigint                  NOT NULL,
    password       varchar(255),
    salt           varchar(255),
    henkiloid      bigint                  NOT NULL
        CONSTRAINT password_henkiloid_key
            UNIQUE
        CONSTRAINT fk4c641ebbdc0430b7
            REFERENCES henkilo,
    createdat      timestamp DEFAULT now() NOT NULL,
    invalidated    boolean   DEFAULT FALSE NOT NULL,
    username       varchar(255)
        CONSTRAINT username_unique
            UNIQUE,
    mfaprovider    text,
    passwordchange timestamp
)
;

CREATE INDEX password_idx
    ON kayttajatiedot (password);

CREATE INDEX password_salt_idx
    ON kayttajatiedot (salt);

CREATE INDEX username_idx
    ON kayttajatiedot (lower(username::text));

CREATE UNIQUE INDEX username_ci_unique
    ON kayttajatiedot (lower(username::text));

CREATE TABLE kutsu
(
    id                      bigint                                         NOT NULL
        CONSTRAINT kutsu_pkey
            PRIMARY KEY,
    version                 bigint      DEFAULT 0                          NOT NULL,
    aikaleima               timestamp   DEFAULT now()                      NOT NULL,
    kutsuja_oid             varchar(128)                                   NOT NULL,
    tila                    varchar(64) DEFAULT 'AVOIN'::character varying NOT NULL
        CONSTRAINT kutsu_tila
            CHECK ((tila)::text = ANY
                   (ARRAY [('AVOIN'::character varying)::text, ('KAYTETTY'::character varying)::text, ('POISTETTU'::character varying)::text])),
    sahkoposti              varchar(256)                                   NOT NULL,
    salaisuus               varchar(128)
        CONSTRAINT kutsu_salaisuus_key
            UNIQUE,
    kaytetty                timestamp,
    poistettu               timestamp,
    poistaja_oid            varchar(128),
    luotu_henkilo_oid       varchar(128)
        CONSTRAINT kutsu_luotu_henkilo_oid_fkey
            REFERENCES henkilo (oidhenkilo),
    kieli_koodi             varchar(2)                                     NOT NULL,
    etunimi                 text                                           NOT NULL,
    sukunimi                text                                           NOT NULL,
    temporary_token         varchar(128),
    hetu                    varchar(128),
    temporary_token_created timestamp,
    haka_identifier         varchar(255),
    saate                   text
);

CREATE TABLE kutsu_organisaatio
(
    id                 bigint           NOT NULL
        CONSTRAINT kutsu_organisaatio_pkey
            PRIMARY KEY,
    version            bigint DEFAULT 0 NOT NULL,
    kutsu              bigint           NOT NULL
        CONSTRAINT kutsu_organisaatio_kutsu_fkey
            REFERENCES kutsu,
    organisaatio_oid   varchar(128)     NOT NULL,
    voimassa_loppu_pvm date,
    CONSTRAINT kutsu_organisaatio_kutsu_organisaatio_oid_key
        UNIQUE (kutsu, organisaatio_oid)
);

CREATE TABLE organisaatiohenkilo
(
    id                 bigint                NOT NULL
        CONSTRAINT organisaatiohenkilo_pkey
            PRIMARY KEY,
    version            bigint                NOT NULL,
    matkapuhelinnumero varchar(255),
    organisaatio_oid   varchar(255)          NOT NULL,
    puhelinnumero      varchar(255),
    sahkopostiosoite   varchar(255),
    tehtavanimike      varchar(255),
    henkilo_id         bigint                NOT NULL
        CONSTRAINT fk7bdcd693620670b2
            REFERENCES henkilo,
    passivoitu         boolean DEFAULT FALSE NOT NULL,
    tyyppi             varchar(255),
    voimassa_alku_pvm  date,
    voimassa_loppu_pvm date,
    CONSTRAINT organisaatiohenkilo_organisaatio_oid_key
        UNIQUE (organisaatio_oid, henkilo_id)
)
;

CREATE INDEX organisaatio_henkilo_pass_idx
    ON organisaatiohenkilo (passivoitu);

CREATE INDEX organisaatiohenkilo_henkilo_id_idx
    ON organisaatiohenkilo (henkilo_id);

CREATE INDEX orghenkilo_oid_idx
    ON organisaatiohenkilo (organisaatio_oid);

CREATE TABLE schedule_timestamps
(
    id         bigint       NOT NULL
        CONSTRAINT schedule_timestamps_pkey
            PRIMARY KEY,
    version    bigint       NOT NULL,
    modified   timestamp    NOT NULL,
    identifier varchar(255) NOT NULL
);

CREATE TABLE scheduled_tasks
(
    task_name      text                     NOT NULL,
    task_instance  text                     NOT NULL,
    task_data      bytea,
    execution_time timestamp WITH TIME ZONE NOT NULL,
    picked         boolean                  NOT NULL,
    picked_by      text,
    last_success   timestamp WITH TIME ZONE,
    last_failure   timestamp WITH TIME ZONE,
    last_heartbeat timestamp WITH TIME ZONE,
    version        bigint                   NOT NULL,
    CONSTRAINT scheduled_tasks_pkey
        PRIMARY KEY (task_name, task_instance)
);

CREATE TABLE schema_table_ko
(
    installed_rank integer                 NOT NULL
        CONSTRAINT schema_table_ko_pk
            PRIMARY KEY,
    version        varchar(50),
    description    varchar(200)            NOT NULL,
    type           varchar(20)             NOT NULL,
    script         varchar(1000)           NOT NULL,
    checksum       integer,
    installed_by   varchar(100)            NOT NULL,
    installed_on   timestamp DEFAULT now() NOT NULL,
    execution_time integer                 NOT NULL,
    success        boolean                 NOT NULL
);

CREATE INDEX schema_table_ko_s_idx
    ON schema_table_ko (success);

CREATE TABLE spring_session
(
    primary_id            char(36) NOT NULL
        CONSTRAINT spring_session_pk
            PRIMARY KEY,
    session_id            char(36) NOT NULL,
    creation_time         bigint   NOT NULL,
    last_access_time      bigint   NOT NULL,
    max_inactive_interval integer  NOT NULL,
    expiry_time           bigint   NOT NULL,
    principal_name        varchar(100)
);

CREATE UNIQUE INDEX spring_session_ix1
    ON spring_session (session_id);

CREATE INDEX spring_session_ix2
    ON spring_session (expiry_time);

CREATE INDEX spring_session_ix3
    ON spring_session (principal_name);

CREATE TABLE spring_session_attributes
(
    session_primary_id char(36)     NOT NULL
        CONSTRAINT spring_session_attributes_fk
            REFERENCES spring_session
            ON DELETE CASCADE,
    attribute_name     varchar(200) NOT NULL,
    attribute_bytes    bytea        NOT NULL,
    CONSTRAINT spring_session_attributes_pk
        PRIMARY KEY (session_primary_id, attribute_name)
);

CREATE INDEX spring_session_attributes_ix1
    ON spring_session_attributes (session_primary_id);

CREATE TABLE text_group
(
    id      bigint NOT NULL
        CONSTRAINT text_group_pkey
            PRIMARY KEY,
    version bigint NOT NULL
);

CREATE TABLE kayttooikeusryhma
(
    id                bigint                NOT NULL
        CONSTRAINT kayttooikeusryhma_pkey
            PRIMARY KEY,
    version           bigint                NOT NULL,
    name              varchar(255)          NOT NULL
        CONSTRAINT kayttooikeusryhma_name_key
            UNIQUE,
    textgroup_id      bigint
        CONSTRAINT fk178e889798562292
            REFERENCES text_group,
    hidden            boolean DEFAULT FALSE NOT NULL,
    rooli_rajoite     varchar(255),
    kuvaus_id         bigint
        CONSTRAINT fk_kayttooikeusryhma_textgroup_kuvaus
            REFERENCES text_group,
    ryhma_restriction boolean DEFAULT FALSE NOT NULL,
    allowed_usertype  text,
    muokattu          timestamp,
    muokkaaja         varchar(255)
);

CREATE TABLE haettu_kayttooikeusryhma
(
    id                   bigint NOT NULL
        CONSTRAINT haettu_kayttooikeusryhma_pkey
            PRIMARY KEY,
    version              bigint NOT NULL,
    anomus_id            bigint
        CONSTRAINT fk43643dc1562181e2
            REFERENCES anomus,
    kayttooikeusryhma_id bigint
        CONSTRAINT fk43643dc16f540452
            REFERENCES kayttooikeusryhma,
    kasittelypvm         timestamp,
    tyyppi               varchar(255)
)
;

CREATE TABLE kayttooikeusryhma_myontoviite
(
    id                          bigint NOT NULL,
    version                     bigint NOT NULL,
    kayttooikeusryhma_master_id bigint NOT NULL
        CONSTRAINT fk4a1451e27a46214c
            REFERENCES kayttooikeusryhma,
    kayttooikeusryhma_slave_id  bigint NOT NULL
        CONSTRAINT fk32b4b1526d467183
            REFERENCES kayttooikeusryhma
);

CREATE TABLE kayttooikeusryhma_tapahtuma_historia
(
    id                     bigint       NOT NULL
        CONSTRAINT kayttooikeusryhma_tapahtuma_historia_pkey
            PRIMARY KEY,
    version                bigint       NOT NULL,
    aikaleima              timestamp    NOT NULL,
    tila                   varchar(255) NOT NULL,
    syy                    varchar(255),
    kayttooikeusryhma_id   bigint       NOT NULL
        CONSTRAINT fkd2a5a3123d4653a7
            REFERENCES kayttooikeusryhma,
    organisaatiohenkilo_id bigint       NOT NULL
        CONSTRAINT fkd2b4a1e23d4671c1
            REFERENCES organisaatiohenkilo,
    kasittelija_henkilo_id bigint       NOT NULL
        CONSTRAINT fkd29a21fd314634c3
            REFERENCES henkilo
)
;

CREATE TABLE kutsu_organisaatio_ryhma
(
    kutsu_organisaatio bigint NOT NULL
        CONSTRAINT kutsu_organisaatio_ryhma_kutsu_organisaatio_fkey
            REFERENCES kutsu_organisaatio,
    ryhma              bigint NOT NULL
        CONSTRAINT kutsu_organisaatio_ryhma_ryhma_fkey
            REFERENCES kayttooikeusryhma,
    CONSTRAINT kutsu_organisaatio_ryhma_pkey
        PRIMARY KEY (kutsu_organisaatio, ryhma)
);

CREATE TABLE myonnetty_kayttooikeusryhma_tapahtuma
(
    id                     bigint       NOT NULL
        CONSTRAINT myonnetty_kayttooikeusryhma_tapahtuma_pkey
            PRIMARY KEY,
    version                bigint       NOT NULL,
    aikaleima              timestamp    NOT NULL,
    syy                    varchar(255),
    tila                   varchar(255) NOT NULL,
    kasittelija_henkilo_id bigint
        CONSTRAINT fk7870a32fe20ca904
            REFERENCES henkilo,
    kayttooikeusryhma_id   bigint       NOT NULL
        CONSTRAINT fkce0453a546b127a
            REFERENCES kayttooikeusryhma,
    organisaatiohenkilo_id bigint       NOT NULL
        CONSTRAINT fkce0623ed4db1575
            REFERENCES organisaatiohenkilo,
    voimassaalkupvm        date         NOT NULL,
    voimassaloppupvm       date,
    CONSTRAINT kayttooikeus_organisaatio_unique
        UNIQUE (kayttooikeusryhma_id, organisaatiohenkilo_id)
)
;

CREATE INDEX myonnetty_alkupvm_idx
    ON myonnetty_kayttooikeusryhma_tapahtuma (voimassaalkupvm);

CREATE INDEX myonnetty_kayttooikeusryhma_tapahtuma_kayttooikeusryhma_id_idx
    ON myonnetty_kayttooikeusryhma_tapahtuma (kayttooikeusryhma_id);

CREATE INDEX myonnetty_kayttooikeusryhma_tapahtuma_organisaatiohenkilo_id_id
    ON myonnetty_kayttooikeusryhma_tapahtuma (organisaatiohenkilo_id);

CREATE INDEX myonnetty_loppupvm_idx
    ON myonnetty_kayttooikeusryhma_tapahtuma (voimassaloppupvm);

CREATE TABLE organisaatioviite
(
    id                   bigint NOT NULL
        CONSTRAINT organisaatioviite_pkey
            PRIMARY KEY,
    version              bigint NOT NULL,
    kayttooikeusryhma_id bigint NOT NULL
        CONSTRAINT fkd2c4a4cf690670e9
            REFERENCES kayttooikeusryhma,
    organisaatio_tyyppi  varchar(255)
);

CREATE TABLE palvelu
(
    id            bigint       NOT NULL
        CONSTRAINT palvelu_pkey
            PRIMARY KEY,
    version       bigint       NOT NULL,
    name          varchar(255) NOT NULL
        CONSTRAINT palvelu_name_key
            UNIQUE,
    palvelutyyppi varchar(255),
    textgroup_id  bigint
        CONSTRAINT fkd069179398562292
            REFERENCES text_group,
    kokoelma_id   bigint
        CONSTRAINT fkd069179395232062
            REFERENCES palvelu
);

CREATE TABLE kayttooikeus
(
    id           bigint NOT NULL
        CONSTRAINT kayttooikeus_pkey
            PRIMARY KEY,
    version      bigint NOT NULL,
    palvelu_id   bigint NOT NULL
        CONSTRAINT fkce0627ec2d90112
            REFERENCES palvelu,
    rooli        varchar(255),
    textgroup_id bigint
);

CREATE TABLE haettu_kayttooikeus
(
    id              bigint NOT NULL
        CONSTRAINT haettu_kayttooikeus_pkey
            PRIMARY KEY,
    version         bigint NOT NULL,
    anomus_id       bigint
        CONSTRAINT fk92e49b14562181e2
            REFERENCES anomus,
    kayttooikeus_id bigint
        CONSTRAINT fk92e49b146cad2be2
            REFERENCES kayttooikeus
);

CREATE TABLE kayttooikeusryhma_kayttooikeus
(
    kayttooikeusryhma_id bigint NOT NULL
        CONSTRAINT fkbba031266f540452
            REFERENCES kayttooikeusryhma,
    kayttooikeus_id      bigint NOT NULL
        CONSTRAINT fkbba031266cad2be2
            REFERENCES kayttooikeus,
    CONSTRAINT kayttooikeusryhma_kayttooikeus_pkey
        PRIMARY KEY (kayttooikeusryhma_id, kayttooikeus_id)
);

CREATE INDEX kayttooikeusryhma_kayttooikeus_kayttooikeusryhma_id_idx
    ON kayttooikeusryhma_kayttooikeus (kayttooikeusryhma_id);

CREATE TABLE text
(
    id           bigint NOT NULL
        CONSTRAINT text_pkey
            PRIMARY KEY,
    version      bigint NOT NULL,
    lang         varchar(255),
    text         text,
    textgroup_id bigint
        CONSTRAINT fk36452d98562292
            REFERENCES text_group
);

CREATE TABLE tunnistus_token
(
    id               bigint NOT NULL,
    version          bigint NOT NULL,
    login_token      varchar(255),
    henkilo_id       bigint NOT NULL,
    aikaleima        timestamp,
    kaytetty         timestamp,
    hetu             varchar(255),
    salasanan_vaihto boolean
);

CREATE TABLE cas_client_session
(
    mapping_id text NOT NULL
        CONSTRAINT cas_client_session_pkey
            PRIMARY KEY,
    session_id text NOT NULL
);

CREATE TABLE lakkautettu_organisaatio
(
    oid varchar(255) NOT NULL
        CONSTRAINT lakkautettu_organisaatio_pkey
            PRIMARY KEY
);

CREATE TABLE henkilo_anomusilmoitus_kayttooikeusryhma
(
    henkilo_id           bigint NOT NULL
        CONSTRAINT henkilo_anomusilmoitus_kayttooikeusryhma_henkilo_id_fkey
            REFERENCES henkilo,
    kayttooikeusryhma_id bigint NOT NULL
        CONSTRAINT henkilo_anomusilmoitus_kayttooikeusry_kayttooikeusryhma_id_fkey
            REFERENCES kayttooikeusryhma
);

CREATE TABLE login_counter
(
    id          bigint NOT NULL
        CONSTRAINT login_counter_pkey
            PRIMARY KEY,
    userid      bigint
        CONSTRAINT login_counter_userid_key
            UNIQUE
        CONSTRAINT login_counter_userid_fkey
            REFERENCES kayttajatiedot
            ON DELETE CASCADE,
    login_count bigint,
    last_login  timestamp
);

CREATE TABLE google_auth_token
(
    id                serial
        CONSTRAINT google_auth_token_pkey
            PRIMARY KEY,
    henkilo_id        bigint NOT NULL
        CONSTRAINT google_auth_token_henkilo_id_fkey
            REFERENCES henkilo,
    registration_date timestamp,
    secret_key        text   NOT NULL,
    salt              text   NOT NULL,
    iv                text   NOT NULL
);

CREATE FUNCTION arkistoi_arvosana_deltat(amount integer) RETURNS integer
    LANGUAGE plpgsql
AS
$$
DECLARE
    _resource_id varchar(200);
    _inserted    bigint;
    _count       int := 0;
    delta        record;
BEGIN
    FOR delta IN
        SELECT resource_id, inserted
        FROM arvosana
        EXCEPT
        SELECT resource_id, inserted
        FROM v_arvosana
        LIMIT amount
        LOOP
            INSERT INTO a_arvosana
            SELECT *
            FROM arvosana
            WHERE resource_id = delta.resource_id AND inserted = delta.inserted;
            DELETE FROM arvosana WHERE resource_id = delta.resource_id AND inserted = delta.inserted;
            _count := _count + 1;
            RAISE NOTICE '%: archived delta: %, %', _count, delta.resource_id, delta.inserted;
        END LOOP;

    RETURN _count;
END;
$$;

CREATE FUNCTION deletekayttooikeus(character varying, character varying) RETURNS integer
    LANGUAGE plpgsql
AS
$$
DECLARE
    palvelu_name ALIAS FOR $1;
    kayttooikeus_rooli ALIAS FOR $2;
    _kayttooikeus_id bigint;
    _textgroup_id    bigint;

BEGIN

    SELECT k.id
    INTO _kayttooikeus_id
    FROM kayttooikeus k
             INNER JOIN palvelu p ON p.id = k.palvelu_id
    WHERE k.rooli = kayttooikeus_rooli
        AND p.name = palvelu_name;
    IF found THEN
        SELECT textgroup_id INTO STRICT _textgroup_id FROM kayttooikeus WHERE id = _kayttooikeus_id;
        DELETE FROM kayttooikeusryhma_kayttooikeus WHERE kayttooikeus_id = _kayttooikeus_id;
        DELETE FROM kayttooikeus WHERE id = _kayttooikeus_id;
        DELETE FROM text WHERE textgroup_id = _textgroup_id;
        DELETE FROM text_group WHERE id = _textgroup_id;
    END IF;

    RETURN 1;

END;

$$;

CREATE FUNCTION insertkayttooikeus(character varying, character varying, character varying) RETURNS integer
    LANGUAGE plpgsql
AS
$$
DECLARE
    palvelu_name ALIAS FOR $1;
    kayttooikeus_rooli ALIAS FOR $2;
    kayttooikeus_text_fi ALIAS FOR $3;
    _kayttooikeus_exists bigint;

BEGIN

    SELECT count(*)
    INTO _kayttooikeus_exists
    FROM kayttooikeus k
             INNER JOIN palvelu p ON p.id = k.palvelu_id
    WHERE k.rooli = kayttooikeus_rooli
        AND p.name = palvelu_name;

    IF _kayttooikeus_exists = 0 THEN
        INSERT INTO text_group (id, version) VALUES (nextval('public.hibernate_sequence'), 1);
        INSERT INTO text (id, version, lang, text, textgroup_id)
        VALUES (nextval('public.hibernate_sequence'), 1, 'FI', kayttooikeus_text_fi, (SELECT max(id) FROM text_group));
        INSERT INTO text (id, version, lang, text, textgroup_id)
        VALUES (nextval('public.hibernate_sequence'), 1, 'SV', kayttooikeus_text_fi, (SELECT max(id) FROM text_group));
        INSERT INTO text (id, version, lang, text, textgroup_id)
        VALUES (nextval('public.hibernate_sequence'), 1, 'EN', kayttooikeus_text_fi, (SELECT max(id) FROM text_group));
        INSERT INTO kayttooikeus (id, version, palvelu_id, rooli, textgroup_id)
        VALUES (nextval('public.hibernate_sequence'), 1, (SELECT id FROM palvelu WHERE name = palvelu_name), kayttooikeus_rooli, (SELECT max(id) FROM text_group));
    END IF;

    RETURN 1;

END;

$$;

CREATE FUNCTION insertpalvelu(character varying, character varying) RETURNS integer
    LANGUAGE plpgsql
AS
$$
DECLARE
    role_name ALIAS FOR $1;
    role_text_fi ALIAS FOR $2;
    _role_exists bigint;

BEGIN

    SELECT count(*) INTO _role_exists FROM palvelu WHERE name = role_name;

    IF _role_exists = 0 THEN
        INSERT INTO text_group (id, version) VALUES (nextval('public.hibernate_sequence'), 1);
        INSERT INTO text (id, version, lang, text, textgroup_id)
        VALUES (nextval('public.hibernate_sequence'), 1, 'FI', role_text_fi, (SELECT max(id) FROM text_group));
        INSERT INTO text (id, version, lang, text, textgroup_id)
        VALUES (nextval('public.hibernate_sequence'), 1, 'SV', role_text_fi, (SELECT max(id) FROM text_group));
        INSERT INTO text (id, version, lang, text, textgroup_id)
        VALUES (nextval('public.hibernate_sequence'), 1, 'EN', role_text_fi, (SELECT max(id) FROM text_group));
        INSERT INTO palvelu (id, version, name, palvelutyyppi, textgroup_id)
        VALUES (nextval('public.hibernate_sequence'), 1, role_name, 'YKSITTAINEN', (SELECT max(id) FROM text_group));
    END IF;

    RETURN 1;

END;

$$;


SELECT insertpalvelu('AIPAL', 'Aipal');
SELECT insertpalvelu('AITU', 'Aitu');
SELECT insertpalvelu('AKT', 'AKT');
SELECT insertpalvelu('AMKPAL', 'AMKPAL');
SELECT insertpalvelu('ANOMUSTENHALLINTA', 'Anomustenhallintapalvelu');
SELECT insertpalvelu('ASIAKIRJAPALVELU', 'Asiakirjapalvelu');
SELECT insertpalvelu('ATARU_EDITORI', 'Ataru editori');
SELECT insertpalvelu('ATARU_HAKEMUS', 'Ataru hakemus');
SELECT insertpalvelu('EHOKS', 'EHOKS-palvelu');
SELECT insertpalvelu('EPERUSTEET', 'E-perusteet');
SELECT insertpalvelu('EPERUSTEET_AMOSAA', 'E-perusteet amosaa');
SELECT insertpalvelu('EPERUSTEET_AMOSAA_NEW', 'ePerusteet Amosaa Uusi Käyttöliittymä');
SELECT insertpalvelu('EPERUSTEET_KOTO', 'ePerusteet KOTO');
SELECT insertpalvelu('EPERUSTEET_MAARAYS', 'ePerusteet määräyskokoelma');
SELECT insertpalvelu('EPERUSTEET_TUVA', 'ePerusteet TUVA');
SELECT insertpalvelu('EPERUSTEET_VST', 'ePerusteet VST');
SELECT insertpalvelu('EPERUSTEET_YLOPS', 'E-perusteet ylops');
SELECT insertpalvelu('HAKEMUS', 'Hakemuspalvelu');
SELECT insertpalvelu('HAKUJENHALLINTA', 'Hakujen hallinta');
SELECT insertpalvelu('HAKUKOHDERYHMAPALVELU', 'Hakukohderyhmäpalvelu');
SELECT insertpalvelu('HAKULOMAKKEENHALLINTA', 'Hakulomakkeen hallinta');
SELECT insertpalvelu('HAKUPERUSTEETADMIN', 'Hakuperusteet admin');
SELECT insertpalvelu('HENKILONHALLINTA', 'Henkilonhallintapalvelu');
SELECT insertpalvelu('HENKILOTIETOMUUTOS', 'Henkilötietomuutos');
SELECT insertpalvelu('IPOSTI', 'Iposti');
SELECT insertpalvelu('KAYTTOOIKEUS', 'Käyttöoikeus');
SELECT insertpalvelu('KKHAKUVIRKAILIJA', 'Kk-haku virkailija');
SELECT insertpalvelu('KOODISTO', 'Koodistopalvelu');
SELECT insertpalvelu('KOOSTEROOLIENHALLINTA', 'Koosteroolien hallintapalvelu');
SELECT insertpalvelu('KOSKI', 'Koski');
SELECT insertpalvelu('KOUTA', 'KOUTA-palvelu');
SELECT insertpalvelu('KOUTE', 'Koute');
SELECT insertpalvelu('LIITERI', 'Liiteri-palvelu');
SELECT insertpalvelu('LOKALISOINTI', 'Käännösten hallinta');
SELECT insertpalvelu('LUDOS', 'LUDOS');
SELECT insertpalvelu('MAKSUT', 'Maksupalvelu');
SELECT insertpalvelu('MPASSID', 'MPASSid');
SELECT insertpalvelu('OHJAUSPARAMETRIT', 'Ohjausparametrit');
SELECT insertpalvelu('OID', 'OID-palvelu');
SELECT insertpalvelu('OIKEUSTULKKIREKISTERI', 'Oikeustulkkirekisteri');
SELECT insertpalvelu('OIVA_APP', 'Oiva-palvelu');
SELECT insertpalvelu('OMATTIEDOT', 'omattiedotpalvelu');
SELECT insertpalvelu('OPPIJANTUNNISTUS', 'Oppijan tunnistus');
SELECT insertpalvelu('OPPIJANUMEROREKISTERI', 'Oppijanumerorekisteri');
SELECT insertpalvelu('ORGANISAATIOHALLINTA', 'Organisaatioidenhallinta');
SELECT insertpalvelu('ORGANISAATIOIDEN_REKISTEROITYMINEN', 'Yksityisten rekisteröityminen -palvelu');
SELECT insertpalvelu('OSOITE', 'Osoitepalvelu');
SELECT insertpalvelu('OTI', 'OTI');
SELECT insertpalvelu('PALAUTE', 'PALAUTE-palvelu');
SELECT insertpalvelu('RAPORTOINTI', 'Raportointi');
SELECT insertpalvelu('RYHMASAHKOPOSTI', 'Ryhmäsähköposti');
SELECT insertpalvelu('SIJOITTELU', 'Sijoittelu');
SELECT insertpalvelu('SISALLONHALLINTA', 'Sisällönhallinta');
SELECT insertpalvelu('SUORITUSREKISTERI', 'Suoritusrekisteri');
SELECT insertpalvelu('TARJONTA', 'Tarjontatiedonhallinta');
SELECT insertpalvelu('TARJONTA_KK', 'Tarjonta kk');
SELECT insertpalvelu('TIEDONSIIRTO', 'Tiedonsiirto');
SELECT insertpalvelu('ULKOISETRAJAPINNAT', 'APIt Tilastokeskukselle, ODW:lle ym.');
SELECT insertpalvelu('VALINTAPERUSTEET', 'Valintaperusteet');
SELECT insertpalvelu('VALINTAPERUSTEETKK', 'Valintaperusteet kk');
SELECT insertpalvelu('VALINTAPERUSTEKUVAUSTENHALLINTA', 'Valintaperustekuvausten hallinta');
SELECT insertpalvelu('VALINTAPERUSTEKUVAUSTENHALLINTA_KK', 'Valintaperustekuvausten hallinta kk');
SELECT insertpalvelu('VALINTATULOSSERVICE', 'Valintatulosservice');
SELECT insertpalvelu('VALINTOJENTOTEUTTAMINEN', 'Valintojen toteuttaminen');
SELECT insertpalvelu('VALINTOJENTOTEUTTAMINENKK', 'Valintalaskentojen toteuttaminen kk');
SELECT insertpalvelu('VALPAS', 'Valpas-palvelu');
SELECT insertpalvelu('VALSSI', 'Valssi-palvelu');
SELECT insertpalvelu('VALTIONAVUSTUS', 'Valtionavustus');
SELECT insertpalvelu('VARDA', 'Varda');
SELECT insertpalvelu('VIESTINVALITYS', 'Viestinvälityspalvelu');
SELECT insertpalvelu('VIRKAILIJANTYOPOYTA', 'Virkailijan työpöytä');
SELECT insertpalvelu('VKT', 'VKT');
SELECT insertpalvelu('VOS', 'Valtionosuudet');
SELECT insertpalvelu('YHTEYSTIETOTYYPPIENHALLINTA', 'Yhteystietotyyppien hallinta');
SELECT insertpalvelu('YKI', 'Yleisten kielitutkintojen rekisteri');
SELECT insertpalvelu('YKSITYISTEN_REKISTEROITYMINEN', 'Yksityisten rekisteröityminen -palvelu');
SELECT insertpalvelu('YTLMATERIAALITILAUS', 'YTL materiaalitilaus');
SELECT insertpalvelu('YTLTULOSLUETTELO', 'YTL tulosluettelo');
SELECT insertkayttooikeus('AIPAL', 'AITUREAD', 'Aitu lukuoikeus');
SELECT insertkayttooikeus('AIPAL', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('AIPAL', 'KTKATSELIJA', 'Koulutustoimijan katselija');
SELECT insertkayttooikeus('AIPAL', 'KTKAYTTAJA', 'Koulutustoimijan käyttäjä');
SELECT insertkayttooikeus('AIPAL', 'KTPAAKAYTTAJA', 'Koulutustoimijan pääkäyttäjä');
SELECT insertkayttooikeus('AIPAL', 'KTVASTUUKAYTTAJA', 'Koulutustoimijan vastuukäyttäjä');
SELECT insertkayttooikeus('AIPAL', 'NTMVASTUUKAYTTAJA', 'NTM-vastuukäyttäjä');
SELECT insertkayttooikeus('AIPAL', 'OPHKATSELIJA', 'OPH:n katselija');
SELECT insertkayttooikeus('AIPAL', 'OPHPAAKAYTTAJA', 'OPH:n pääkäyttäjä');
SELECT insertkayttooikeus('AIPAL', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('AIPAL', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('AIPAL', 'TKTKATSELIJA', 'Tutkintotoimikunnan katselija');
SELECT insertkayttooikeus('AIPAL', 'YLKATSELIJA', 'Yleinen katselija');
SELECT insertkayttooikeus('AITU', 'AIPALREAD', 'Aipal lukuoikeus');
SELECT insertkayttooikeus('AITU', 'AITU-OPH-KATSELIJA', 'AITU-OPH-katselija');
SELECT insertkayttooikeus('AITU', 'AITU-TMK-JASEN', 'AITU-Tmk-jäsen');
SELECT insertkayttooikeus('AITU', 'AITU-TMK-KATSELIJA', 'AITU-Tmk-katselija');
SELECT insertkayttooikeus('AITU', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('AITU', 'OSOITEPALVELU', 'Osoitepalvelu');
SELECT insertkayttooikeus('AITU', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('AITU', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('AITU', 'TYOELAMAJARJESTO', 'Työelämäjärjestön käyttäjä');
SELECT insertkayttooikeus('AKT', 'PAAKAYTTAJA', 'Pääkäyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'AMKKATSELIJA', 'Arvo-AMK-katselija');
SELECT insertkayttooikeus('AMKPAL', 'AMKKAYTTAJA', 'Arvo-AMK-käyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'AMKVASTUUKAYTTAJA', 'Arvo-AMK-vastuukäyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-KT-KATSELIJA', 'Arvo-koulutustoimijan katselija');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-KT-KAYTTAJA', 'Arvo-koulutustoimijan käyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-KT-KYSELYKERTAKAYTTAJA', 'ARVO-koulutustoimijan kyselykertakäyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-KT-VASTUUKAYTTAJA', 'Arvo-koulutustoimijan vastuukäyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-RAJAPINTA', 'Arvo-Rajapintakäyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-YO-KATSELIJA', 'Arvo-YO-katselija');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-YO-KAYTTAJA', 'Arvo-YO-käyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-YO-PAAKAYTTAJA', 'Arvo-YO-pääkäyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-YO-VASTUUKAYTTAJA', 'Arvo-YO-vastuukäyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('AMKPAL', 'KATSELIJA', 'Arvo-katselija');
SELECT insertkayttooikeus('AMKPAL', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('AMKPAL', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('AMKPAL', 'YLLAPITAJA', 'Arvo-ylläpitäjä');
SELECT insertkayttooikeus('ANOMUSTENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('ANOMUSTENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('ANOMUSTENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'ASIOINTITILICRUD', 'Asiointitilipalvelun luku- ja lähetysoikeudet');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'CREATE_LETTER', 'Kirjeen luonti');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'CREATE_TEMPLATE', 'Kirjepohjan luonti');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'READ', 'Kirjeiden luku');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'SEND_LETTER_EMAIL', 'Sähköpostin lähetys kirjeestä');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'SYSTEM_ATTACHMENT_DOWNLOAD', 'Kirjelähetyksen sähköpostiliitteiden lataus (järjestelmien välinen)');
SELECT insertkayttooikeus('ATARU_EDITORI', 'CRUD', 'Luku-, muokkaus-, ja poisto-oikeus');
SELECT insertkayttooikeus('ATARU_HAKEMUS', 'ARKALUONTOINEN_READ', 'Hakemuksen arkaluontoisten tietojen lukuoikeus');
SELECT insertkayttooikeus('ATARU_HAKEMUS', 'ARKALUONTOINEN_UPDATE', 'Hakemuksen arkaluontoisten tietojen muokkausoikeus');
SELECT insertkayttooikeus('ATARU_HAKEMUS', 'CRUD', 'Luku-, muokkaus-, ja poisto-oikeus');
SELECT insertkayttooikeus('ATARU_HAKEMUS', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('ATARU_HAKEMUS', 'VALINTA_CRUD', 'Oikeus valinnan tuloksen luontiin, tarkasteluun, päivittämiseen ja poistamiseen Hakemuspalvelussa');
SELECT insertkayttooikeus('ATARU_HAKEMUS', 'VALINTA_READ', 'Oikeus valinnan tuloksen tarkasteluun Hakemuspalvelussa');
SELECT insertkayttooikeus('ATARU_HAKEMUS', 'opinto-ohjaaja', 'Opinto-ohjaajan oikeudet hakemusten käsittelyyn');
SELECT insertkayttooikeus('ATARU_HAKEMUS', 'valinnat-valilehti', 'Hakemuksen Valinnat-välilehden tietojen katseluoikeus');
SELECT insertkayttooikeus('EHOKS', 'CRUD', 'Luku-, luonti- ja päivitysoikeudet (omat organisaatiot)');
SELECT insertkayttooikeus('EHOKS', 'HOKS_DELETE', 'Hoksien poisto-oikeus');
SELECT insertkayttooikeus('EHOKS', 'HYODYNTAMIS_JA_JAKO', 'Ehoks hyödyntämis- ja jako-oikeus');
SELECT insertkayttooikeus('EHOKS', 'OPHPAAKAYTTAJA', 'Ehoks pääkäyttäjä');
SELECT insertkayttooikeus('EHOKS', 'READ', 'Lukuoikeus (omat organisaatiot)');
SELECT insertkayttooikeus('EHOKS', 'TIEDONSIIRTO', 'Ehoks tiedonsiirto omaan organisaatioon');
SELECT insertkayttooikeus('EPERUSTEET', 'ADMIN', 'Pääkäyttäjä');
SELECT insertkayttooikeus('EPERUSTEET', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('EPERUSTEET', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('EPERUSTEET', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('EPERUSTEET_AMOSAA', 'ADMIN', 'Pääkäyttäjä');
SELECT insertkayttooikeus('EPERUSTEET_AMOSAA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('EPERUSTEET_AMOSAA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('EPERUSTEET_AMOSAA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('EPERUSTEET_AMOSAA_NEW', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('EPERUSTEET_KOTO', 'ADMIN', 'Pääkäyttäjä');
SELECT insertkayttooikeus('EPERUSTEET_KOTO', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('EPERUSTEET_KOTO', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('EPERUSTEET_MAARAYS', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('EPERUSTEET_MAARAYS', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('EPERUSTEET_TUVA', 'ADMIN', 'Pääkäyttäjä');
SELECT insertkayttooikeus('EPERUSTEET_TUVA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('EPERUSTEET_TUVA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('EPERUSTEET_VST', 'ADMIN', 'Pääkäyttäjä');
SELECT insertkayttooikeus('EPERUSTEET_VST', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('EPERUSTEET_VST', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('EPERUSTEET_YLOPS', 'ADMIN', 'Pääkäyttäjä');
SELECT insertkayttooikeus('EPERUSTEET_YLOPS', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('EPERUSTEET_YLOPS', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('EPERUSTEET_YLOPS', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('HAKEMUS', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('HAKEMUS', 'HETUTTOMIENKASITTELY', 'Hetuttomien käsittely');
SELECT insertkayttooikeus('HAKEMUS', 'LISATIETOCRUD', 'Hakemuksen lisätiedot ylläpitäjä');
SELECT insertkayttooikeus('HAKEMUS', 'LISATIETOREAD', 'Hakemuksen lisätiedot lukuoikeus');
SELECT insertkayttooikeus('HAKEMUS', 'LISATIETORU', 'Hakemuksen lisätiedot vastuukäyttäjä');
SELECT insertkayttooikeus('HAKEMUS', 'OPO', 'Hakemuspalvelun opinto-ohjaajat');
SELECT insertkayttooikeus('HAKEMUS', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('HAKEMUS', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('HAKUJENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('HAKUJENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('HAKUJENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('HAKUKOHDERYHMAPALVELU', 'CRUD', 'Hakukohderyhmäpalvelun luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('HAKULOMAKKEENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('HAKULOMAKKEENHALLINTA', 'LOMAKEPOHJANVAIHTO', 'Lomakepohjan vaihto');
SELECT insertkayttooikeus('HAKULOMAKKEENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('HAKULOMAKKEENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('HAKUPERUSTEETADMIN', 'CRUD', 'Luku-, muokkaus-, ja poisto-oikeus');
SELECT insertkayttooikeus('HAKUPERUSTEETADMIN', 'REKISTERINPITAJA', 'Rekisterinpitäjä');
SELECT insertkayttooikeus('HENKILONHALLINTA', '2ASTEENVASTUU', '2. asteen vastuukäyttäjä');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'HENKILOVIITE_READ', 'Henkilöviitetiedon lukuoikeus');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'KKVASTUU', 'Korkeakoulun vastuukäyttäjä');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'MUUTOSTIETOPALVELU', 'Muutostietopalvelu');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'OPHREKISTERI', 'OPH rekisterinpitäjä');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'OPOTVIRKAILIJAT', 'Opinto-ohjaajat ja -virkailijat');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'VASTUUKAYTTAJAT', 'Vastuukäyttäjät');
SELECT insertkayttooikeus('HENKILOTIETOMUUTOS', 'PALVELUKAYTTAJA', 'Palvelukäyttäjä');
SELECT insertkayttooikeus('HENKILOTIETOMUUTOS', 'REKISTERINPITAJA', 'Rekisterinpitäjä');
SELECT insertkayttooikeus('IPOSTI', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('IPOSTI', 'SEND', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'ACCESS_RIGHTS_REPORT', 'Käyttöoikeusraportti');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'KAYTTOOIKEUSRYHMIEN_LUKU', 'Käyttöoikeusryhmien luku');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'KUTSU_CRUD', 'Virkailijan kutsuminen');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'PALVELUKAYTTAJA_CRUD', 'Palvelukäyttäjän muokkausoikeus');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'REKISTERINPITAJA', 'Rekisterinpitäjä');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'SCHEDULE', 'Ajastusoikeus');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'VASTUUKAYTTAJAT', 'Vastuukäyttäjät');
SELECT insertkayttooikeus('KKHAKUVIRKAILIJA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KKHAKUVIRKAILIJA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('KKHAKUVIRKAILIJA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOODISTO', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOODISTO', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('KOODISTO', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOOSTEROOLIENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOOSTEROOLIENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('KOOSTEROOLIENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOSKI', 'AIKUISTENPERUSOPETUS', 'Saa nähdä aikuisten perusopetuksen opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'AMMATILLINENKOULUTUS', 'Saa nähdä ammatillisia opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'DIATUTKINTO', 'Saa nähdä DIA-tutkinnon opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'ESIOPETUS', 'Saa nähdä esiopetuksen opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'GLOBAALI_LUKU_KORKEAKOULU', 'Lukuoikeus (kaikki organisaatiot, korkeakoulutus)');
SELECT insertkayttooikeus('KOSKI', 'GLOBAALI_LUKU_MUU_KUIN_SAANNELTY', 'Lukuoikeus (kaikki organisaatiot, muu kuin säännelty koulutus)');
SELECT insertkayttooikeus('KOSKI', 'GLOBAALI_LUKU_PERUSOPETUS', 'Lukuoikeus (kaikki organisaatiot, perusopetus)');
SELECT insertkayttooikeus('KOSKI', 'GLOBAALI_LUKU_TAITEENPERUSOPETUS', 'Lukuoikeus (kaikki organisaatiot, taiteen perusopetus)');
SELECT insertkayttooikeus('KOSKI', 'GLOBAALI_LUKU_TOINEN_ASTE', 'Lukuoikeus (kaikki organisaatiot, toisen asteen koulutus)');
SELECT insertkayttooikeus('KOSKI', 'HSL', 'Koski-tietojen käsittelyoikeudet HSL:lle');
SELECT insertkayttooikeus('KOSKI', 'IBTUTKINTO', 'Saa nähdä IB-tutkinnon opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'INTERNATIONALSCHOOL', 'Saa nähdä International school opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'KAIKKI_OPISKELUOIKEUS_TYYPIT', 'Saa nähdä kaiken tyyppiset opiskeluoikeudet');
SELECT insertkayttooikeus('KOSKI', 'KORKEAKOULUTUS', 'Saa nähdä korkeakoulujen opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'LUKIOKOULUTUS', 'Saa nähdä lukiokoulutuksen opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'LUKU_ESIOPETUS', 'Rajaa organisaation Koski-lukuoikeus vain esiopetukseen');
SELECT insertkayttooikeus('KOSKI', 'LUOTTAMUKSELLINEN', 'Arkaluontoisen datan lukuoikeus');
SELECT insertkayttooikeus('KOSKI', 'LUOTTAMUKSELLINEN_KELA_LAAJA', 'Kelan laajat arkaluontoisten Koski-tietojen katseluoikeudet');
SELECT insertkayttooikeus('KOSKI', 'LUOTTAMUKSELLINEN_KELA_SUPPEA', 'Kelan suppeat arkaluontoisten Koski-tietojen katseluoikeudet');
SELECT insertkayttooikeus('KOSKI', 'LUVA', 'Saa nähdä lukioon valmistavan koulutuksen opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'MIGRI', 'Koski-tietojen käsittelyoikeudet Migrille');
SELECT insertkayttooikeus('KOSKI', 'MIGRI_RAJAPINTA_LUKUOIKEUS', 'Migrin Koski-tietojen lukuoikeudet');
SELECT insertkayttooikeus('KOSKI', 'OPHKATSELIJA', 'OPH:n katselija');
SELECT insertkayttooikeus('KOSKI', 'OPHPAAKAYTTAJA', 'OPH:n pääkäyttäjä');
SELECT insertkayttooikeus('KOSKI', 'OPPIVELVOLLISUUSTIETO_RAJAPINTA', 'Saa kutsua Kosken rajapintaa, josta haetaan oppivelvollisuuden ja koulutuksen maksuttomuuden voimassaoloajat');
SELECT insertkayttooikeus('KOSKI', 'PERUSOPETUKSEENVALMISTAVAOPETUS', 'Saa nähdä perusopetukseen valmistavan opetuksen opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'PERUSOPETUKSENLISAOPETUS', 'Saa nähdä perusopetuksen lisäopetuksen opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'PERUSOPETUS', 'Saa nähdä nuorten perusopetuksen opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'READ', 'Lukuoikeus (omat organisaatiot)');
SELECT insertkayttooikeus('KOSKI', 'READ_UPDATE', 'Luku- ja muokkausoikeus (omat organisaatiot)');
SELECT insertkayttooikeus('KOSKI', 'READ_UPDATE_ESIOPETUS', 'Voi katsella ja tallentaa vain esiopetuksen opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'SUOMIFI', 'Koski-tietojen käsittelyoikeudet Suomi.fi:lle');
SELECT insertkayttooikeus('KOSKI', 'TAITEENPERUSOPETUS_HANKINTAKOULUTUS', 'Saa nähdä ja muokata taiteen perusopetuksen itse tai hankintakoulutuksena järjestettyjä opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'TIEDONSIIRRON_MITATOINTI', 'Tiedonsiirron mitätöinti');
SELECT insertkayttooikeus('KOSKI', 'TIEDONSIIRTO', 'Tiedonsiirto');
SELECT insertkayttooikeus('KOSKI', 'TIEDONSIIRTO_LUOVUTUSPALVELU', 'Koski tiedonsiirto luovutuspalvelu');
SELECT insertkayttooikeus('KOSKI', 'TILASTOKESKUS', 'Tilastokeskuksen luovutuspalveluhakuoikeus');
SELECT insertkayttooikeus('KOSKI', 'VALVIRA', 'Valvira-rajapinnan käyttöoikeus');
SELECT insertkayttooikeus('KOSKI', 'VAPAANSIVISTYSTYONKOULUTUS', 'Saa nähdä vapaan sivistystyön opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'YLIOPPILASTUTKINTO', 'Saa nähdä ylioppilastutkinnon opiskeluoikeuksia');
SELECT insertkayttooikeus('KOSKI', 'YLLAPITAJA', 'Oiva-ylläpitäjä');
SELECT insertkayttooikeus('KOSKI', 'YTL', 'Oikeus käyttää KOSKI-palvelun YTL-APIa');
SELECT insertkayttooikeus('KOUTA', 'HAKUKOHDE_CRUD', 'Hakukohteen luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTA', 'HAKUKOHDE_READ', 'Hakukohteen lukuoikeus');
SELECT insertkayttooikeus('KOUTA', 'HAKUKOHDE_READ_UPDATE', 'Hakukohteen luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTA', 'HAKU_CRUD', 'Haun luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTA', 'HAKU_READ', 'Haun lukuoikeus');
SELECT insertkayttooikeus('KOUTA', 'HAKU_READ_UPDATE', 'Haun luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTA', 'INDEKSOINTI', 'Indeksoinnin oikeudet');
SELECT insertkayttooikeus('KOUTA', 'KOULUTUS_CRUD', 'Koulutuksen luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTA', 'KOULUTUS_READ', 'Koulutuksen lukuoikeus');
SELECT insertkayttooikeus('KOUTA', 'KOULUTUS_READ_UPDATE', 'Koulutuksen luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTA', 'OPHPAAKAYTTAJA', 'OPH:n pääkäyttäjäoikeudet');
SELECT insertkayttooikeus('KOUTA', 'OPPILAITOS_CRUD', 'Oppilaitoksen ja oppilaitoksen osien luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTA', 'OPPILAITOS_READ', 'Oppilaitoksen ja oppilaitoksen osien lukuoikeus');
SELECT insertkayttooikeus('KOUTA', 'OPPILAITOS_READ_UPDATE', 'Oppilaitoksen ja oppilaitoksen osien luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTA', 'TOTEUTUS_CRUD', 'Toteutuksen luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTA', 'TOTEUTUS_READ', 'Toteutuksen lukuoikeus');
SELECT insertkayttooikeus('KOUTA', 'TOTEUTUS_READ_UPDATE', 'Toteutuksen luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTA', 'VALINTAPERUSTE_CRUD', 'Valintaperustekuvauksen luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTA', 'VALINTAPERUSTE_READ', 'Valintaperustekuvauksen lukuoikeus');
SELECT insertkayttooikeus('KOUTA', 'VALINTAPERUSTE_READ_UPDATE', 'Valintaperustekuvauksen luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTE', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTE', 'ESITTELIJA', 'Oiva-esittelijä');
SELECT insertkayttooikeus('KOUTE', 'KATSELIJA', 'Oiva-katselija');
SELECT insertkayttooikeus('KOUTE', 'KAYTTAJA', 'Oiva-käyttäjä');
SELECT insertkayttooikeus('KOUTE', 'NIMENKIRJOITTAJA', 'Oiva-nimenkirjoittaja');
SELECT insertkayttooikeus('KOUTE', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('KOUTE', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTE', 'YLLAPITAJA', 'Oiva-ylläpitäjä');
SELECT insertkayttooikeus('LIITERI', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('LOKALISOINTI', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('LOKALISOINTI', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('LOKALISOINTI', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('LUDOS', 'LUKU', 'Lukuoikeus');
SELECT insertkayttooikeus('LUDOS', 'LUKU_MUOKKAUS', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('LUDOS', 'LUKU_MUOKKAUS_POISTO', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('MAKSUT', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('MPASSID', 'KATSELIJA', 'Katselija');
SELECT insertkayttooikeus('MPASSID', 'PALVELU_TALLENTAJA', 'Palvelun tallentaja');
SELECT insertkayttooikeus('MPASSID', 'TALLENTAJA', 'Tallentaja');
SELECT insertkayttooikeus('OHJAUSPARAMETRIT', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('OID', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('OID', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('OID', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('OIKEUSTULKKIREKISTERI', 'OIKEUSTULKKI_CRUD', 'Oikeustulkkirekisterin ylläpito');
SELECT insertkayttooikeus('OIVA_APP', 'ADMIN', 'Oiva-admin');
SELECT insertkayttooikeus('OIVA_APP', 'ESITTELIJA', 'Oiva-esittelijä');
SELECT insertkayttooikeus('OIVA_APP', 'KATSELIJA', 'Oiva-katselija');
SELECT insertkayttooikeus('OIVA_APP', 'KAYTTAJA', 'Oiva-käyttäjä');
SELECT insertkayttooikeus('OIVA_APP', 'NIMENKIRJOITTAJA', 'Oiva-nimenkirjoittaja');
SELECT insertkayttooikeus('OIVA_APP', 'PAAKAYTTAJA', 'Oiva-pääkäyttäjä');
SELECT insertkayttooikeus('OIVA_APP', 'YLEISSIVISTAVA_ESITTELIJA', 'Yleissivistävä esittelijä');
SELECT insertkayttooikeus('OIVA_APP', 'YLEISSIVISTAVA_KATSELIJA', 'Yleissivistävä katselija');
SELECT insertkayttooikeus('OIVA_APP', 'YLEISSIVISTAVA_MUOKKAAJA', 'Yleissivistävä muokkaaja');
SELECT insertkayttooikeus('OIVA_APP', 'YLEISSIVISTAVA_NIMENKIRJOITTAJA', 'Yleissivistävä nimenkirjoittaja');
SELECT insertkayttooikeus('OIVA_APP', 'YLLAPITAJA', 'Oiva-ylläpitäjä');
SELECT insertkayttooikeus('OMATTIEDOT', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('OMATTIEDOT', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('OMATTIEDOT', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('OPPIJANTUNNISTUS', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'DUPLICATE_READ', 'Duplikaattihaku');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'DUPLIKAATTINAKYMA', 'Duplikaattinäkymä');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'HENKILON_RU', 'Henkilön luku- ja muokkausoikeus');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'MANUAALINEN_YKSILOINTI', 'Manuaalinen yksilöinti');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'MUUTOSTIETOPALVELU', 'Muutostieto palvelukäyttäjä');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'OPPIJOIDENTUONTI', 'Oppijoiden tuonti');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'REKISTERINPITAJA', 'Rekisterinpitäjä');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'REKISTERINPITAJA_READ', 'rekisterinpitäjä read');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'SCHEDULE', 'Ajastusoikeus');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'TUONTIDATA_READ', 'Tuontidatan tarkastelu');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'VTJ_VERTAILUNAKYMA', 'VTJ vertailunäkymä');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'YKSILOINNIN_PURKU', 'Yksilöinnin purku');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'YLEISTUNNISTE_LUONTI', 'Yleistunnisteen luonti');
SELECT insertkayttooikeus('ORGANISAATIOHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('ORGANISAATIOHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('ORGANISAATIOHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('ORGANISAATIOHALLINTA', 'RYHMA', 'Ryhmän käyttöoikeus');
SELECT insertkayttooikeus('ORGANISAATIOHALLINTA', 'VASTUUKAYTTAJAT', 'Vastuukäyttäjät');
SELECT insertkayttooikeus('ORGANISAATIOIDEN_REKISTEROITYMINEN', 'JOTPA', 'Jotpa-käyttäjän oikeudet');
SELECT insertkayttooikeus('ORGANISAATIOIDEN_REKISTEROITYMINEN', 'OPH', 'OPH-virkailijan oikeudet');
SELECT insertkayttooikeus('ORGANISAATIOIDEN_REKISTEROITYMINEN', 'VARDA', 'Varda-käyttäjän oikeudet');
SELECT insertkayttooikeus('OSOITE', 'CRUD', 'Osoitepalvelun ylläpitäjät');
SELECT insertkayttooikeus('OTI', 'CRUD', 'Tutkintorekisterin ylläpito');
SELECT insertkayttooikeus('PALAUTE', 'PALAUTE_CREATE', 'Palautteen luontioikeus');
SELECT insertkayttooikeus('PALAUTE', 'PALAUTE_READ', 'Palautteen lukuoikeus');
SELECT insertkayttooikeus('RAPORTOINTI', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('RAPORTOINTI', 'KK', 'KK-Raportointi');
SELECT insertkayttooikeus('RAPORTOINTI', 'OPO', 'Hakemuspalvelun opinto-ohjaajat');
SELECT insertkayttooikeus('RAPORTOINTI', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('RAPORTOINTI', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('RAPORTOINTI', 'VALINTAKAYTTAJA', 'Valintakäyttäjä');
SELECT insertkayttooikeus('RYHMASAHKOPOSTI', 'SEND', 'Ryhmäsähköpostin lähetys');
SELECT insertkayttooikeus('RYHMASAHKOPOSTI', 'VIEW', 'Ryhmäsähköpostin raportointi');
SELECT insertkayttooikeus('SIJOITTELU', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('SIJOITTELU', 'PERUUNTUNEIDEN_HYVAKSYNTA', 'Peruuntuneiden hyväksyntä');
SELECT insertkayttooikeus('SIJOITTELU', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('SIJOITTELU', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('SISALLONHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('SISALLONHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('SISALLONHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('SUORITUSREKISTERI', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('SUORITUSREKISTERI', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('SUORITUSREKISTERI', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('SUORITUSREKISTERI', 'VALINTA', 'Hakeneet ja valitut');
SELECT insertkayttooikeus('SUORITUSREKISTERI', 'VALPAS_READ', 'Valpas-rajapinnan lukuoikeus');
SELECT insertkayttooikeus('TARJONTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('TARJONTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('TARJONTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('TARJONTA_KK', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('TARJONTA_KK', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('TARJONTA_KK', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('TIEDONSIIRTO', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('TIEDONSIIRTO', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('TIEDONSIIRTO', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('TIEDONSIIRTO', 'VALINTA', 'Hakeneet ja valitut');
SELECT insertkayttooikeus('ULKOISETRAJAPINNAT', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEET', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEET', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEET', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEETKK', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEETKK', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEETKK', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA_KK', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA_KK', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA_KK', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTATULOSSERVICE', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTATULOSSERVICE', 'KELA_READ', 'Kela API:n lukuoikeus');
SELECT insertkayttooikeus('VALINTATULOSSERVICE', 'MIGRI_READ', 'Migri API:n lukuoikeus');
SELECT insertkayttooikeus('VALINTATULOSSERVICE', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTATULOSSERVICE', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINEN', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINEN', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINEN', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINEN', 'TOISEN_ASTEEN_MUSIIKKIALAN_VALINTAKAYTTAJA', '2. asteen musiikkialan valintakäyttäjä');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINEN', 'TULOSTENTUONTI', 'Tulosten tuonti');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINENKK', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINENKK', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINENKK', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALPAS', 'KELA', 'Oikeus käyttää Valppaan KELA-APIa');
SELECT insertkayttooikeus('VALPAS', 'KUNTA', 'Saa käsitellä kunnan valvontatietoja');
SELECT insertkayttooikeus('VALPAS', 'OPPILAITOS', 'Saa käsitellä oppilaitoksen valvontatietoja');
SELECT insertkayttooikeus('VALPAS', 'OPPILAITOS_HAKEUTUMINEN', 'Saa käsitellä oppilaitoksen hakeutumisen valvonnan tietoja');
SELECT insertkayttooikeus('VALPAS', 'OPPILAITOS_MAKSUTTOMUUS', 'Saa käsitellä oppilaitoksen opintojen maksuttomuuden määrittelyn tietoja');
SELECT insertkayttooikeus('VALPAS', 'OPPILAITOS_SUORITTAMINEN', 'Saa käsitellä oppilaitoksen oppivelvollisuuden suorittamisen valvonnan tietoja');
SELECT insertkayttooikeus('VALPAS', 'YTL', 'Oikeus käyttää Valppaan YTL-APIa');
SELECT insertkayttooikeus('VALSSI', 'PAAKAYTTAJA', 'Kutsuu/hallinnoi valssi käyttäjiä oman organisaatio sisällä: Pääkäyttäjä, Rinnakkaispääkäyttäjä, Toteuttaja');
SELECT insertkayttooikeus('VALSSI', 'TOTEUTTAJA', 'Toteuttaja');
SELECT insertkayttooikeus('VALSSI', 'YLLAPITAJA', 'Kutsuu/hallinnoi kaikkia valssi käyttäjiä: Ylläpitäjä, Pääkäyttäjä');
SELECT insertkayttooikeus('VALTIONAVUSTUS', 'ADMIN', 'Pääkäyttäjä');
SELECT insertkayttooikeus('VALTIONAVUSTUS', 'CRUD', 'Luku-, muokkaus-, ja poisto-oikeus');
SELECT insertkayttooikeus('VALTIONAVUSTUS', 'USER', 'Käyttäjä');
SELECT insertkayttooikeus('VARDA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VARDA', 'HENKILOSTO_TAYDENNYSKOULUTUS_KATSELIJA', 'Henkilöstö täydennyskoulutus katselija');
SELECT insertkayttooikeus('VARDA', 'HENKILOSTO_TAYDENNYSKOULUTUS_TALLENTAJA', 'Henkilöstö täydennyskoulutus tallentaja');
SELECT insertkayttooikeus('VARDA', 'HENKILOSTO_TILAPAISET_KATSELIJA', 'Henkilöstö tilapäiset katselija');
SELECT insertkayttooikeus('VARDA', 'HENKILOSTO_TILAPAISET_TALLENTAJA', 'Henkilöstö tilapäiset tallentaja');
SELECT insertkayttooikeus('VARDA', 'HENKILOSTO_TYONTEKIJA_KATSELIJA', 'Henkilöstö työntekijä katselija');
SELECT insertkayttooikeus('VARDA', 'HENKILOSTO_TYONTEKIJA_TALLENTAJA', 'Henkilöstö työntekijä tallentaja');
SELECT insertkayttooikeus('VARDA', 'HUOLTAJATIETO_KATSELU',   'Varda-huoltajatietojen katselija' );
SELECT insertkayttooikeus('VARDA', 'HUOLTAJATIETO_TALLENNUS', 'Varda-huoltajatietojen tallentaja');
SELECT insertkayttooikeus('VARDA', 'TUEN_TIEDOT_KATSELIJA', 'Tuen-tiedot-katselija');
SELECT insertkayttooikeus('VARDA', 'TUEN_TIEDOT_TALLENTAJA', 'Tuen-tiedot-tallentaja');
SELECT insertkayttooikeus('VARDA', 'VARDA-KATSELIJA', 'VARDA-katselija');
SELECT insertkayttooikeus('VARDA', 'VARDA-PAAKAYTTAJA', 'VARDA-pääkäyttäjä');
SELECT insertkayttooikeus('VARDA', 'VARDA-PALVELUKAYTTAJA', 'VARDA-palvelukäyttäjä');
SELECT insertkayttooikeus('VARDA', 'VARDA-TALLENTAJA', 'VARDA-tallentaja');
SELECT insertkayttooikeus('VARDA', 'VARDA-YLLAPITAJA', 'VARDA-ylläpitäjä');
SELECT insertkayttooikeus('VARDA', 'VARDA_LUOVUTUSPALVELU', 'Varda luovutuspalvelu');
SELECT insertkayttooikeus('VARDA', 'VARDA_RAPORTTIEN_KATSELIJA', 'Katselee tiedonsiirtoraportteja käyttöliittymässä');
SELECT insertkayttooikeus('VARDA', 'VARDA_TOIMIJATIEDOT_KATSELIJA', 'Katselee toimijatietoja käyttöliittymässä');
SELECT insertkayttooikeus('VARDA', 'VARDA_TOIMIJATIEDOT_TALLENTAJA', 'Ylläpitää toimijatietoja käyttöliittymässä');
SELECT insertkayttooikeus('VIESTINVALITYS', 'KATSELU', 'Saa katsella viestinvälityspalvelun kaikkia viestejä');
SELECT insertkayttooikeus('VIESTINVALITYS', 'LAHETYS', 'Saa lähettää viestejä viestinvälityspalvelun kautta');
SELECT insertkayttooikeus('VIESTINVALITYS', 'OPH_PAAKAYTTAJA', 'Saa käyttää viestinvälityspalvelun kaikkea toiminnallisuutta ilman rajoituksia');
SELECT insertkayttooikeus('VIRKAILIJANTYOPOYTA', '2ASTE', '2. aste');
SELECT insertkayttooikeus('VIRKAILIJANTYOPOYTA', 'CRUD', 'Luku-, muokkaus-, ja poisto-oikeus');
SELECT insertkayttooikeus('VIRKAILIJANTYOPOYTA', 'KK', 'Korkeakoulu');
SELECT insertkayttooikeus('VIRKAILIJANTYOPOYTA', 'MUUT', 'Muut');
SELECT insertkayttooikeus('VIRKAILIJANTYOPOYTA', 'PERUS', 'Perusopetus');
SELECT insertkayttooikeus('VIRKAILIJANTYOPOYTA', 'VARDA', 'Varhaiskasvatus');
SELECT insertkayttooikeus('VKT', 'PAAKAYTTAJA', 'Pääkäyttäjä');
SELECT insertkayttooikeus('VOS', 'KATSELIJA', 'Katselija');
SELECT insertkayttooikeus('VOS', 'PAAKAYTTAJA', 'Pääkäyttäjä');
SELECT insertkayttooikeus('VOS', 'TALLENTAJA', 'Tallentaja');
SELECT insertkayttooikeus('VOS', 'TIETOJEN_HYVAKSYJA', 'Tietojen hyväksyjä');
SELECT insertkayttooikeus('VOS', 'TIETOJEN_SYOTTAJA', 'Tietojen syöttäjä');
SELECT insertkayttooikeus('YHTEYSTIETOTYYPPIENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('YHTEYSTIETOTYYPPIENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('YHTEYSTIETOTYYPPIENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('YKI', 'JARJESTAJA', 'Järjestäjä');
SELECT insertkayttooikeus('YKI', 'YLLAPITAJA', 'Ylläpitäjä');
SELECT insertkayttooikeus('YKSITYISTEN_REKISTEROITYMINEN', 'CRUD', 'Rekisteröitymisten luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('YTLMATERIAALITILAUS', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('YTLMATERIAALITILAUS', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('YTLMATERIAALITILAUS', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('YTLTULOSLUETTELO', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('YTLTULOSLUETTELO', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('YTLTULOSLUETTELO', 'READ_UPDATE', 'Luku- ja muokkausoikeus');