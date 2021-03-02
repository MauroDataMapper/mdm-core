ALTER TABLE IF EXISTS datamodel.join_datamodel_to_facet ADD COLUMN model_import_id UUID NULL;
ALTER TABLE IF EXISTS datamodel.join_datamodel_to_facet ADD CONSTRAINT FKdatamodel_to_model_import FOREIGN KEY (model_import_id) REFERENCES core.model_import;

ALTER TABLE IF EXISTS datamodel.join_dataclass_to_facet ADD COLUMN model_import_id UUID NULL;
ALTER TABLE IF EXISTS datamodel.join_dataclass_to_facet ADD CONSTRAINT FKdataclass_to_model_import FOREIGN KEY (model_import_id) REFERENCES core.model_import;