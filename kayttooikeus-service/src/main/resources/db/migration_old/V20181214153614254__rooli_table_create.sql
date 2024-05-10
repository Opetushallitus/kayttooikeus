CREATE TABLE if not exists rooli (
    id bigint NOT NULL,
    version bigint NOT NULL,
    name character varying(255) NOT NULL,
    textgroup_id bigint
);
