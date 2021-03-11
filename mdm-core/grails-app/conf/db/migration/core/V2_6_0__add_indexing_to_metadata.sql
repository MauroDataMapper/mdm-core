create
index metadata_namespace_index
	on core.metadata (namespace);

create
index metadata_namespace_key_index
	on core.metadata (namespace, key);
