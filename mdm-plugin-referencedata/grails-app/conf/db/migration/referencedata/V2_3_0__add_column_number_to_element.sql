ALTER TABLE referencedata.reference_data_element
    ADD COLUMN column_number INT8;

UPDATE referencedata.reference_data_element
SET column_number = 0;

ALTER TABLE referencedata.reference_data_element
    ALTER COLUMN column_number SET NOT NULL;

