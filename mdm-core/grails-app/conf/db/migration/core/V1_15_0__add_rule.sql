CREATE TABLE core.rule (
    id                         UUID         NOT NULL,
    version                    INT8         NOT NULL,
    date_created               TIMESTAMP    NOT NULL,
    last_updated               TIMESTAMP    NOT NULL,
    catalogue_item_domain_type VARCHAR(255) NOT NULL,
    catalogue_item_id          UUID,
    name                       TEXT         NULL,
    created_by                 VARCHAR(255) NOT NULL,
    description                TEXT         NULL,
    PRIMARY KEY (id)
);

CREATE INDEX rule_catalogue_item_idx ON core.rule(catalogue_item_id);
CREATE INDEX rule_created_by_idx ON core.rule(created_by);
