ALTER TABLE maurodatamapper.datamodel.data_type
    ADD COLUMN model_resource_id UUID NULL;

ALTER TABLE maurodatamapper.datamodel.data_type
    ADD COLUMN model_resource_domain_type VARCHAR(255) NULL;