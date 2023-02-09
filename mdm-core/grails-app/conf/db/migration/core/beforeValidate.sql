UPDATE core.flyway_schema_history
SET checksum = 490568862
WHERE version = '1.7.0' AND
      checksum = -1413608684;

DELETE
FROM core.flyway_schema_history
WHERE version = '2.10.0' AND
      description = 'update database metadata values';

UPDATE core.flyway_schema_history
SET checksum = -1978164369
WHERE version = '5.1.3' AND
      checksum = -573557977;