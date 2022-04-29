/*
code_set
term
term_relationship
term_relationship_type
terminology

 */

UPDATE terminology.code_set
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';

UPDATE terminology.terminology
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';

UPDATE terminology.term
SET path = REPLACE(path, REPLACE(code, '+', ' '), code)
WHERE code LIKE '%+%';
UPDATE terminology.term dc
SET path = REPLACE(dc.path, REPLACE(dm.label, '+', ' '), dm.label)
FROM terminology.terminology dm
WHERE dc.terminology_id = dm.id AND
      dm.label LIKE '%+%';

UPDATE terminology.term_relationship tr
SET path = REPLACE(tr.path, REPLACE(code, '+', ' '), code)
FROM terminology.term st
WHERE tr.target_term_id = st.id AND
      st.code LIKE '%+%';
UPDATE terminology.term_relationship tr
SET path = REPLACE(tr.path, REPLACE(code, '+', ' '), code)
FROM terminology.term st
WHERE tr.source_term_id = st.id AND
      st.code LIKE '%+%';

UPDATE terminology.term_relationship_type
SET path = REPLACE(path, REPLACE(label, '+', ' '), label)
WHERE label LIKE '%+%';
UPDATE terminology.term_relationship_type dc
SET path = REPLACE(dc.path, REPLACE(dm.label, '+', ' '), dm.label)
FROM terminology.terminology dm
WHERE dc.terminology_id = dm.id AND
      dm.label LIKE '%+%';

