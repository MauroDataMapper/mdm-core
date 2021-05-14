CREATE TABLE core.join_versionedfolder_to_facet (
    versionedfolder_id UUID NOT NULL,
    version_link_id    UUID
);
ALTER TABLE IF EXISTS core.join_versionedfolder_to_facet
    ADD CONSTRAINT FKcdu99gvtth7g6q2glm329u7uu FOREIGN KEY (version_link_id) REFERENCES core.version_link;
ALTER TABLE IF EXISTS core.join_versionedfolder_to_facet
    ADD CONSTRAINT FKsltt9c209xswibf8ocho4l8ly FOREIGN KEY (versionedfolder_id) REFERENCES core.folder;
