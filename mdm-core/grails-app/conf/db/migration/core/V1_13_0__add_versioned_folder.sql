ALTER TABLE core.folder
    ADD COLUMN class VARCHAR(255) NOT NULL DEFAULT 'uk.ac.ox.softeng.maurodatamapper.core.container.Folder';
ALTER TABLE core.folder
    ADD COLUMN branch_name VARCHAR(255);
ALTER TABLE core.folder
    ADD COLUMN finalised BOOLEAN;
ALTER TABLE core.folder
    ADD COLUMN date_finalised TIMESTAMP;
ALTER TABLE core.folder
    ADD COLUMN documentation_version VARCHAR(255);
ALTER TABLE core.folder
    ADD COLUMN model_version VARCHAR(255);
ALTER TABLE core.folder
    ADD COLUMN authority_id UUID;