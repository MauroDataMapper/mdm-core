/*
reference_data_element
reference_data_model
reference_data_type
reference_data_value
reference_enumeration_value
reference_summary_metadata
reference_summary_metadata_report
 */

UPDATE referencedata.reference_data_model
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';

UPDATE referencedata.reference_data_element
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';
UPDATE referencedata.reference_data_element de
SET path = REPLACE(de.path, REPLACE(dc.label, '+', ' '), dc.label)
FROM referencedata.reference_data_model dc
WHERE de.reference_data_model_id = dc.id AND
      dc.label LIKE '%+%';

UPDATE referencedata.reference_data_value de
SET path = REPLACE(de.path, REPLACE(dc.label, '+', ' '), dc.label)
FROM referencedata.reference_data_element dc
WHERE de.reference_data_element_id = dc.id AND
      dc.label LIKE '%+%';
UPDATE referencedata.reference_data_value de
SET path = REPLACE(de.path, REPLACE(dm.label, '+', ' '), dm.label)
FROM referencedata.reference_data_element dc,
     referencedata.reference_data_model dm
WHERE de.reference_data_element_id = dc.id AND
      dm.id = dc.reference_data_model_id AND
      dm.label LIKE '%+%';


UPDATE referencedata.reference_data_type
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';
UPDATE referencedata.reference_data_type dc
SET path = REPLACE(dc.path, REPLACE(dm.label, '+', ' '), dm.label)
FROM referencedata.reference_data_model dm
WHERE dc.reference_data_model_id = dm.id AND
      dm.label LIKE '%+%';



UPDATE referencedata.reference_enumeration_value
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';

UPDATE referencedata.reference_enumeration_value de
SET path = REPLACE(de.path, REPLACE(dc.label, '+', ' '), dc.label)
FROM referencedata.reference_data_type dc
WHERE de.reference_enumeration_type_id = dc.id AND
      dc.label LIKE '%+%';

UPDATE referencedata.reference_enumeration_value de
SET path = REPLACE(de.path, REPLACE(dm.label, '+', ' '), dm.label)
FROM referencedata.reference_data_type dc,
     referencedata.reference_data_model dm
WHERE de.reference_enumeration_type_id = dc.id AND
      dm.id = dc.reference_data_model_id AND
      dm.label LIKE '%+%';



UPDATE referencedata.reference_summary_metadata
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';
UPDATE referencedata.reference_summary_metadata_report rr
SET path = REPLACE(rr.path, REPLACE(r.label, '+', ' '), r.label)
FROM referencedata.reference_summary_metadata r
WHERE rr.summary_metadata_id = r.id AND
      r.label LIKE '%+%';