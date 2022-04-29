/*
data_class
data_element
data_model
data_type
enumeration_value
summary_metadata
summary_metadata_report

 */

UPDATE datamodel.data_model
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';

UPDATE datamodel.data_class
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';
UPDATE datamodel.data_class dc
SET path = REPLACE(dc.path, REPLACE(dm.label, '+', ' '), dm.label)
FROM datamodel.data_model dm
WHERE dc.data_model_id = dm.id AND
      dm.label LIKE '%+%';

UPDATE datamodel.data_element
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';

UPDATE datamodel.data_element de
SET path = REPLACE(de.path, REPLACE(dc.label, '+', ' '), dc.label)
FROM datamodel.data_class dc
WHERE de.data_class_id = dc.id AND
      dc.label LIKE '%+%';

UPDATE datamodel.data_element de
SET path = REPLACE(de.path, REPLACE(dm.label, '+', ' '), dm.label)
FROM datamodel.data_class dc,
     datamodel.data_model dm
WHERE de.data_class_id = dc.id AND
      dm.id = dc.data_model_id AND
      dm.label LIKE '%+%';

UPDATE datamodel.data_type
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';
UPDATE datamodel.data_type dc
SET path = REPLACE(dc.path, REPLACE(dm.label, '+', ' '), dm.label)
FROM datamodel.data_model dm
WHERE dc.data_model_id = dm.id AND
      dm.label LIKE '%+%';



UPDATE datamodel.enumeration_value
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';

UPDATE datamodel.enumeration_value de
SET path = REPLACE(de.path, REPLACE(dc.label, '+', ' '), dc.label)
FROM datamodel.data_type dc
WHERE de.enumeration_type_id = dc.id AND
      dc.label LIKE '%+%';

UPDATE datamodel.enumeration_value de
SET path = REPLACE(de.path, REPLACE(dm.label, '+', ' '), dm.label)
FROM datamodel.data_type dc,
     datamodel.data_model dm
WHERE de.enumeration_type_id = dc.id AND
      dm.id = dc.data_model_id AND
      dm.label LIKE '%+%';



UPDATE datamodel.summary_metadata
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';
UPDATE datamodel.summary_metadata_report rr
SET path = REPLACE(rr.path, REPLACE(r.label, '+', ' '), r.label)
FROM datamodel.summary_metadata r
WHERE rr.summary_metadata_id = r.id AND
      r.label LIKE '%+%';