CREATE TABLE federation.subscribed_catalogue_authentication_credentials (
    id                        UUID         NOT NULL PRIMARY KEY,
    version                   INT8         NOT NULL,
    subscribed_catalogue_id   UUID         NOT NULL,
    api_key                   UUID,
    token_url                 TEXT,
    client_id                 TEXT,
    client_secret             TEXT,
    access_token              TEXT,
    access_token_expiry_time  TIMESTAMP,
    class                     VARCHAR(255) NOT NULL
);

ALTER TABLE federation.subscribed_catalogue
    ADD subscribed_catalogue_authentication_type VARCHAR(255) NOT NULL DEFAULT 'NO_AUTHENTICATION';