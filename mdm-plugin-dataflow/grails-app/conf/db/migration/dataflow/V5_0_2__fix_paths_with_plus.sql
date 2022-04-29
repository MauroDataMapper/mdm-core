/*
data_class_component
data_element_component
data_flow
 */

UPDATE dataflow.data_flow
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';

UPDATE dataflow.data_class_component
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';
UPDATE dataflow.data_class_component rr
SET path = REPLACE(rr.path, REPLACE(r.label, '+', ' '), r.label)
FROM dataflow.data_flow r
WHERE rr.data_flow_id = r.id AND
      r.label LIKE '%+%';

UPDATE dataflow.data_element_component
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';

UPDATE dataflow.data_element_component dec
SET path = REPLACE(dec.path, REPLACE(dcc.label, '+', ' '), dcc.label)
FROM dataflow.data_class_component dcc
WHERE dec.data_class_component_id = dcc.id AND
      dcc.label LIKE '%+%';

UPDATE dataflow.data_element_component dec
SET path = REPLACE(dec.path, REPLACE(df.label, '+', ' '), df.label)
FROM dataflow.data_class_component dcc,
     dataflow.data_flow df
WHERE dec.data_class_component_id = dcc.id AND
      df.id = dcc.data_flow_id AND
      df.label LIKE '%+%';