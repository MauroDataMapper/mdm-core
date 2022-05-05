ALTER TABLE federation.subscribed_catalogue
    ADD subscribed_catalogue_type VARCHAR(255) NOT NULL DEFAULT 'MDM_JSON';