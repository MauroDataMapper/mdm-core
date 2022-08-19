CREATE TABLE federation.subscribed_catalogue_authentication_credentials (
    id            UUID NOT NULL PRIMARY KEY,
    api_key       UUID,
    client_id     TEXT,
    client_secret TEXT
);