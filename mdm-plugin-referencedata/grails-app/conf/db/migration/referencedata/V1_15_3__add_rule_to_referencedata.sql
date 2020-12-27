ALTER TABLE IF EXISTS referencedata.join_referencedataelement_to_facet
    ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS referencedata.join_referencedataelement_to_facet
    ADD CONSTRAINT FKreferencedataelement_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS referencedata.join_referencedatatype_to_facet
    ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS referencedata.join_referencedatatype_to_facet
    ADD CONSTRAINT FKreferencedatatype_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS referencedata.join_referencedatamodel_to_facet
    ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS referencedata.join_referencedatamodel_to_facet
    ADD CONSTRAINT FKreferencedatamodel_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS referencedata.join_referencedatavalue_to_facet
    ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS referencedata.join_referencedatavalue_to_facet
    ADD CONSTRAINT FKreferencedatavalue_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS referencedata.join_referenceenumerationvalue_to_facet
    ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS referencedata.join_referenceenumerationvalue_to_facet
    ADD CONSTRAINT FKreferenceenumerationvalue_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;