CREATE SCHEMA IF NOT EXISTS security;

CREATE TABLE security.catalogue_user (
    id                 UUID         NOT NULL,
    version            INT8         NOT NULL,
    pending            BOOLEAN      NOT NULL,
    salt               BYTEA        NOT NULL,
    date_created       TIMESTAMP    NOT NULL,
    first_name         VARCHAR(255) NOT NULL,
    profile_picture_id UUID,
    last_updated       TIMESTAMP    NOT NULL,
    organisation       VARCHAR(255),
    reset_token        UUID,
    disabled           BOOLEAN      NOT NULL,
    job_title          VARCHAR(255),
    email_address      VARCHAR(255) NOT NULL,
    user_preferences   TEXT,
    password           BYTEA,
    created_by         VARCHAR(255) NOT NULL,
    temp_password      VARCHAR(255),
    last_name          VARCHAR(255) NOT NULL,
    last_login         TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE TABLE security.group_role (
    id                     UUID         NOT NULL,
    version                INT8         NOT NULL,
    date_created           TIMESTAMP    NOT NULL,
    display_name           VARCHAR(255) NOT NULL,
    name                   VARCHAR(255) NOT NULL,
    parent_id              UUID,
    last_updated           TIMESTAMP    NOT NULL,
    path                   TEXT         NOT NULL,
    depth                  INT4         NOT NULL,
    application_level_role BOOLEAN      NOT NULL,
    created_by             VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE security.join_catalogue_user_to_user_group (
    catalogue_user_id UUID NOT NULL,
    user_group_id     UUID NOT NULL,
    PRIMARY KEY (user_group_id, catalogue_user_id)
);
CREATE TABLE security.securable_resource_group_role (
    id                             UUID         NOT NULL,
    version                        INT8         NOT NULL,
    securable_resource_id          UUID         NOT NULL,
    user_group_id                  UUID         NOT NULL,
    date_created                   TIMESTAMP    NOT NULL,
    securable_resource_domain_type VARCHAR(255) NOT NULL,
    last_updated                   TIMESTAMP    NOT NULL,
    group_role_id                  UUID         NOT NULL,
    finalised_model                BOOLEAN,
    created_by                     VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);
CREATE TABLE security.user_group (
    id                        UUID         NOT NULL,
    version                   INT8         NOT NULL,
    date_created              TIMESTAMP    NOT NULL,
    last_updated              TIMESTAMP    NOT NULL,
    name                      VARCHAR(255) NOT NULL,
    application_group_role_id UUID,
    created_by                VARCHAR(255) NOT NULL,
    description               VARCHAR(255),
    PRIMARY KEY (id)
);
CREATE INDEX catalogue_user_profile_picture_idx ON security.catalogue_user(profile_picture_id);
CREATE INDEX catalogueUser_created_by_idx ON security.catalogue_user(created_by);
ALTER TABLE IF EXISTS security.catalogue_user
    ADD CONSTRAINT UK_26qjnuqu76954q376opkqelqd UNIQUE (email_address);
CREATE INDEX groupRole_created_by_idx ON security.group_role(created_by);
ALTER TABLE IF EXISTS security.group_role
    ADD CONSTRAINT UK_7kvrlnisllgg2md5614ywh82g UNIQUE (name);
CREATE INDEX jcutug_catalogue_user_idx ON security.join_catalogue_user_to_user_group(catalogue_user_id);
CREATE INDEX jcutug_user_group_idx ON security.join_catalogue_user_to_user_group(user_group_id);
CREATE INDEX securableResourceGroupRole_created_by_idx ON security.securable_resource_group_role(created_by);
CREATE INDEX userGroup_created_by_idx ON security.user_group(created_by);
ALTER TABLE IF EXISTS security.user_group
    ADD CONSTRAINT UK_kas9w8ead0ska5n3csefp2bpp UNIQUE (name);
ALTER TABLE IF EXISTS security.catalogue_user
    ADD CONSTRAINT FKrvd4rw9ujjx4ca9b4dkps3jyt FOREIGN KEY (profile_picture_id) REFERENCES core.user_image_file;
ALTER TABLE IF EXISTS security.group_role
    ADD CONSTRAINT FK9y8ew5lpksnila4b7g56xcl1n FOREIGN KEY (parent_id) REFERENCES security.group_role;
ALTER TABLE IF EXISTS security.join_catalogue_user_to_user_group
    ADD CONSTRAINT FKauyvlits5bug2jc362csx3m18 FOREIGN KEY (user_group_id) REFERENCES security.user_group;
ALTER TABLE IF EXISTS security.join_catalogue_user_to_user_group
    ADD CONSTRAINT FKr4d5x0mewom4ibi8h9qy61ycc FOREIGN KEY (catalogue_user_id) REFERENCES security.catalogue_user;
ALTER TABLE IF EXISTS security.securable_resource_group_role
    ADD CONSTRAINT FKdjitehknypyvc8rjpeiw9ri97 FOREIGN KEY (user_group_id) REFERENCES security.user_group;
ALTER TABLE IF EXISTS security.securable_resource_group_role
    ADD CONSTRAINT FKgxkys8feqb0jvmshenxe7hvig FOREIGN KEY (group_role_id) REFERENCES security.group_role;
ALTER TABLE IF EXISTS security.user_group
    ADD CONSTRAINT FKnfw9gxi505amomyyy78665950 FOREIGN KEY (application_group_role_id) REFERENCES security.group_role;
