/*
annotation
api_property
authority
classifier
edit
folder
metadata
reference_file
rule
rule_representation
semantic_link
user_image_file
version_link

 */

UPDATE core.annotation
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';

CREATE OR REPLACE FUNCTION update_children(parentId UUID)
    RETURNS INT
    LANGUAGE plpgsql
AS
$$
DECLARE
    cid UUID;
BEGIN
    FOR cid IN
        SELECT id
        FROM core.annotation
        WHERE parent_annotation_id = parentId
        LOOP
            UPDATE core.annotation c
            SET path = REPLACE(path, REPLACE(p.label, '+', ' '), p.label)
            FROM core.annotation p
            WHERE c.id = cid AND
                  c.parent_annotation_id = p.id AND
                  p.path LIKE '%+%';
            PERFORM update_children(cid);

        END LOOP;
    RETURN 1;
END
$$;

DO
$do$
    DECLARE
        cid UUID;
    BEGIN
        FOR cid IN
            SELECT id
            FROM core.annotation
            WHERE parent_annotation_id IS NULL
            LOOP
                PERFORM update_children(cid);
            END LOOP;
    END;
$do$ LANGUAGE plpgsql;
DROP FUNCTION update_children;


UPDATE core.api_property
SET path = REPLACE(path, REPLACE(category, '+', ' '), category)
WHERE category LIKE '%+%';
UPDATE core.api_property
SET path = REPLACE(path, REPLACE(key, '+', ' '), key)
WHERE key LIKE '%+%';

UPDATE core.authority
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';
UPDATE core.authority
SET path = REPLACE(path, REPLACE(url, '+', ' '), url)
WHERE url LIKE '%+%';

UPDATE core.classifier
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';
CREATE OR REPLACE FUNCTION update_children(parentId UUID)
    RETURNS INT
    LANGUAGE plpgsql
AS
$$
DECLARE
    cid UUID;
BEGIN
    FOR cid IN
        SELECT id
        FROM core.classifier
        WHERE parent_classifier_id = parentId
        LOOP
            UPDATE core.classifier c
            SET path = REPLACE(path, REPLACE(p.label, '+', ' '), p.label)
            FROM core.classifier p
            WHERE c.id = cid AND
                  c.parent_classifier_id = p.id AND
                  p.path LIKE '%+%';
            PERFORM update_children(cid);

        END LOOP;
    RETURN 1;
END
$$;

DO
$do$
    DECLARE
        cid UUID;
    BEGIN
        FOR cid IN
            SELECT id
            FROM core.classifier
            WHERE parent_classifier_id IS NULL
            LOOP
                PERFORM update_children(cid);
            END LOOP;
    END;
$do$ LANGUAGE plpgsql;
DROP FUNCTION update_children;

UPDATE core.edit
SET path = REPLACE(path, REPLACE(title, '+', ' '), title)
WHERE title LIKE '%+%';

UPDATE core.folder
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';
CREATE OR REPLACE FUNCTION update_children(parentId UUID)
    RETURNS INT
    LANGUAGE plpgsql
AS
$$
DECLARE
    cid UUID;
BEGIN
    FOR cid IN
        SELECT id
        FROM core.folder
        WHERE parent_folder_id = parentId
        LOOP
            UPDATE core.folder c
            SET path = REPLACE(path, REPLACE(p.label, '+', ' '), p.label)
            FROM core.folder p
            WHERE c.id = cid AND
                  c.parent_folder_id = p.id AND
                  p.path LIKE '%+%';
            PERFORM update_children(cid);

        END LOOP;
    RETURN 1;
END
$$;

DO
$do$
    DECLARE
        cid UUID;
    BEGIN
        FOR cid IN
            SELECT id
            FROM core.folder
            WHERE parent_folder_id IS NULL
            LOOP
                PERFORM update_children(cid);
            END LOOP;
    END;
$do$ LANGUAGE plpgsql;
DROP FUNCTION update_children;

UPDATE core.metadata
SET path = REPLACE(path, REPLACE(namespace, '+', ' '), namespace)
WHERE namespace LIKE '%+%';
UPDATE core.metadata
SET path = REPLACE(path, REPLACE(key, '+', ' '), key)
WHERE key LIKE '%+%';

UPDATE core.reference_file
SET path = REPLACE(path, REPLACE(file_name, '+', ' '), file_name)
WHERE file_name LIKE '%+%';


UPDATE core.rule
SET path = REPLACE(path, REPLACE(name, '+', ' '), name)
WHERE name LIKE '%+%';

UPDATE core.rule_representation
SET path = REPLACE(path, REPLACE(language, '+', ' '), language)
WHERE language LIKE '%+%';
UPDATE core.rule_representation rr
SET path = REPLACE(rr.path, REPLACE(r.name, '+', ' '), r.name)
FROM core.rule r
WHERE rr.rule_id = r.id AND
      r.name LIKE '%+%';

UPDATE core.user_image_file
SET path = REPLACE(path, REPLACE(file_name, '+', ' '), file_name)
WHERE file_name LIKE '%+%';