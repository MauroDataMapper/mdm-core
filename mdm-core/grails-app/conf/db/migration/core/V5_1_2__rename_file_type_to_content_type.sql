UPDATE core.domain_export
SET export_content_type = export_file_type
WHERE export_content_type IS NULL;

ALTER TABLE core.domain_export
    ALTER COLUMN export_content_type SET NOT NULL;

ALTER TABLE core.domain_export
    DROP COLUMN export_file_type;