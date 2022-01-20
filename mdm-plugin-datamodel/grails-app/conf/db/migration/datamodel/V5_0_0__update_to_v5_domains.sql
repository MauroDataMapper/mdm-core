ALTER TABLE datamodel.data_model
    ADD path TEXT;
UPDATE datamodel.data_model
SET path = CONCAT('dm:', label, '$', COALESCE(model_version, branch_name));
ALTER TABLE datamodel.data_model
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE datamodel.summary_metadata
    ADD path TEXT;
UPDATE datamodel.summary_metadata
SET path = CONCAT('sm:', label);
ALTER TABLE datamodel.summary_metadata
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE datamodel.summary_metadata_report
    ADD path TEXT;
UPDATE datamodel.summary_metadata_report
SET path = CONCAT('smr:', to_char(report_date at TIME ZONE 'UTC', 'yyyyMMddHH24MISSUSZ'));
ALTER TABLE datamodel.summary_metadata_report
    ALTER COLUMN path SET NOT NULL;