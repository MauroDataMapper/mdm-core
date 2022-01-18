ALTER TABLE datamodel.data_model
    ADD path TEXT NOT NULL;

ALTER TABLE datamodel.summary_metadata
    ADD path TEXT NOT NULL;

ALTER TABLE datamodel.summary_metadata_report
    ADD path TEXT NOT NULL;