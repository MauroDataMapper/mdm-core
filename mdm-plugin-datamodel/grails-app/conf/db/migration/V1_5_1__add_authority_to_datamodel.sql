ALTER TABLE datamodel.data_model
    ADD COLUMN authority_id UUID;

ALTER TABLE datamodel.data_model
    ADD CONSTRAINT fkkq5e5fj5kdb737ktmhyyljy4e
        FOREIGN KEY (authority_id) REFERENCES core.authority;

