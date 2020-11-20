ALTER TABLE IF EXISTS dataflow.join_dataflow_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS dataflow.join_dataflow_to_facet ADD CONSTRAINT FKdataflow_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS dataflow.join_dataclasscomponent_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS dataflow.join_dataclasscomponent_to_facet ADD CONSTRAINT FKdataclasscomponent_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;

ALTER TABLE IF EXISTS dataflow.join_dataelementcomponent_to_facet ADD COLUMN rule_id UUID;
ALTER TABLE IF EXISTS dataflow.join_dataelementcomponent_to_facet ADD CONSTRAINT FKdataelementcomponent_to_rule FOREIGN KEY (rule_id) REFERENCES core.rule;