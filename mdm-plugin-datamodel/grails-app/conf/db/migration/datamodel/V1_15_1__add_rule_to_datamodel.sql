ALTER TABLE IF EXISTS datamodel.join_dataclass_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS datamodel.join_dataclass_to_facet ADD CONSTRAINT FKdataclass_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS datamodel.join_dataelement_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS datamodel.join_dataelement_to_facet ADD CONSTRAINT FKdataelement_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS datamodel.join_datamodel_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS datamodel.join_datamodel_to_facet ADD CONSTRAINT FKdatamodel_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS datamodel.join_datatype_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS datamodel.join_datatype_to_facet ADD CONSTRAINT FKdatatype_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS datamodel.join_enumerationvalue_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS datamodel.join_enumerationvalue_to_facet ADD CONSTRAINT FKenumerationvalue_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

