--
-- JdbcSessionMappingStorage operates on cas_client_session table and gets
-- confused under special circumstances if same session cannot be connected
-- to multiple CAS sessions. see: KJHH-2045
--
ALTER TABLE cas_client_session DROP CONSTRAINT cas_client_session_session_id_key;
