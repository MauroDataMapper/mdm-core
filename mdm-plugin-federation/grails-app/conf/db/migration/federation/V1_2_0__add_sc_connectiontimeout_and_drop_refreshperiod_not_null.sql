ALTER TABLE IF EXISTS federation.subscribed_catalogue
    ADD COLUMN connection_timeout INT;

ALTER TABLE IF EXISTS federation.subscribed_catalogue
    ALTER COLUMN refresh_period DROP NOT NULL;