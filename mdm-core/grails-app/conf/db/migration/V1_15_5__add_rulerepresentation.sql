CREATE TABLE core.rule_representation (
    id                         UUID         NOT NULL,
    version                    INT8         NOT NULL,
    date_created               TIMESTAMP    NOT NULL,
    last_updated               TIMESTAMP    NOT NULL,
    rule_id                    UUID         NOT NULL,
    language                   TEXT         NOT NULL,
    representation             TEXT         NOT NULL,
    created_by                 VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX rule_representation_rule_idx ON core.rule_representation(rule_id);
CREATE INDEX rule_representation_created_by_idx ON core.rule_representation(created_by);
