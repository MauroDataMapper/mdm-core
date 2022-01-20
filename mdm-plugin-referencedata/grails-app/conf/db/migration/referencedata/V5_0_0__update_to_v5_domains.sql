ALTER TABLE referencedata.reference_data_model
    ADD path TEXT;
UPDATE referencedata.reference_data_model
SET path = CONCAT('dm:', label, '$', COALESCE(model_version, branch_name));
ALTER TABLE referencedata.reference_data_model
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE referencedata.reference_data_value
    ADD path TEXT;
UPDATE referencedata.reference_data_value
SET path = CONCAT('rdv:', row_number);
ALTER TABLE referencedata.reference_data_value
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE referencedata.reference_summary_metadata
    ADD path TEXT;
UPDATE referencedata.reference_summary_metadata
SET path = CONCAT('sm:', label);
ALTER TABLE referencedata.reference_summary_metadata
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE referencedata.reference_summary_metadata_report
    ADD path TEXT;
UPDATE referencedata.reference_summary_metadata_report
SET path = CONCAT('smr:', to_char(report_date at TIME ZONE 'UTC', 'yyyyMMddHH24MISSUSZ'));
ALTER TABLE referencedata.reference_summary_metadata_report
    ALTER COLUMN path SET NOT NULL;