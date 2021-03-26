ALTER TABLE core.edit ADD COLUMN title VARCHAR(255);

--migrate existing rows
UPDATE core.edit SET title = 'CREATE' WHERE description LIKE '[%] added to component [%]' AND title IS NULL;
UPDATE core.edit SET title = 'DELETE' WHERE description LIKE '[%] removed from component [%]' AND title IS NULL;
UPDATE core.edit SET title = 'FINALISE' WHERE description LIKE '% finalised by %' AND title IS NULL;
UPDATE core.edit SET title = 'COPY' WHERE description LIKE '% created as a copy of %' AND title IS NULL;
UPDATE core.edit SET title = 'CHANGENOTICE' WHERE description LIKE 'CHANGENOTICE%' AND title IS NULL;
UPDATE core.edit SET title = 'UPDATE' WHERE title IS NULL;

--after migration, set title not null
ALTER TABLE core.edit ALTER COLUMN title SET NOT NULL;