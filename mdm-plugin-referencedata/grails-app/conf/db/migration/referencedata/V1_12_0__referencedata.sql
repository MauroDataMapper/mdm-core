CREATE SCHEMA IF NOT EXISTS referencedata;

CREATE TABLE referencedata.join_referenceDataElement_to_facet (
    referenceDataElement_id UUID NOT NULL,
    classifier_id           UUID,
    annotation_id           UUID,
    semantic_link_id        UUID,
    reference_file_id       UUID,
    metadata_id             UUID
);
CREATE TABLE referencedata.join_referenceDataModel_to_facet (
    referenceDataModel_id UUID NOT NULL,
    classifier_id         UUID,
    annotation_id         UUID,
    semantic_link_id      UUID,
    version_link_id       UUID,
    reference_file_id     UUID,
    metadata_id           UUID
);
CREATE TABLE referencedata.join_referenceDataType_to_facet (
    referenceDataType_id UUID NOT NULL,
    classifier_id        UUID,
    annotation_id        UUID,
    semantic_link_id     UUID,
    reference_file_id    UUID,
    metadata_id          UUID
);
CREATE TABLE referencedata.join_referenceDataValue_to_facet (
    referenceDataValue_id UUID NOT NULL,
    classifier_id         UUID,
    annotation_id         UUID,
    semantic_link_id      UUID,
    reference_file_id     UUID,
    metadata_id           UUID
);
CREATE TABLE referencedata.join_referenceEnumerationValue_to_facet (
    referenceEnumerationValue_id UUID NOT NULL,
    classifier_id                UUID,
    annotation_id                UUID,
    semantic_link_id             UUID,
    reference_file_id            UUID,
    metadata_id                  UUID
);
CREATE TABLE referencedata.reference_data_element (
    id                      UUID         NOT NULL,
    version                 INT8         NOT NULL,
    date_created            TIMESTAMP    NOT NULL,
    reference_data_type_id  UUID         NOT NULL,
    reference_data_model_id UUID         NOT NULL,
    last_updated            TIMESTAMP    NOT NULL,
    path                    TEXT         NOT NULL,
    depth                   INT4         NOT NULL,
    min_multiplicity        INT4,
    max_multiplicity        INT4,
    breadcrumb_tree_id      UUID         NOT NULL,
    idx                     INT4         NOT NULL,
    created_by              VARCHAR(255) NOT NULL,
    aliases_string          TEXT,
    label                   TEXT         NOT NULL,
    description             TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE referencedata.reference_data_element_reference_summary_metadata (
    reference_data_element_reference_summary_metadata_id UUID NOT NULL,
    reference_summary_metadata_id                        UUID
);
CREATE TABLE referencedata.reference_data_model (
    id                              UUID         NOT NULL,
    version                         INT8         NOT NULL,
    branch_name                     VARCHAR(255) NOT NULL,
    date_created                    TIMESTAMP    NOT NULL,
    finalised                       BOOLEAN      NOT NULL,
    readable_by_authenticated_users BOOLEAN      NOT NULL,
    date_finalised                  TIMESTAMP,
    documentation_version           VARCHAR(255) NOT NULL,
    readable_by_everyone            BOOLEAN      NOT NULL,
    model_type                      VARCHAR(255) NOT NULL,
    last_updated                    TIMESTAMP    NOT NULL,
    organisation                    VARCHAR(255),
    deleted                         BOOLEAN      NOT NULL,
    author                          VARCHAR(255),
    breadcrumb_tree_id              UUID         NOT NULL,
    model_version                   VARCHAR(255),
    folder_id                       UUID         NOT NULL,
    authority_id                    UUID         NOT NULL,
    created_by                      VARCHAR(255) NOT NULL,
    aliases_string                  TEXT,
    label                           TEXT         NOT NULL,
    description                     TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE referencedata.reference_data_model_reference_summary_metadata (
    reference_data_model_reference_summary_metadata_id UUID NOT NULL,
    reference_summary_metadata_id                      UUID
);
CREATE TABLE referencedata.reference_data_type (
    id                      UUID         NOT NULL,
    version                 INT8         NOT NULL,
    date_created            TIMESTAMP    NOT NULL,
    reference_data_model_id UUID         NOT NULL,
    domain_type             VARCHAR(30)  NOT NULL,
    last_updated            TIMESTAMP    NOT NULL,
    path                    TEXT         NOT NULL,
    depth                   INT4         NOT NULL,
    breadcrumb_tree_id      UUID         NOT NULL,
    idx                     INT4         NOT NULL,
    created_by              VARCHAR(255) NOT NULL,
    aliases_string          TEXT,
    label                   TEXT         NOT NULL,
    description             TEXT,
    class                   VARCHAR(255) NOT NULL,
    units                   VARCHAR(255),
    PRIMARY KEY (id)
);
CREATE TABLE referencedata.reference_data_type_reference_summary_metadata (
    reference_data_type_reference_summary_metadata_id UUID NOT NULL,
    reference_summary_metadata_id                     UUID
);
CREATE TABLE referencedata.reference_enumeration_value (
    id                            UUID         NOT NULL,
    version                       INT8         NOT NULL,
    date_created                  TIMESTAMP    NOT NULL,
    value                         TEXT         NOT NULL,
    reference_enumeration_type_id UUID         NOT NULL,
    last_updated                  TIMESTAMP    NOT NULL,
    path                          TEXT         NOT NULL,
    depth                         INT4         NOT NULL,
    breadcrumb_tree_id            UUID         NOT NULL,
    idx                           INT4         NOT NULL,
    category                      TEXT,
    created_by                    VARCHAR(255) NOT NULL,
    aliases_string                TEXT,
    key                           TEXT         NOT NULL,
    label                         TEXT         NOT NULL,
    description                   TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE referencedata.reference_summary_metadata (
    id                         UUID         NOT NULL,
    version                    INT8         NOT NULL,
    summary_metadata_type      VARCHAR(255) NOT NULL,
    date_created               TIMESTAMP    NOT NULL,
    last_updated               TIMESTAMP    NOT NULL,
    catalogue_item_domain_type VARCHAR(255) NOT NULL,
    catalogue_item_id          UUID,
    created_by                 VARCHAR(255) NOT NULL,
    label                      TEXT         NOT NULL,
    description                TEXT,
    PRIMARY KEY (id)
);
CREATE TABLE referencedata.reference_summary_metadata_report (
    id                  UUID         NOT NULL,
    version             INT8         NOT NULL,
    date_created        TIMESTAMP    NOT NULL,
    last_updated        TIMESTAMP    NOT NULL,
    report_date         TIMESTAMP    NOT NULL,
    created_by          VARCHAR(255) NOT NULL,
    report_value        TEXT         NOT NULL,
    summary_metadata_id UUID         NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE referencedata.reference_data_value (
    id                        UUID         NOT NULL,
    version                   INT8         NOT NULL,
    date_created              TIMESTAMP    NOT NULL,
    value                     TEXT,
    reference_data_model_id   UUID         NOT NULL,
    reference_data_element_id UUID         NOT NULL,
    row_number                INT8         NOT NULL,
    last_updated              TIMESTAMP    NOT NULL,
    path                      TEXT         NOT NULL,
    depth                     INT4         NOT NULL,
    breadcrumb_tree_id        UUID         NOT NULL,
    idx                       INT4         NOT NULL,
    created_by                VARCHAR(255) NOT NULL,
    aliases_string            TEXT,
    label                     TEXT,
    description               TEXT,
    PRIMARY KEY (id)
);
CREATE INDEX data_element_data_type_idx ON referencedata.reference_data_element(reference_data_type_id);
CREATE INDEX data_element_reference_data_model_idx ON referencedata.reference_data_element(reference_data_model_id);
CREATE INDEX referenceDataElement_created_by_idx ON referencedata.reference_data_element(created_by);
CREATE INDEX referenceDataModel_created_by_idx ON referencedata.reference_data_model(created_by);
CREATE INDEX data_type_reference_data_model_idx ON referencedata.reference_data_type(reference_data_model_id);
CREATE INDEX referenceDataType_created_by_idx ON referencedata.reference_data_type(created_by);
CREATE INDEX referenceEnumerationValue_created_by_idx ON referencedata.reference_enumeration_value(created_by);
CREATE INDEX referenceSummaryMetadata_created_by_idx ON referencedata.reference_summary_metadata(created_by);
CREATE INDEX referenceSummaryMetadataReport_created_by_idx ON referencedata.reference_summary_metadata_report(created_by);
CREATE INDEX summary_metadata_report_summary_metadata_idx ON referencedata.reference_summary_metadata_report(summary_metadata_id);
CREATE INDEX reference_data_value_reference_data_element_idx ON referencedata.reference_data_value(reference_data_element_id);
CREATE INDEX reference_data_value_reference_data_model_idx ON referencedata.reference_data_value(reference_data_model_id);
ALTER TABLE IF EXISTS referencedata.join_referenceDataElement_to_facet
    ADD CONSTRAINT FK2fki0p2nnwaurehb5cjttuvix FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS referencedata.join_referenceDataElement_to_facet
    ADD CONSTRAINT FK2ls8wxo2ymrl7lpcys7j0xv3b FOREIGN KEY (referenceDataElement_id) REFERENCES referencedata.reference_data_element;
ALTER TABLE IF EXISTS referencedata.join_referenceDataElement_to_facet
    ADD CONSTRAINT FKd3a65vscren7g42xw4rahy6g5 FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS referencedata.join_referenceDataElement_to_facet
    ADD CONSTRAINT FKb7mrla3ru59iox823w8cgdiy0 FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS referencedata.join_referenceDataElement_to_facet
    ADD CONSTRAINT FKrltsh3bwdh88lysiui0euxus8 FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS referencedata.join_referenceDataElement_to_facet
    ADD CONSTRAINT FKqp0ri5bm3hvss6s1j3pyonkxr FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS referencedata.join_referenceDataModel_to_facet
    ADD CONSTRAINT FK3jbl1c288a9m1wp6hpira3esu FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS referencedata.join_referenceDataModel_to_facet
    ADD CONSTRAINT FK8gio5kn4wbjxsb3vpxno2guty FOREIGN KEY (referenceDataModel_id) REFERENCES referencedata.reference_data_model;
ALTER TABLE IF EXISTS referencedata.join_referenceDataModel_to_facet
    ADD CONSTRAINT FKjiqw3v6crj988n5addti0ar4u FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS referencedata.join_referenceDataModel_to_facet
    ADD CONSTRAINT FK8jwrx0ncwyb64s7d9ygmjr2f7 FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS referencedata.join_referenceDataModel_to_facet
    ADD CONSTRAINT FKpq9dfcuckjwcdeh9n54r062e0 FOREIGN KEY (version_link_id) REFERENCES core.version_link;
ALTER TABLE IF EXISTS referencedata.join_referenceDataModel_to_facet
    ADD CONSTRAINT FKksgi9yaaa427xe5saynb6rd2i FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS referencedata.join_referenceDataModel_to_facet
    ADD CONSTRAINT FKtlkajagcv38bnatcquinb7p2v FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS referencedata.join_referenceDataType_to_facet
    ADD CONSTRAINT FK3vwe6oyjkdap164w7imcng9vx FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS referencedata.join_referenceDataType_to_facet
    ADD CONSTRAINT FKser4c5ad6dkspbnyjl2r1yuj3 FOREIGN KEY (referenceDataType_id) REFERENCES referencedata.reference_data_type;
ALTER TABLE IF EXISTS referencedata.join_referenceDataType_to_facet
    ADD CONSTRAINT FK7j8ag77c03icvomcohocy682d FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS referencedata.join_referenceDataType_to_facet
    ADD CONSTRAINT FKag55g7g8434y1497a6jmldxlr FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS referencedata.join_referenceDataType_to_facet
    ADD CONSTRAINT FKbw5w6fr1vaf9v0pcu7qs81nvu FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS referencedata.join_referenceDataType_to_facet
    ADD CONSTRAINT FKggbf0ml2ou4b2k525xrb1mxq6 FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS referencedata.join_referenceEnumerationValue_to_facet
    ADD CONSTRAINT FKp2io00cx587eojmbl5v27g7m3 FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS referencedata.join_referenceEnumerationValue_to_facet
    ADD CONSTRAINT FKclc83k4qxd0yxfspwkkttsjmj FOREIGN KEY (referenceEnumerationValue_id) REFERENCES referencedata.reference_enumeration_value;
ALTER TABLE IF EXISTS referencedata.join_referenceEnumerationValue_to_facet
    ADD CONSTRAINT FK2cfjn7dvabjkphwvne3jmhu24 FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS referencedata.join_referenceEnumerationValue_to_facet
    ADD CONSTRAINT FK87toxbm4bddbchculnipo9876 FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS referencedata.join_referenceEnumerationValue_to_facet
    ADD CONSTRAINT FKemx1xs8y5xnl1a6kdu18mp3us FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS referencedata.join_referenceEnumerationValue_to_facet
    ADD CONSTRAINT FKq50iqxdtfqwh3x6mdaepsx143 FOREIGN KEY (metadata_id) REFERENCES core.metadata;
ALTER TABLE IF EXISTS referencedata.reference_data_element
    ADD CONSTRAINT FK72aidiwlq9doq630milqmpt0h FOREIGN KEY (reference_data_type_id) REFERENCES referencedata.reference_data_type;
ALTER TABLE IF EXISTS referencedata.reference_data_element
    ADD CONSTRAINT FK5s8ym98wxlmji2cwd5c2uqx51 FOREIGN KEY (reference_data_model_id) REFERENCES referencedata.reference_data_model;
ALTER TABLE IF EXISTS referencedata.reference_data_element
    ADD CONSTRAINT FKfmyjc00b03urjiavamg30vryh FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS referencedata.reference_data_element_reference_summary_metadata
    ADD CONSTRAINT FKcamqn6r8ruu47dfcwd8t5khre FOREIGN KEY (reference_summary_metadata_id) REFERENCES referencedata.reference_summary_metadata;
ALTER TABLE IF EXISTS referencedata.reference_data_element_reference_summary_metadata
    ADD CONSTRAINT FKh9lqs0iqoqr674lvpfl01m4f FOREIGN KEY (reference_data_element_reference_summary_metadata_id) REFERENCES referencedata.reference_data_element;
ALTER TABLE IF EXISTS referencedata.reference_data_model
    ADD CONSTRAINT FKk0dbj4ejwa3rpnm87ten7l650 FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS referencedata.reference_data_model
    ADD CONSTRAINT FK8dvr6bt8lf5xtces9vstu3h9i FOREIGN KEY (folder_id) REFERENCES core.folder;
ALTER TABLE IF EXISTS referencedata.reference_data_model
    ADD CONSTRAINT FK7jnsebhp01jrvj1cnoiglnk36 FOREIGN KEY (authority_id) REFERENCES core.authority;
ALTER TABLE IF EXISTS referencedata.reference_data_model_reference_summary_metadata
    ADD CONSTRAINT FKe9f307usil36627eebfyncmas FOREIGN KEY (reference_summary_metadata_id) REFERENCES referencedata.reference_summary_metadata;
ALTER TABLE IF EXISTS referencedata.reference_data_model_reference_summary_metadata
    ADD CONSTRAINT FK7018d6f5ebbikvy7ka0mmlwug FOREIGN KEY (reference_data_model_reference_summary_metadata_id) REFERENCES referencedata.reference_data_model;
ALTER TABLE IF EXISTS referencedata.reference_data_type
    ADD CONSTRAINT FKn6ied2qohp1b9guvwcsskng2b FOREIGN KEY (reference_data_model_id) REFERENCES referencedata.reference_data_model;
ALTER TABLE IF EXISTS referencedata.reference_data_type
    ADD CONSTRAINT FK21bionqtblyjus0xdx0fpxsd0 FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS referencedata.reference_data_type_reference_summary_metadata
    ADD CONSTRAINT FKrydmedwui8knor4eiktsfrw1r FOREIGN KEY (reference_summary_metadata_id) REFERENCES referencedata.reference_summary_metadata;
ALTER TABLE IF EXISTS referencedata.reference_data_type_reference_summary_metadata
    ADD CONSTRAINT FKkps5yegy08ndnnd8ic3bgk56q FOREIGN KEY (reference_data_type_reference_summary_metadata_id) REFERENCES referencedata.reference_data_type;
ALTER TABLE IF EXISTS referencedata.reference_enumeration_value
    ADD CONSTRAINT FKfcsl5wvgo4hhgd32kio4vsxke FOREIGN KEY (reference_enumeration_type_id) REFERENCES referencedata.reference_data_type;
ALTER TABLE IF EXISTS referencedata.reference_enumeration_value
    ADD CONSTRAINT FKdh4kk2d1frpb2rfep76o7d6v8 FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS referencedata.reference_summary_metadata_report
    ADD CONSTRAINT FKtm1k29089tgksd63i7yjaha8g FOREIGN KEY (summary_metadata_id) REFERENCES referencedata.reference_summary_metadata;
ALTER TABLE IF EXISTS referencedata.reference_data_value
    ADD CONSTRAINT FK3ru68cbfsr7cx03c1szowx23u FOREIGN KEY (reference_data_model_id) REFERENCES referencedata.reference_data_model;
ALTER TABLE IF EXISTS referencedata.reference_data_value
    ADD CONSTRAINT FKbq7kc2ry1zi21fch17trnjtm8 FOREIGN KEY (breadcrumb_tree_id) REFERENCES core.breadcrumb_tree;
ALTER TABLE IF EXISTS referencedata.reference_data_value
    ADD CONSTRAINT FKuknlrsbwja5t5vd84ceulvn9p FOREIGN KEY (reference_data_element_id) REFERENCES referencedata.reference_data_element;
ALTER TABLE IF EXISTS referencedata.join_referencedatavalue_to_facet
    ADD CONSTRAINT FK9kjgg5wj8npozbd0fuj982g6p FOREIGN KEY (classifier_id) REFERENCES core.classifier;
ALTER TABLE IF EXISTS referencedata.join_referencedatavalue_to_facet
    ADD CONSTRAINT FK477k0y3842t22demljgfjjryt FOREIGN KEY (referencedatavalue_id) REFERENCES referencedata.reference_data_value;
ALTER TABLE IF EXISTS referencedata.join_referencedatavalue_to_facet
    ADD CONSTRAINT FKar18o3cu7vcu1ivvmd8087tgx FOREIGN KEY (semantic_link_id) REFERENCES core.semantic_link;
ALTER TABLE IF EXISTS referencedata.join_referencedatavalue_to_facet
    ADD CONSTRAINT FKbbb79ed9t0u7v1gw3l4eksed7 FOREIGN KEY (annotation_id) REFERENCES core.annotation;
ALTER TABLE IF EXISTS referencedata.join_referencedatavalue_to_facet
    ADD CONSTRAINT FKenneayqmmakd3jjtz0rc9ww2z FOREIGN KEY (reference_file_id) REFERENCES core.reference_file;
ALTER TABLE IF EXISTS referencedata.join_referencedatavalue_to_facet
    ADD CONSTRAINT FKs3h18ie0xp4vrwnmvgwajqkt8 FOREIGN KEY (metadata_id) REFERENCES core.metadata;
