CREATE TABLE federation.subscribed_catalogue_authentication_credentials (
    id                       UUID         NOT NULL PRIMARY KEY,
    version                  INT8         NOT NULL,
    subscribed_catalogue_id  UUID         NOT NULL,
    api_key                  TEXT,
    token_url                TEXT,
    client_id                TEXT,
    client_secret            TEXT,
    access_token             TEXT,
    access_token_expiry_time TIMESTAMP,
    class                    VARCHAR(255) NOT NULL
);

ALTER TABLE federation.subscribed_catalogue
    ADD subscribed_catalogue_authentication_type VARCHAR(255) NOT NULL DEFAULT 'NO_AUTHENTICATION';

UPDATE federation.subscribed_catalogue
SET subscribed_catalogue_authentication_type = 'API_KEY'
WHERE api_key IS NOT NULL;

INSERT INTO federation.subscribed_catalogue_authentication_credentials(id, version, subscribed_catalogue_id, api_key, token_url, client_id, client_secret, access_token,
                                                                       access_token_expiry_time, class)
SELECT uuid_generate_v1(),
       0,
       subscribed_catalogue.id,
       subscribed_catalogue.api_key::text,
       NULL,
       NULL,
       NULL,
       NULL,
       NULL,
       'uk.ac.ox.softeng.maurodatamapper.federation.authentication.ApiKeyAuthenticationCredentials'
FROM federation.subscribed_catalogue
WHERE subscribed_catalogue.subscribed_catalogue_authentication_type = 'API_KEY';

ALTER TABLE federation.subscribed_catalogue
    DROP COLUMN api_key;