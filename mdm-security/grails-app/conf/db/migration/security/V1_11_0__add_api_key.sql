CREATE TABLE security.api_key (
    id                UUID         NOT NULL,
    version           INT8         NOT NULL,
    refreshable       BOOLEAN      NOT NULL,
    date_created      TIMESTAMP    NOT NULL,
    expiry_date       DATE         NOT NULL,
    last_updated      TIMESTAMP    NOT NULL,
    disabled          BOOLEAN      NOT NULL,
    catalogue_user_id UUID         NOT NULL,
    name              VARCHAR(255) NOT NULL,
    created_by        VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX apiKey_created_by_idx ON security.api_key(created_by);

ALTER TABLE IF EXISTS security.api_key
    ADD CONSTRAINT UKee162bd1d3e12dac9f8ef55811f7 UNIQUE (catalogue_user_id, name);

ALTER TABLE IF EXISTS security.api_key
    ADD CONSTRAINT FKl8s3q1v3lg1crjh3kmqqbiwcu FOREIGN KEY (catalogue_user_id) REFERENCES security.catalogue_user;
