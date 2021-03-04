ALTER TABLE IF EXISTS terminology.terminology ADD COLUMN model_version_tag VARCHAR(255);
ALTER TABLE IF EXISTS terminology.code_set ADD COLUMN model_version_tag VARCHAR(255);

