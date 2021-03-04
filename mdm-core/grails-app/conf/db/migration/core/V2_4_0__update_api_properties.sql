ALTER TABLE core.api_property
    ADD publicly_visible BOOLEAN DEFAULT FALSE NOT NULL;

ALTER TABLE core.api_property
    ADD category VARCHAR(255) NULL;

UPDATE core.api_property
set category = 'Email'
where starts_with(key, 'email');


UPDATE core.api_property
set category = 'Site'
where starts_with(key, 'site');