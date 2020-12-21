ALTER TABLE IF EXISTS terminology.join_terminology_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS terminology.join_terminology_to_facet ADD CONSTRAINT FKterminology_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS terminology.join_term_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS terminology.join_term_to_facet ADD CONSTRAINT FKterm_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS terminology.join_termrelationship_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS terminology.join_termrelationship_to_facet ADD CONSTRAINT FKtermrelationship_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS terminology.join_termrelationshiptype_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS terminology.join_termrelationshiptype_to_facet ADD CONSTRAINT FKtermrelationshiptype_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS terminology.join_codeset_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS terminology.join_codeset_to_facet ADD CONSTRAINT FKcodeset_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

