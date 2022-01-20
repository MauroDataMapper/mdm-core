ALTER TABLE terminology.terminology
    ADD path TEXT;
UPDATE terminology.terminology
SET path = CONCAT('te:', label, '$', COALESCE(model_version, branch_name));
ALTER TABLE terminology.terminology
    ALTER COLUMN path SET NOT NULL;

ALTER TABLE terminology.code_set
    ADD path TEXT;
UPDATE terminology.code_set
SET path = CONCAT('cs:', label, '$', COALESCE(model_version, branch_name));
ALTER TABLE terminology.code_set
    ALTER COLUMN path SET NOT NULL;

