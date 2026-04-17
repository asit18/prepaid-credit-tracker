\set db_name `echo ${DB_NAME:-credittracker}`
\set db_user `echo ${DB_USER:-credittracker}`
\set db_password `echo ${DB_PASSWORD:-change-me}`

SELECT format('CREATE DATABASE %I', :'db_name')
WHERE NOT EXISTS (
    SELECT FROM pg_database WHERE datname = :'db_name'
)\gexec

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'db_user', :'db_password')
WHERE NOT EXISTS (
    SELECT FROM pg_roles WHERE rolname = :'db_user'
)\gexec

SELECT format('ALTER ROLE %I LOGIN PASSWORD %L', :'db_user', :'db_password')
WHERE EXISTS (
    SELECT FROM pg_roles WHERE rolname = :'db_user'
)\gexec

SELECT format('GRANT ALL PRIVILEGES ON DATABASE %I TO %I', :'db_name', :'db_user')\gexec

\connect :db_name

SELECT format('GRANT ALL ON SCHEMA public TO %I', :'db_user')\gexec
SELECT format('ALTER SCHEMA public OWNER TO %I', :'db_user')\gexec
SELECT format('ALTER TABLE public.%I OWNER TO %I', tablename, :'db_user')
FROM pg_tables
WHERE schemaname = 'public'\gexec
SELECT format('ALTER SEQUENCE public.%I OWNER TO %I', sequencename, :'db_user')
FROM pg_sequences
WHERE schemaname = 'public'\gexec
