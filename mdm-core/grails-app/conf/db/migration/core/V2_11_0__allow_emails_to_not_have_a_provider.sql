ALTER TABLE core.email
    ALTER COLUMN email_service_used DROP NOT NULL;

ALTER TABLE core.email
    ALTER COLUMN date_time_sent DROP NOT NULL;