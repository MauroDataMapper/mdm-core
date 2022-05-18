ALTER TABLE federation.subscribed_catalogue
    ADD subscribed_catalogue_type VARCHAR(255) NOT NULL DEFAULT 'MAURO_JSON';

ALTER TABLE federation.subscribed_model
    ALTER COLUMN subscribed_model_id TYPE TEXT USING subscribed_model_id::TEXT;

ALTER TABLE federation.subscribed_model
    ALTER COLUMN subscribed_model_type DROP NOT NULL;