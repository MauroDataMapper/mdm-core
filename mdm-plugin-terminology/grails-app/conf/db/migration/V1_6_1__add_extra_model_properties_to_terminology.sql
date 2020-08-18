ALTER TABLE terminology.terminology
    ADD COLUMN branch_name VARCHAR(255);

ALTER TABLE terminology.terminology
    ADD model_version VARCHAR(255);

ALTER TABLE terminology.code_set
    ADD COLUMN branch_name VARCHAR(255);

ALTER TABLE terminology.code_set
    ADD model_version VARCHAR(255);


