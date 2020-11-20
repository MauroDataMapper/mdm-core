/**
  Setup all the tables for managing flyway
 */

CREATE SCHEMA IF NOT EXISTS datamodel;

CREATE TABLE datamodel.flyway_schema_history (
    installed_rank INT4                       NOT NULL
        CONSTRAINT flyway_schema_history_pk
            PRIMARY KEY,
    version        VARCHAR(50),
    description    VARCHAR(200)               NOT NULL,
    type           VARCHAR(20)                NOT NULL,
    script         VARCHAR(1000)              NOT NULL,
    checksum       INT4,
    installed_by   VARCHAR(100)               NOT NULL,
    installed_on   TIMESTAMP(6) DEFAULT now() NOT NULL,
    execution_time INT4                       NOT NULL,
    success        BOOL                       NOT NULL
);

CREATE INDEX flyway_schema_history_s_idx
    ON datamodel.flyway_schema_history(success);

CREATE SCHEMA IF NOT EXISTS security;

CREATE TABLE security.flyway_schema_history (
    installed_rank INT4                       NOT NULL
        CONSTRAINT flyway_schema_history_pk
            PRIMARY KEY,
    version        VARCHAR(50),
    description    VARCHAR(200)               NOT NULL,
    type           VARCHAR(20)                NOT NULL,
    script         VARCHAR(1000)              NOT NULL,
    checksum       INT4,
    installed_by   VARCHAR(100)               NOT NULL,
    installed_on   TIMESTAMP(6) DEFAULT now() NOT NULL,
    execution_time INT4                       NOT NULL,
    success        BOOL                       NOT NULL
);

CREATE INDEX flyway_schema_history_s_idx
    ON security.flyway_schema_history(success);

CREATE SCHEMA IF NOT EXISTS terminology;

CREATE TABLE terminology.flyway_schema_history (
    installed_rank INT4                       NOT NULL
        CONSTRAINT flyway_schema_history_pk
            PRIMARY KEY,
    version        VARCHAR(50),
    description    VARCHAR(200)               NOT NULL,
    type           VARCHAR(20)                NOT NULL,
    script         VARCHAR(1000)              NOT NULL,
    checksum       INT4,
    installed_by   VARCHAR(100)               NOT NULL,
    installed_on   TIMESTAMP(6) DEFAULT now() NOT NULL,
    execution_time INT4                       NOT NULL,
    success        BOOL                       NOT NULL
);

CREATE INDEX flyway_schema_history_s_idx
    ON terminology.flyway_schema_history(success);

CREATE SCHEMA IF NOT EXISTS dataflow;

CREATE TABLE dataflow.flyway_schema_history (
    installed_rank INT4                       NOT NULL
        CONSTRAINT flyway_schema_history_pk
            PRIMARY KEY,
    version        VARCHAR(50),
    description    VARCHAR(200)               NOT NULL,
    type           VARCHAR(20)                NOT NULL,
    script         VARCHAR(1000)              NOT NULL,
    checksum       INT4,
    installed_by   VARCHAR(100)               NOT NULL,
    installed_on   TIMESTAMP(6) DEFAULT now() NOT NULL,
    execution_time INT4                       NOT NULL,
    success        BOOL                       NOT NULL
);

CREATE INDEX flyway_schema_history_s_idx
    ON dataflow.flyway_schema_history(success);

-- As we want to rename referencedatamodel schema we have to have custom handling
DO
$$
    DECLARE
        already_exists INT := (SELECT count(*)
                               FROM information_schema.schemata
                               WHERE schema_name = 'referencedatamodel');
    BEGIN
        IF already_exists = 1
        THEN ALTER SCHEMA referencedatamodel RENAME TO referencedata;
        ELSE
            CREATE SCHEMA IF NOT EXISTS dataflow;
        END IF;
    END;
$$;


CREATE TABLE referencedata.flyway_schema_history (
    installed_rank INT4                       NOT NULL
        CONSTRAINT flyway_schema_history_pk
            PRIMARY KEY,
    version        VARCHAR(50),
    description    VARCHAR(200)               NOT NULL,
    type           VARCHAR(20)                NOT NULL,
    script         VARCHAR(1000)              NOT NULL,
    checksum       INT4,
    installed_by   VARCHAR(100)               NOT NULL,
    installed_on   TIMESTAMP(6) DEFAULT now() NOT NULL,
    execution_time INT4                       NOT NULL,
    success        BOOL                       NOT NULL
);

CREATE INDEX flyway_schema_history_s_idx
    ON referencedata.flyway_schema_history(success);

/**
 Now we need to move the relevant bits into each history table
 */

-- Add the initial schema control statements, but only if the first relevant flyway migration has been run
DO
$$
    DECLARE
        dm_install_dt TIMESTAMP(6) := (SELECT installed_on
                                       FROM core.flyway_schema_history
                                       WHERE version = '1.1.0');
        DECLARE
        s_install_dt  TIMESTAMP(6) := (SELECT installed_on
                                       FROM core.flyway_schema_history
                                       WHERE version = '1.2.0');
        DECLARE
        tm_install_dt TIMESTAMP(6) := (SELECT installed_on
                                       FROM core.flyway_schema_history
                                       WHERE version = '1.3.0');
        DECLARE
        df_install_dt TIMESTAMP(6) := (SELECT installed_on
                                       FROM core.flyway_schema_history
                                       WHERE version = '1.4.0');
        DECLARE
        rd_install_dt TIMESTAMP(6) := (SELECT installed_on
                                       FROM core.flyway_schema_history
                                       WHERE version = '1.12.0');

    BEGIN
        IF dm_install_dt IS NOT NULL
        THEN
            INSERT INTO datamodel.flyway_schema_history(installed_rank, version, description, type, script, checksum, installed_by, installed_on,
                                                        execution_time,
                                                        success)
            VALUES (0, NULL, '<< Flyway Schema Creation >>', 'SCHEMA', '"datamodel"', NULL, 'maurodatamapper', dm_install_dt, 0, TRUE);
        END IF;
        IF s_install_dt IS NOT NULL
        THEN
            INSERT INTO security.flyway_schema_history(installed_rank, version, description, type, script, checksum, installed_by, installed_on,
                                                       execution_time,
                                                       success)
            VALUES (0, NULL, '<< Flyway Schema Creation >>', 'SCHEMA', '"security"', NULL, 'maurodatamapper', s_install_dt, 0, TRUE);
        END IF;
        IF tm_install_dt IS NOT NULL
        THEN
            INSERT INTO terminology.flyway_schema_history(installed_rank, version, description, type, script, checksum, installed_by, installed_on,
                                                          execution_time,
                                                          success)
            VALUES (0, NULL, '<< Flyway Schema Creation >>', 'SCHEMA', '"terminology"', NULL, 'maurodatamapper', tm_install_dt, 0, TRUE);
        END IF;
        IF df_install_dt IS NOT NULL
        THEN
            INSERT INTO dataflow.flyway_schema_history(installed_rank, version, description, type, script, checksum, installed_by, installed_on,
                                                       execution_time,
                                                       success)
            VALUES (0, NULL, '<< Flyway Schema Creation >>', 'SCHEMA', '"dataflow"', NULL, 'maurodatamapper', df_install_dt, 0, TRUE);
        END IF;
        IF rd_install_dt IS NOT NULL
        THEN
            INSERT INTO referencedata.flyway_schema_history(installed_rank, version, description, type, script, checksum, installed_by, installed_on,
                                                            execution_time,
                                                            success)
            VALUES (0, NULL, '<< Flyway Schema Creation >>', 'SCHEMA', '"referencedata"', NULL, 'maurodatamapper', rd_install_dt, 0, TRUE);
        END IF;
    END ;
$$;

-- Add all of the migrations which have been applied under the version 1 into the correct history table
INSERT INTO datamodel.flyway_schema_history(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time,
                                            success)
SELECT *
FROM core.flyway_schema_history
WHERE version IS NOT NULL AND
      version IN ('1.1.0', '1.5.1', '1.6.0', '1.9.0', '1.11.0');

INSERT INTO security.flyway_schema_history(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time,
                                           success)
SELECT *
FROM core.flyway_schema_history
WHERE version IS NOT NULL AND
      version IN ('1.2.0', '1.8.0', '1.10.0');

INSERT INTO terminology.flyway_schema_history(installed_rank, version, description, type, script, checksum, installed_by, installed_on,
                                              execution_time,
                                              success)
SELECT *
FROM core.flyway_schema_history
WHERE version IS NOT NULL AND
      version IN ('1.3.0', '1.5.2', '1.6.1');

INSERT INTO dataflow.flyway_schema_history(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time,
                                           success)
SELECT *
FROM core.flyway_schema_history
WHERE version IS NOT NULL AND
      version IN ('1.4.0');

INSERT INTO referencedata.flyway_schema_history(installed_rank, version, description, type, script, checksum, installed_by, installed_on,
                                                execution_time,
                                                success)
SELECT *
FROM core.flyway_schema_history
WHERE version IS NOT NULL AND
      version IN ('1.12.0', '1.14.0');

-- Remove all the non-core migrations
DELETE
FROM core.flyway_schema_history
WHERE version IS NOT NULL AND
      version IN ('1.1.0', '1.5.1', '1.6.0', '1.9.0', '1.11.0',
                  '1.2.0', '1.8.0', '1.10.0',
                  '1.3.0', '1.5.2', '1.6.1',
                  '1.4.0',
                  '1.12.0', '1.14.0'
          );
-- Set the core to have a schema statement of just core
UPDATE core.flyway_schema_history
SET script = '"core"'
WHERE installed_rank = 0;
