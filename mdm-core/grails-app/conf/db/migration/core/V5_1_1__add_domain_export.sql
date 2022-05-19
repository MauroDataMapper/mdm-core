CREATE TABLE core.domain_export (
    id                   UUID         NOT NULL PRIMARY KEY,
    version              BIGINT       NOT NULL,
    export_file_type     VARCHAR(255) NOT NULL,
    multi_domain_export  BOOLEAN      NOT NULL,
    date_created         TIMESTAMP    NOT NULL,
    exporter_namespace   VARCHAR(255) NOT NULL,
    export_file_name     VARCHAR(255) NOT NULL,
    last_updated         TIMESTAMP    NOT NULL,
    path                 TEXT,
    exported_domain_ids  VARCHAR(255),
    exported_domain_type VARCHAR(255) NOT NULL,
    exporter_name        VARCHAR(255) NOT NULL,
    exported_domain_id   UUID,
    export_data          BYTEA        NOT NULL,
    created_by           VARCHAR(255) NOT NULL,
    exporter_version     VARCHAR(255) NOT NULL,
    export_content_type  VARCHAR(255)
);

CREATE INDEX domainexport_created_by_idx
    ON core.domain_export(created_by);

