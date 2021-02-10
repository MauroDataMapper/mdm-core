ALTER TABLE core.api_property
    ADD publicly_visible BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE core.api_property
    ADD category VARCHAR(255) NULL;