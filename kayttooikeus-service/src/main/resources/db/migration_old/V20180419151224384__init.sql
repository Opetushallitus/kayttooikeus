--
-- PostgreSQL database dump
--

-- Dumped from database version 9.5.10
-- Dumped by pg_dump version 9.5.12

SET statement_timeout = 0;
SET lock_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;


--
-- Name: EXTENSION pg_trgm; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: anomus; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.anomus (
    id bigint NOT NULL,
    version bigint NOT NULL,
    anomuksentila character varying(255),
    anomustilatapahtumapvm timestamp without time zone,
    anomustyyppi character varying(255),
    anottupvm timestamp without time zone,
    matkapuhelinnumero character varying(255),
    organisaatiooid character varying(255),
    perustelut character varying(255),
    puhelinnumero character varying(255),
    sahkopostiosoite character varying(255) NOT NULL,
    tehtavanimike character varying(255),
    henkilo_id bigint,
    kasittelija_henkilo_id bigint,
    hylkaamisperuste character varying(255)
)
WITH (autovacuum_vacuum_scale_factor='0.0', autovacuum_vacuum_threshold='1000', autovacuum_analyze_scale_factor='0.0', autovacuum_analyze_threshold='1000');


ALTER TABLE public.anomus OWNER TO oph;

--
-- Name: anomus_myonnettykayttooikeus; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.anomus_myonnettykayttooikeus (
    kayttooikeus_id bigint NOT NULL,
    kayttooikeusryhma_id bigint NOT NULL
);


ALTER TABLE public.anomus_myonnettykayttooikeus OWNER TO oph;

--
-- Name: anomus_myonnettykayttooikeusryhmas; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.anomus_myonnettykayttooikeusryhmas (
    anomus_id bigint NOT NULL,
    myonnettykayttooikeusryhma_id bigint NOT NULL
);


ALTER TABLE public.anomus_myonnettykayttooikeusryhmas OWNER TO oph;

--
-- Name: externalid; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.externalid (
    id bigint NOT NULL,
    version bigint NOT NULL,
    externalid character varying(255) NOT NULL,
    henkilo_id bigint NOT NULL
);


ALTER TABLE public.externalid OWNER TO oph;

--
-- Name: haettu_kayttooikeusryhma; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.haettu_kayttooikeusryhma (
    id bigint NOT NULL,
    version bigint NOT NULL,
    anomus_id bigint,
    kayttooikeusryhma_id bigint,
    kasittelypvm timestamp without time zone,
    tyyppi character varying(255)
)
WITH (autovacuum_vacuum_scale_factor='0.0', autovacuum_vacuum_threshold='1000', autovacuum_analyze_scale_factor='0.0', autovacuum_analyze_threshold='1000');


ALTER TABLE public.haettu_kayttooikeusryhma OWNER TO oph;

--
-- Name: henkilo; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.henkilo (
    id bigint NOT NULL,
    oidhenkilo character varying(255) NOT NULL,
    henkilotyyppi character varying(255),
    etunimet_cached character varying(255),
    sukunimi_cached character varying(255),
    passivoitu_cached boolean,
    duplicate_cached boolean,
    vahvasti_tunnistettu boolean DEFAULT false,
    hetu_cached character varying(255),
    kutsumanimi_cached character varying(255)
)
WITH (autovacuum_vacuum_scale_factor='0.0', autovacuum_vacuum_threshold='1000', autovacuum_analyze_scale_factor='0.0', autovacuum_analyze_threshold='1000');


ALTER TABLE public.henkilo OWNER TO oph;


--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: oph
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO oph;

--
-- Name: identification; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.identification (
    id bigint NOT NULL,
    version bigint NOT NULL,
    authtoken character varying(255),
    email character varying(255),
    identifier character varying(255) NOT NULL,
    idpentityid character varying(255) NOT NULL,
    henkilo_id bigint NOT NULL,
    expiration_date timestamp without time zone,
    auth_token_created timestamp without time zone
)
WITH (autovacuum_vacuum_scale_factor='0.0', autovacuum_vacuum_threshold='1000', autovacuum_analyze_scale_factor='0.0', autovacuum_analyze_threshold='1000');


ALTER TABLE public.identification OWNER TO oph;


--
-- Name: kayttajatiedot; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.kayttajatiedot (
    id bigint NOT NULL,
    version bigint NOT NULL,
    password character varying(255),
    salt character varying(255),
    henkiloid bigint NOT NULL,
    createdat timestamp without time zone DEFAULT now() NOT NULL,
    invalidated boolean DEFAULT false NOT NULL,
    username character varying(255)
)
WITH (autovacuum_vacuum_scale_factor='0.0', autovacuum_vacuum_threshold='1000', autovacuum_analyze_scale_factor='0.0', autovacuum_analyze_threshold='1000');


ALTER TABLE public.kayttajatiedot OWNER TO oph;

--
-- Name: kayttooikeus; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.kayttooikeus (
    id bigint NOT NULL,
    version bigint NOT NULL,
    palvelu_id bigint NOT NULL,
    rooli character varying(255),
    textgroup_id bigint
);


ALTER TABLE public.kayttooikeus OWNER TO oph;

--
-- Name: kayttooikeusryhma; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.kayttooikeusryhma (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    textgroup_id bigint,
    hidden boolean DEFAULT false NOT NULL,
    rooli_rajoite character varying(255),
    kuvaus_id bigint,
    ryhma_restriction boolean DEFAULT false NOT NULL
);


ALTER TABLE public.kayttooikeusryhma OWNER TO oph;

--
-- Name: kayttooikeusryhma_kayttooikeus; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.kayttooikeusryhma_kayttooikeus (
    kayttooikeusryhma_id bigint NOT NULL,
    kayttooikeus_id bigint NOT NULL
);


ALTER TABLE public.kayttooikeusryhma_kayttooikeus OWNER TO oph;

--
-- Name: kayttooikeusryhma_myontoviite; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.kayttooikeusryhma_myontoviite (
    id bigint NOT NULL,
    version bigint NOT NULL,
    kayttooikeusryhma_master_id bigint NOT NULL,
    kayttooikeusryhma_slave_id bigint NOT NULL
);


ALTER TABLE public.kayttooikeusryhma_myontoviite OWNER TO oph;

--
-- Name: kayttooikeusryhma_tapahtuma_historia; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.kayttooikeusryhma_tapahtuma_historia (
    id bigint NOT NULL,
    version bigint NOT NULL,
    aikaleima timestamp without time zone NOT NULL,
    tila character varying(255) NOT NULL,
    syy character varying(255),
    kayttooikeusryhma_id bigint NOT NULL,
    organisaatiohenkilo_id bigint NOT NULL,
    kasittelija_henkilo_id bigint NOT NULL
)
WITH (autovacuum_vacuum_scale_factor='0.0', autovacuum_vacuum_threshold='1000', autovacuum_analyze_scale_factor='0.0', autovacuum_analyze_threshold='1000');


ALTER TABLE public.kayttooikeusryhma_tapahtuma_historia OWNER TO oph;


--
-- Name: kutsu; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.kutsu (
    id bigint NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    aikaleima timestamp without time zone DEFAULT now() NOT NULL,
    kutsuja_oid character varying(128) NOT NULL,
    tila character varying(64) DEFAULT 'AVOIN'::character varying NOT NULL,
    sahkoposti character varying(256) NOT NULL,
    salaisuus character varying(128),
    kaytetty timestamp without time zone,
    poistettu timestamp without time zone,
    poistaja_oid character varying(128),
    luotu_henkilo_oid character varying(128),
    kieli_koodi character varying(2) NOT NULL,
    etunimi text NOT NULL,
    sukunimi text NOT NULL,
    temporary_token character varying(128),
    hetu character varying(128),
    temporary_token_created timestamp without time zone,
    haka_identifier character varying(255),
    CONSTRAINT kutsu_tila CHECK (((tila)::text = ANY (ARRAY[('AVOIN'::character varying)::text, ('KAYTETTY'::character varying)::text, ('POISTETTU'::character varying)::text])))
);


ALTER TABLE public.kutsu OWNER TO oph;

--
-- Name: kutsu_organisaatio; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.kutsu_organisaatio (
    id bigint NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    kutsu bigint NOT NULL,
    organisaatio_oid character varying(128) NOT NULL
);


ALTER TABLE public.kutsu_organisaatio OWNER TO oph;

--
-- Name: kutsu_organisaatio_ryhma; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.kutsu_organisaatio_ryhma (
    kutsu_organisaatio bigint NOT NULL,
    ryhma bigint NOT NULL
);


ALTER TABLE public.kutsu_organisaatio_ryhma OWNER TO oph;

--
-- Name: ldap_synchronization_data; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.ldap_synchronization_data (
    id bigint NOT NULL,
    version bigint NOT NULL,
    last_run timestamp without time zone NOT NULL,
    avg_update_time integer NOT NULL,
    total_runtime integer NOT NULL,
    total_amount integer NOT NULL,
    cooloff boolean DEFAULT false NOT NULL,
    run_batch boolean DEFAULT false NOT NULL
);


ALTER TABLE public.ldap_synchronization_data OWNER TO oph;

--
-- Name: ldap_update_data; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.ldap_update_data (
    id bigint NOT NULL,
    version bigint NOT NULL,
    priority integer NOT NULL,
    henkilo_oid character varying(255),
    kor_id bigint,
    status integer NOT NULL,
    modified timestamp without time zone NOT NULL
);


ALTER TABLE public.ldap_update_data OWNER TO oph;

--
-- Name: myonnetty_kayttooikeusryhma_tapahtuma; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.myonnetty_kayttooikeusryhma_tapahtuma (
    id bigint NOT NULL,
    version bigint NOT NULL,
    aikaleima timestamp without time zone NOT NULL,
    syy character varying(255),
    tila character varying(255) NOT NULL,
    kasittelija_henkilo_id bigint,
    kayttooikeusryhma_id bigint NOT NULL,
    organisaatiohenkilo_id bigint NOT NULL,
    voimassaalkupvm date NOT NULL,
    voimassaloppupvm date
)
WITH (autovacuum_vacuum_scale_factor='0.0', autovacuum_vacuum_threshold='1000', autovacuum_analyze_scale_factor='0.0', autovacuum_analyze_threshold='1000');


ALTER TABLE public.myonnetty_kayttooikeusryhma_tapahtuma OWNER TO oph;

--
-- Name: organisaatio_cache; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.organisaatio_cache (
    organisaatio_oid character varying(255) NOT NULL,
    organisaatio_oid_path character varying(255)
);


ALTER TABLE public.organisaatio_cache OWNER TO oph;

--
-- Name: organisaatiohenkilo; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.organisaatiohenkilo (
    id bigint NOT NULL,
    version bigint NOT NULL,
    matkapuhelinnumero character varying(255),
    organisaatio_oid character varying(255) NOT NULL,
    puhelinnumero character varying(255),
    sahkopostiosoite character varying(255),
    tehtavanimike character varying(255),
    henkilo_id bigint NOT NULL,
    passivoitu boolean DEFAULT false NOT NULL,
    tyyppi character varying(255),
    voimassa_alku_pvm date,
    voimassa_loppu_pvm date
)
WITH (autovacuum_vacuum_scale_factor='0.0', autovacuum_vacuum_threshold='1000', autovacuum_analyze_scale_factor='0.0', autovacuum_analyze_threshold='1000');


ALTER TABLE public.organisaatiohenkilo OWNER TO oph;

--
-- Name: organisaatioviite; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.organisaatioviite (
    id bigint NOT NULL,
    version bigint NOT NULL,
    kayttooikeusryhma_id bigint NOT NULL,
    organisaatio_tyyppi character varying(255)
);


ALTER TABLE public.organisaatioviite OWNER TO oph;

--
-- Name: palvelu; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.palvelu (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    palvelutyyppi character varying(255),
    textgroup_id bigint,
    kokoelma_id bigint
);


ALTER TABLE public.palvelu OWNER TO oph;


--
-- Name: schedule_timestamps; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.schedule_timestamps (
    id bigint NOT NULL,
    version bigint NOT NULL,
    modified timestamp without time zone NOT NULL,
    identifier character varying(255) NOT NULL
);


ALTER TABLE public.schedule_timestamps OWNER TO oph;

--
-- Name: scheduled_tasks; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.scheduled_tasks (
    task_name text NOT NULL,
    task_instance text NOT NULL,
    task_data bytea,
    execution_time timestamp with time zone NOT NULL,
    picked boolean NOT NULL,
    picked_by text,
    last_success timestamp with time zone,
    last_failure timestamp with time zone,
    last_heartbeat timestamp with time zone,
    version bigint NOT NULL
);


ALTER TABLE public.scheduled_tasks OWNER TO oph;


--
-- Name: text; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.text (
    id bigint NOT NULL,
    version bigint NOT NULL,
    lang character varying(255),
    text text,
    textgroup_id bigint
);


ALTER TABLE public.text OWNER TO oph;

--
-- Name: text_group; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.text_group (
    id bigint NOT NULL,
    version bigint NOT NULL
);


ALTER TABLE public.text_group OWNER TO oph;


--
-- Name: tunnistus_token; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.tunnistus_token (
    id bigint NOT NULL,
    version bigint NOT NULL,
    login_token character varying(255),
    henkilo_id bigint NOT NULL,
    aikaleima timestamp without time zone,
    kaytetty timestamp without time zone,
    hetu character varying(255),
    salasanan_vaihto boolean
);


ALTER TABLE public.tunnistus_token OWNER TO oph;


--
-- Name: varmennus_poletti; Type: TABLE; Schema: public; Owner: oph
--

CREATE TABLE public.varmennus_poletti (
    id bigint NOT NULL,
    version bigint NOT NULL,
    poletti character varying(255) NOT NULL,
    tyyppi character varying(255) NOT NULL,
    voimassa timestamp without time zone NOT NULL,
    henkilo_id bigint NOT NULL
);


ALTER TABLE public.varmennus_poletti OWNER TO oph;


--
-- Name: anomus_myonnettykayttooikeus_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.anomus_myonnettykayttooikeus
    ADD CONSTRAINT anomus_myonnettykayttooikeus_pkey PRIMARY KEY (kayttooikeus_id, kayttooikeusryhma_id);


--
-- Name: anomus_myonnettykayttooikeusryhmas_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.anomus_myonnettykayttooikeusryhmas
    ADD CONSTRAINT anomus_myonnettykayttooikeusryhmas_pkey PRIMARY KEY (anomus_id, myonnettykayttooikeusryhma_id);


--
-- Name: anomus_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.anomus
    ADD CONSTRAINT anomus_pkey PRIMARY KEY (id);



--
-- Name: externalid_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.externalid
    ADD CONSTRAINT externalid_pkey PRIMARY KEY (id);


--
-- Name: haettu_kayttooikeusryhma_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.haettu_kayttooikeusryhma
    ADD CONSTRAINT haettu_kayttooikeusryhma_pkey PRIMARY KEY (id);


--
-- Name: henkilo_oidhenkilo_key; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.henkilo
    ADD CONSTRAINT henkilo_oidhenkilo_key UNIQUE (oidhenkilo);


--
-- Name: henkilo_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.henkilo
    ADD CONSTRAINT henkilo_pkey PRIMARY KEY (id);


--
-- Name: identification_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.identification
    ADD CONSTRAINT identification_pkey PRIMARY KEY (id);


--
-- Name: kayttooikeus_organisaatio_unique; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.myonnetty_kayttooikeusryhma_tapahtuma
    ADD CONSTRAINT kayttooikeus_organisaatio_unique UNIQUE (kayttooikeusryhma_id, organisaatiohenkilo_id);


--
-- Name: kayttooikeus_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeus
    ADD CONSTRAINT kayttooikeus_pkey PRIMARY KEY (id);


--
-- Name: kayttooikeusryhma_kayttooikeus_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma_kayttooikeus
    ADD CONSTRAINT kayttooikeusryhma_kayttooikeus_pkey PRIMARY KEY (kayttooikeusryhma_id, kayttooikeus_id);


--
-- Name: kayttooikeusryhma_name_key; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma
    ADD CONSTRAINT kayttooikeusryhma_name_key UNIQUE (name);


--
-- Name: kayttooikeusryhma_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma
    ADD CONSTRAINT kayttooikeusryhma_pkey PRIMARY KEY (id);


--
-- Name: kayttooikeusryhma_tapahtuma_historia_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma_tapahtuma_historia
    ADD CONSTRAINT kayttooikeusryhma_tapahtuma_historia_pkey PRIMARY KEY (id);


--
-- Name: kutsu_organisaatio_kutsu_organisaatio_oid_key; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kutsu_organisaatio
    ADD CONSTRAINT kutsu_organisaatio_kutsu_organisaatio_oid_key UNIQUE (kutsu, organisaatio_oid);


--
-- Name: kutsu_organisaatio_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kutsu_organisaatio
    ADD CONSTRAINT kutsu_organisaatio_pkey PRIMARY KEY (id);


--
-- Name: kutsu_organisaatio_ryhma_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kutsu_organisaatio_ryhma
    ADD CONSTRAINT kutsu_organisaatio_ryhma_pkey PRIMARY KEY (kutsu_organisaatio, ryhma);


--
-- Name: kutsu_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kutsu
    ADD CONSTRAINT kutsu_pkey PRIMARY KEY (id);


--
-- Name: kutsu_salaisuus_key; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kutsu
    ADD CONSTRAINT kutsu_salaisuus_key UNIQUE (salaisuus);


--
-- Name: myonnetty_kayttooikeusryhma_tapahtuma_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.myonnetty_kayttooikeusryhma_tapahtuma
    ADD CONSTRAINT myonnetty_kayttooikeusryhma_tapahtuma_pkey PRIMARY KEY (id);


--
-- Name: organisaatio_cache_key; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.organisaatio_cache
    ADD CONSTRAINT organisaatio_cache_key PRIMARY KEY (organisaatio_oid);


--
-- Name: organisaatiohenkilo_organisaatio_oid_key; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.organisaatiohenkilo
    ADD CONSTRAINT organisaatiohenkilo_organisaatio_oid_key UNIQUE (organisaatio_oid, henkilo_id);


--
-- Name: organisaatiohenkilo_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.organisaatiohenkilo
    ADD CONSTRAINT organisaatiohenkilo_pkey PRIMARY KEY (id);


--
-- Name: organisaatioviite_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.organisaatioviite
    ADD CONSTRAINT organisaatioviite_pkey PRIMARY KEY (id);


--
-- Name: palvelu_name_key; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.palvelu
    ADD CONSTRAINT palvelu_name_key UNIQUE (name);


--
-- Name: palvelu_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.palvelu
    ADD CONSTRAINT palvelu_pkey PRIMARY KEY (id);


--
-- Name: password_henkiloid_key; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttajatiedot
    ADD CONSTRAINT password_henkiloid_key UNIQUE (henkiloid);


--
-- Name: password_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttajatiedot
    ADD CONSTRAINT password_pkey PRIMARY KEY (id);


--
-- Name: schedule_timestamps_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.schedule_timestamps
    ADD CONSTRAINT schedule_timestamps_pkey PRIMARY KEY (id);


--
-- Name: scheduled_tasks_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.scheduled_tasks
    ADD CONSTRAINT scheduled_tasks_pkey PRIMARY KEY (task_name, task_instance);


--
-- Name: text_group_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.text_group
    ADD CONSTRAINT text_group_pkey PRIMARY KEY (id);


--
-- Name: text_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.text
    ADD CONSTRAINT text_pkey PRIMARY KEY (id);


--
-- Name: varmennus_poletti_pkey; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.varmennus_poletti
    ADD CONSTRAINT varmennus_poletti_pkey PRIMARY KEY (id);


--
-- Name: varmennus_poletti_poletti_key; Type: CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.varmennus_poletti
    ADD CONSTRAINT varmennus_poletti_poletti_key UNIQUE (poletti);


--
-- Name: avoin_kutsu_sahkoposti; Type: INDEX; Schema: public; Owner: oph
--

CREATE UNIQUE INDEX avoin_kutsu_sahkoposti ON public.kutsu USING btree (sahkoposti) WHERE ((tila)::text = 'AVOIN'::text);


--
-- Name: externalid_externalid_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE UNIQUE INDEX externalid_externalid_idx ON public.externalid USING btree (externalid);


--
-- Name: henkilo_etunimet_cached_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX henkilo_etunimet_cached_idx ON public.henkilo USING btree (lower((etunimet_cached)::text) text_pattern_ops);


--
-- Name: henkilo_hetu_cached_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX henkilo_hetu_cached_idx ON public.henkilo USING btree (hetu_cached);


--
-- Name: henkilo_kutsumanimi_cached_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX henkilo_kutsumanimi_cached_idx ON public.henkilo USING btree (lower((kutsumanimi_cached)::text) text_pattern_ops);


--
-- Name: henkilo_oid_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX henkilo_oid_idx ON public.henkilo USING btree (oidhenkilo);


--
-- Name: henkilo_sukunimi_cached_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX henkilo_sukunimi_cached_idx ON public.henkilo USING btree (lower((sukunimi_cached)::text) text_pattern_ops);


--
-- Name: identification_henkilo_id_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX identification_henkilo_id_idx ON public.identification USING btree (henkilo_id);


--
-- Name: identifier_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX identifier_idx ON public.identification USING btree (identifier);


--
-- Name: kayttooikeusryhma_kayttooikeus_kayttooikeusryhma_id_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX kayttooikeusryhma_kayttooikeus_kayttooikeusryhma_id_idx ON public.kayttooikeusryhma_kayttooikeus USING btree (kayttooikeusryhma_id);


--
-- Name: ldap_updater_oid; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX ldap_updater_oid ON public.ldap_update_data USING btree (henkilo_oid);


--
-- Name: ldap_updater_prio; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX ldap_updater_prio ON public.ldap_update_data USING btree (priority);


--
-- Name: ldap_updater_status; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX ldap_updater_status ON public.ldap_update_data USING btree (status);


--
-- Name: myonnetty_alkupvm_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX myonnetty_alkupvm_idx ON public.myonnetty_kayttooikeusryhma_tapahtuma USING btree (voimassaalkupvm);


--
-- Name: myonnetty_kayttooikeusryhma_tapahtuma_kayttooikeusryhma_id_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX myonnetty_kayttooikeusryhma_tapahtuma_kayttooikeusryhma_id_idx ON public.myonnetty_kayttooikeusryhma_tapahtuma USING btree (kayttooikeusryhma_id);


--
-- Name: myonnetty_kayttooikeusryhma_tapahtuma_organisaatiohenkilo_id_id; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX myonnetty_kayttooikeusryhma_tapahtuma_organisaatiohenkilo_id_id ON public.myonnetty_kayttooikeusryhma_tapahtuma USING btree (organisaatiohenkilo_id);


--
-- Name: myonnetty_loppupvm_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX myonnetty_loppupvm_idx ON public.myonnetty_kayttooikeusryhma_tapahtuma USING btree (voimassaloppupvm);


--
-- Name: organisaatio_henkilo_pass_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX organisaatio_henkilo_pass_idx ON public.organisaatiohenkilo USING btree (passivoitu);


--
-- Name: organisaatiohenkilo_henkilo_id_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX organisaatiohenkilo_henkilo_id_idx ON public.organisaatiohenkilo USING btree (henkilo_id);


--
-- Name: orghenkilo_oid_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX orghenkilo_oid_idx ON public.organisaatiohenkilo USING btree (organisaatio_oid);


--
-- Name: password_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX password_idx ON public.kayttajatiedot USING btree (password);


--
-- Name: password_salt_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX password_salt_idx ON public.kayttajatiedot USING btree (salt);


--
-- Name: username_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX username_idx ON public.kayttajatiedot USING btree (username);


--
-- Name: varmennus_poletti_henkilo_id_idx; Type: INDEX; Schema: public; Owner: oph
--

CREATE INDEX varmennus_poletti_henkilo_id_idx ON public.varmennus_poletti USING btree (henkilo_id);


--
-- Name: fk178e889798562292; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma
    ADD CONSTRAINT fk178e889798562292 FOREIGN KEY (textgroup_id) REFERENCES public.text_group(id);


--
-- Name: fk187d426e620670b2; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.identification
    ADD CONSTRAINT fk187d426e620670b2 FOREIGN KEY (henkilo_id) REFERENCES public.henkilo(id);


--
-- Name: fk31a855dd562181e2; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.anomus_myonnettykayttooikeusryhmas
    ADD CONSTRAINT fk31a855dd562181e2 FOREIGN KEY (anomus_id) REFERENCES public.anomus(id);


--
-- Name: fk32b4b1526d467183; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma_myontoviite
    ADD CONSTRAINT fk32b4b1526d467183 FOREIGN KEY (kayttooikeusryhma_slave_id) REFERENCES public.kayttooikeusryhma(id);


--
-- Name: fk36452d98562292; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.text
    ADD CONSTRAINT fk36452d98562292 FOREIGN KEY (textgroup_id) REFERENCES public.text_group(id);


--
-- Name: fk43643dc1562181e2; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.haettu_kayttooikeusryhma
    ADD CONSTRAINT fk43643dc1562181e2 FOREIGN KEY (anomus_id) REFERENCES public.anomus(id);


--
-- Name: fk43643dc16f540452; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.haettu_kayttooikeusryhma
    ADD CONSTRAINT fk43643dc16f540452 FOREIGN KEY (kayttooikeusryhma_id) REFERENCES public.kayttooikeusryhma(id);


--
-- Name: fk4a1451e27a46214c; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma_myontoviite
    ADD CONSTRAINT fk4a1451e27a46214c FOREIGN KEY (kayttooikeusryhma_master_id) REFERENCES public.kayttooikeusryhma(id);


--
-- Name: fk4c641ebbdc0430b7; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttajatiedot
    ADD CONSTRAINT fk4c641ebbdc0430b7 FOREIGN KEY (henkiloid) REFERENCES public.henkilo(id);


--
-- Name: fk68b0d63f48dedded; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.anomus_myonnettykayttooikeus
    ADD CONSTRAINT fk68b0d63f48dedded FOREIGN KEY (kayttooikeus_id) REFERENCES public.anomus(id);


--
-- Name: fk752d36c9620670b2; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.anomus
    ADD CONSTRAINT fk752d36c9620670b2 FOREIGN KEY (henkilo_id) REFERENCES public.henkilo(id);


--
-- Name: fk752d36c9e20ca904; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.anomus
    ADD CONSTRAINT fk752d36c9e20ca904 FOREIGN KEY (kasittelija_henkilo_id) REFERENCES public.henkilo(id);


--
-- Name: fk7870a32fe20ca904; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.myonnetty_kayttooikeusryhma_tapahtuma
    ADD CONSTRAINT fk7870a32fe20ca904 FOREIGN KEY (kasittelija_henkilo_id) REFERENCES public.henkilo(id);


--
-- Name: fk7bdcd693620670b2; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.organisaatiohenkilo
    ADD CONSTRAINT fk7bdcd693620670b2 FOREIGN KEY (henkilo_id) REFERENCES public.henkilo(id);


--
-- Name: fk_externalid_henkilo; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.externalid
    ADD CONSTRAINT fk_externalid_henkilo FOREIGN KEY (henkilo_id) REFERENCES public.henkilo(id);


--
-- Name: fk_kayttooikeusryhma_textgroup_kuvaus; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma
    ADD CONSTRAINT fk_kayttooikeusryhma_textgroup_kuvaus FOREIGN KEY (kuvaus_id) REFERENCES public.text_group(id);


--
-- Name: fkbba031266cad2be2; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma_kayttooikeus
    ADD CONSTRAINT fkbba031266cad2be2 FOREIGN KEY (kayttooikeus_id) REFERENCES public.kayttooikeus(id);


--
-- Name: fkbba031266f540452; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma_kayttooikeus
    ADD CONSTRAINT fkbba031266f540452 FOREIGN KEY (kayttooikeusryhma_id) REFERENCES public.kayttooikeusryhma(id);


--
-- Name: fkce0453a546b127a; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.myonnetty_kayttooikeusryhma_tapahtuma
    ADD CONSTRAINT fkce0453a546b127a FOREIGN KEY (kayttooikeusryhma_id) REFERENCES public.kayttooikeusryhma(id);


--
-- Name: fkce0623ed4db1575; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.myonnetty_kayttooikeusryhma_tapahtuma
    ADD CONSTRAINT fkce0623ed4db1575 FOREIGN KEY (organisaatiohenkilo_id) REFERENCES public.organisaatiohenkilo(id);


--
-- Name: fkce0627ec2d90112; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeus
    ADD CONSTRAINT fkce0627ec2d90112 FOREIGN KEY (palvelu_id) REFERENCES public.palvelu(id);


--
-- Name: fkd069179395232062; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.palvelu
    ADD CONSTRAINT fkd069179395232062 FOREIGN KEY (kokoelma_id) REFERENCES public.palvelu(id);


--
-- Name: fkd069179398562292; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.palvelu
    ADD CONSTRAINT fkd069179398562292 FOREIGN KEY (textgroup_id) REFERENCES public.text_group(id);


--
-- Name: fkd29a21fd314634c3; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma_tapahtuma_historia
    ADD CONSTRAINT fkd29a21fd314634c3 FOREIGN KEY (kasittelija_henkilo_id) REFERENCES public.henkilo(id);


--
-- Name: fkd2a5a3123d4653a7; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma_tapahtuma_historia
    ADD CONSTRAINT fkd2a5a3123d4653a7 FOREIGN KEY (kayttooikeusryhma_id) REFERENCES public.kayttooikeusryhma(id);


--
-- Name: fkd2b4a1e23d4671c1; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kayttooikeusryhma_tapahtuma_historia
    ADD CONSTRAINT fkd2b4a1e23d4671c1 FOREIGN KEY (organisaatiohenkilo_id) REFERENCES public.organisaatiohenkilo(id);


--
-- Name: fkd2c4a4cf690670e9; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.organisaatioviite
    ADD CONSTRAINT fkd2c4a4cf690670e9 FOREIGN KEY (kayttooikeusryhma_id) REFERENCES public.kayttooikeusryhma(id);


--
-- Name: fked24a4cf620670b2; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.varmennus_poletti
    ADD CONSTRAINT fked24a4cf620670b2 FOREIGN KEY (henkilo_id) REFERENCES public.henkilo(id);


--
-- Name: kutsu_luotu_henkilo_oid_fkey; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kutsu
    ADD CONSTRAINT kutsu_luotu_henkilo_oid_fkey FOREIGN KEY (luotu_henkilo_oid) REFERENCES public.henkilo(oidhenkilo);


--
-- Name: kutsu_organisaatio_kutsu_fkey; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kutsu_organisaatio
    ADD CONSTRAINT kutsu_organisaatio_kutsu_fkey FOREIGN KEY (kutsu) REFERENCES public.kutsu(id);


--
-- Name: kutsu_organisaatio_ryhma_kutsu_organisaatio_fkey; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kutsu_organisaatio_ryhma
    ADD CONSTRAINT kutsu_organisaatio_ryhma_kutsu_organisaatio_fkey FOREIGN KEY (kutsu_organisaatio) REFERENCES public.kutsu_organisaatio(id);


--
-- Name: kutsu_organisaatio_ryhma_ryhma_fkey; Type: FK CONSTRAINT; Schema: public; Owner: oph
--

ALTER TABLE ONLY public.kutsu_organisaatio_ryhma
    ADD CONSTRAINT kutsu_organisaatio_ryhma_ryhma_fkey FOREIGN KEY (ryhma) REFERENCES public.kayttooikeusryhma(id);


--
-- PostgreSQL database dump complete
--

