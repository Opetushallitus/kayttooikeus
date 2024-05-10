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
