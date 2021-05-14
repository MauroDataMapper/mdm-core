CREATE TABLE datamodel.join_dataclass_to_extended_data_class (
    dataclass_id          UUID NOT NULL,
    extended_dataclass_id UUID NOT NULL
);

ALTER TABLE datamodel.join_dataclass_to_extended_data_class
    ADD CONSTRAINT FKaph92y3qdyublukjj8mbsivo3 FOREIGN KEY (extended_dataclass_id) REFERENCES datamodel.data_class;
ALTER TABLE datamodel.join_dataclass_to_extended_data_class
    ADD CONSTRAINT FK5cn7jgi02lejlubi97a3x17ar FOREIGN KEY (dataclass_id) REFERENCES datamodel.data_class;

