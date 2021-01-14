alter table core.breadcrumb_tree
    drop constraint fk1hraqwgiiva4reb2v6do4it81;

alter table core.breadcrumb_tree
    add constraint fk1hraqwgiiva4reb2v6do4it81
        foreign key (parent_id) references core.breadcrumb_tree
            on delete cascade;

alter table core.rule_representation
    add constraint rule_representation_rule_id_fk
        foreign key (rule_id) references core.rule;