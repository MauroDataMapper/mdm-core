ALTER TABLE core.user_image_file
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE core.user_image_file
    ADD COLUMN class character varying(255) COLLATE pg_catalog."default" NOT NULL DEFAULT 'uk.ac.ox.softeng.maurodatamapper.core.file.ImageFile'::character varying;

ALTER TABLE core.user_image_file
    RENAME TO image_file;
