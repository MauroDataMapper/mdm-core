UPDATE maurodatamapper.datamodel.data_model
SET model_type = CASE
                     WHEN model_type IN ('DATA_ASSET', 'Data Asset')
                         THEN 'Data Asset'
                     WHEN model_type IN ('DATA_STANDARD', 'Data Standard')
                         THEN 'Data Standard'
                 END