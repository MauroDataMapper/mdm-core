ALTER TABLE terminology.terminology
    ADD COLUMN authority_id UUID;

ALTER TABLE terminology.terminology
    ADD CONSTRAINT fk7dlm65qgt6m8ptacxycqyhl4m
        FOREIGN KEY (authority_id) REFERENCES core.authority;



ALTER TABLE terminology.code_set
    ADD COLUMN authority_id UUID;

ALTER TABLE terminology.code_set
    ADD CONSTRAINT fk2jwton4ry4smlk76tax1n1j5p
        FOREIGN KEY (authority_id) REFERENCES core.authority;

