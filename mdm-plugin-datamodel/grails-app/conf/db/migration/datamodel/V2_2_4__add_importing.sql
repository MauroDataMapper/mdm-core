CREATE TABLE datamodel.join_dataclass_to_imported_data_class (
    imported_dataclass_id UUID NOT NULL,
    dataclass_id          UUID NOT NULL
);
CREATE TABLE datamodel.join_dataclass_to_imported_data_element (
    dataclass_id            UUID NOT NULL,
    imported_dataelement_id UUID NOT NULL
);
CREATE TABLE datamodel.join_datamodel_to_imported_data_class (
    imported_dataclass_id UUID NOT NULL,
    datamodel_id          UUID NOT NULL
);
CREATE TABLE datamodel.join_datamodel_to_imported_data_type (
    imported_datatype_id UUID NOT NULL,
    datamodel_id         UUID NOT NULL
);
ALTER TABLE IF EXISTS datamodel.join_dataclass_to_imported_data_class
    ADD CONSTRAINT FKtfwuhg9cda52duj50ocsed0cl FOREIGN KEY (dataclass_id) REFERENCES datamodel.data_class;
ALTER TABLE IF EXISTS datamodel.join_dataclass_to_imported_data_class
    ADD CONSTRAINT FK8bf683fj07ef7q6ua9ax5sipb FOREIGN KEY (imported_dataclass_id) REFERENCES datamodel.data_class;
ALTER TABLE IF EXISTS datamodel.join_dataclass_to_imported_data_element
    ADD CONSTRAINT FKaywt9cf9pam7w7ieo2kyv64sb FOREIGN KEY (imported_dataelement_id) REFERENCES datamodel.data_element;
ALTER TABLE IF EXISTS datamodel.join_dataclass_to_imported_data_element
    ADD CONSTRAINT FKppmuveyr38fys2lw45kkp8n0s FOREIGN KEY (dataclass_id) REFERENCES datamodel.data_class;
ALTER TABLE IF EXISTS datamodel.join_datamodel_to_imported_data_class
    ADD CONSTRAINT FKhlnup269u21f4tvdkt9sshg51 FOREIGN KEY (datamodel_id) REFERENCES datamodel.data_model;
ALTER TABLE IF EXISTS datamodel.join_datamodel_to_imported_data_class
    ADD CONSTRAINT FKp7q1ry4kxlgldr6vtdqai1bns FOREIGN KEY (imported_dataclass_id) REFERENCES datamodel.data_class;
ALTER TABLE IF EXISTS datamodel.join_datamodel_to_imported_data_type
    ADD CONSTRAINT FKs8icj3nlbxt8bnrtnhpo81lg2 FOREIGN KEY (datamodel_id) REFERENCES datamodel.data_model;
ALTER TABLE IF EXISTS datamodel.join_datamodel_to_imported_data_type
    ADD CONSTRAINT FKbax3mbjn9u65ahhb5t782hq7y FOREIGN KEY (imported_datatype_id) REFERENCES datamodel.data_type;
