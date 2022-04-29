WITH RECURSIVE child_folders AS (
    SELECT id,
           parent_folder_id,
           label,
           branch_name
    FROM core.folder
    WHERE class = 'uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder'
    UNION
    SELECT f.id,
           f.parent_folder_id,
           f.label,
           cf.branch_name
    FROM core.folder f
         INNER JOIN child_folders cf ON cf.id = f.parent_folder_id
)
UPDATE referencedata.reference_data_model dm
SET branch_name = f.branch_name
FROM child_folders f
WHERE dm.folder_id = f.id AND
      dm.branch_name <> f.branch_name;