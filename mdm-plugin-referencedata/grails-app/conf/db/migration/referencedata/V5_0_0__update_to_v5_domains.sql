ALTER TABLE referencedata.reference_data_model
    ADD path TEXT NOT NULL;

ALTER TABLE referencedata.reference_data_value
    ADD path TEXT NOT NULL;

ALTER TABLE referencedata.reference_summary_metadata
    ADD path TEXT NOT NULL;

ALTER TABLE referencedata.reference_summary_metadata_report
    ADD path TEXT NOT NULL;

