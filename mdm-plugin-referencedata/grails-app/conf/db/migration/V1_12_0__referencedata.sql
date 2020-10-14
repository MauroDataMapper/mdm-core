CREATE SCHEMA IF NOT EXISTS referencedatamodel;

create table referencedatamodel.join_referenceDataElement_to_facet (referenceDataElement_id uuid not null, classifier_id uuid, annotation_id uuid, semantic_link_id uuid, reference_file_id uuid, metadata_id uuid);
create table referencedatamodel.join_referenceDataModel_to_facet (referenceDataModel_id uuid not null, classifier_id uuid, annotation_id uuid, semantic_link_id uuid, version_link_id uuid, reference_file_id uuid, metadata_id uuid);
create table referencedatamodel.join_referenceDataType_to_facet (referenceDataType_id uuid not null, classifier_id uuid, annotation_id uuid, semantic_link_id uuid, reference_file_id uuid, metadata_id uuid);
create table referencedatamodel.join_referenceDataValue_to_facet (referenceDataValue_id uuid not null, classifier_id uuid, annotation_id uuid, semantic_link_id uuid, reference_file_id uuid, metadata_id uuid);
create table referencedatamodel.join_referenceEnumerationValue_to_facet (referenceEnumerationValue_id uuid not null, classifier_id uuid, annotation_id uuid, semantic_link_id uuid, reference_file_id uuid, metadata_id uuid);
create table referencedatamodel.reference_data_element (id uuid not null, version int8 not null, date_created timestamp not null, reference_data_type_id uuid not null, reference_data_model_id uuid not null, last_updated timestamp not null, path text not null, depth int4 not null, min_multiplicity int4, max_multiplicity int4, breadcrumb_tree_id uuid not null, idx int4 not null, created_by varchar(255) not null, aliases_string text, label text not null, description text, primary key (id));
create table referencedatamodel.reference_data_element_reference_summary_metadata (reference_data_element_reference_summary_metadata_id uuid not null, reference_summary_metadata_id uuid);
create table referencedatamodel.reference_data_model (id uuid not null, version int8 not null, branch_name varchar(255) not null, date_created timestamp not null, finalised boolean not null, readable_by_authenticated_users boolean not null, date_finalised timestamp, documentation_version varchar(255) not null, readable_by_everyone boolean not null, model_type varchar(255) not null, last_updated timestamp not null, organisation varchar(255), deleted boolean not null, author varchar(255), breadcrumb_tree_id uuid not null, model_version varchar(255), folder_id uuid not null, authority_id uuid not null, created_by varchar(255) not null, aliases_string text, label text not null, description text, primary key (id));
create table referencedatamodel.reference_data_model_reference_summary_metadata (reference_data_model_reference_summary_metadata_id uuid not null, reference_summary_metadata_id uuid);
create table referencedatamodel.reference_data_type (id uuid not null, version int8 not null, date_created timestamp not null, reference_data_model_id uuid not null, domain_type varchar(30) not null, last_updated timestamp not null, path text not null, depth int4 not null, breadcrumb_tree_id uuid not null, idx int4 not null, created_by varchar(255) not null, aliases_string text, label text not null, description text, class varchar(255) not null, units varchar(255), primary key (id));
create table referencedatamodel.reference_data_type_reference_summary_metadata (reference_data_type_summary_metadata_id uuid not null, reference_summary_metadata_id uuid);
create table referencedatamodel.reference_enumeration_value (id uuid not null, version int8 not null, date_created timestamp not null, value text not null, reference_enumeration_type_id uuid not null, last_updated timestamp not null, path text not null, depth int4 not null, breadcrumb_tree_id uuid not null, idx int4 not null, category text, created_by varchar(255) not null, aliases_string text, key text not null, label text not null, description text, primary key (id));
create table referencedatamodel.reference_summary_metadata (id uuid not null, version int8 not null, summary_metadata_type varchar(255) not null, date_created timestamp not null, last_updated timestamp not null, catalogue_item_domain_type varchar(255) not null, catalogue_item_id uuid, created_by varchar(255) not null, label text not null, description text, primary key (id));
create table referencedatamodel.reference_summary_metadata_report (id uuid not null, version int8 not null, date_created timestamp not null, last_updated timestamp not null, report_date timestamp not null, created_by varchar(255) not null, report_value text not null, summary_metadata_id uuid not null, primary key (id));
create table referencedatamodel.reference_data_value (id uuid not null, version int8 not null, date_created timestamp not null, value text, reference_data_model_id uuid not null, reference_data_element_id uuid not null, row_number int8 not null, last_updated timestamp not null, path text not null, depth int4 not null, breadcrumb_tree_id uuid not null, idx int4 not null, created_by varchar(255) not null, aliases_string text, label text, description text, primary key (id));
create index data_element_data_type_idx on referencedatamodel.reference_data_element (reference_data_type_id);
create index data_element_reference_data_model_idx on referencedatamodel.reference_data_element (reference_data_model_id);
create index referenceDataElement_created_by_idx on referencedatamodel.reference_data_element (created_by);
create index referenceDataModel_created_by_idx on referencedatamodel.reference_data_model (created_by);
create index data_type_reference_data_model_idx on referencedatamodel.reference_data_type (reference_data_model_id);
create index referenceDataType_created_by_idx on referencedatamodel.reference_data_type (created_by);
create index referenceEnumerationValue_created_by_idx on referencedatamodel.reference_enumeration_value (created_by);
create index referenceSummaryMetadata_created_by_idx on referencedatamodel.reference_summary_metadata (created_by);
create index referenceSummaryMetadataReport_created_by_idx on referencedatamodel.reference_summary_metadata_report (created_by);
create index summary_metadata_report_summary_metadata_idx on referencedatamodel.reference_summary_metadata_report (summary_metadata_id);
create index reference_data_value_reference_data_element_idx on referencedatamodel.reference_data_value (reference_data_element_id);
create index reference_data_value_reference_data_model_idx on referencedatamodel.reference_data_value (reference_data_model_id);
alter table if exists referencedatamodel.join_referenceDataElement_to_facet add constraint FK2fki0p2nnwaurehb5cjttuvix foreign key (classifier_id) references core.classifier;
alter table if exists referencedatamodel.join_referenceDataElement_to_facet add constraint FK2ls8wxo2ymrl7lpcys7j0xv3b foreign key (referenceDataElement_id) references referencedatamodel.reference_data_element;
alter table if exists referencedatamodel.join_referenceDataElement_to_facet add constraint FKd3a65vscren7g42xw4rahy6g5 foreign key (annotation_id) references core.annotation;
alter table if exists referencedatamodel.join_referenceDataElement_to_facet add constraint FKb7mrla3ru59iox823w8cgdiy0 foreign key (semantic_link_id) references core.semantic_link;
alter table if exists referencedatamodel.join_referenceDataElement_to_facet add constraint FKrltsh3bwdh88lysiui0euxus8 foreign key (reference_file_id) references core.reference_file;
alter table if exists referencedatamodel.join_referenceDataElement_to_facet add constraint FKqp0ri5bm3hvss6s1j3pyonkxr foreign key (metadata_id) references core.metadata;
alter table if exists referencedatamodel.join_referenceDataModel_to_facet add constraint FK3jbl1c288a9m1wp6hpira3esu foreign key (classifier_id) references core.classifier;
alter table if exists referencedatamodel.join_referenceDataModel_to_facet add constraint FK8gio5kn4wbjxsb3vpxno2guty foreign key (referenceDataModel_id) references referencedatamodel.reference_data_model;
alter table if exists referencedatamodel.join_referenceDataModel_to_facet add constraint FKjiqw3v6crj988n5addti0ar4u foreign key (annotation_id) references core.annotation;
alter table if exists referencedatamodel.join_referenceDataModel_to_facet add constraint FK8jwrx0ncwyb64s7d9ygmjr2f7 foreign key (semantic_link_id) references core.semantic_link;
alter table if exists referencedatamodel.join_referenceDataModel_to_facet add constraint FKpq9dfcuckjwcdeh9n54r062e0 foreign key (version_link_id) references core.version_link;
alter table if exists referencedatamodel.join_referenceDataModel_to_facet add constraint FKksgi9yaaa427xe5saynb6rd2i foreign key (reference_file_id) references core.reference_file;
alter table if exists referencedatamodel.join_referenceDataModel_to_facet add constraint FKtlkajagcv38bnatcquinb7p2v foreign key (metadata_id) references core.metadata;
alter table if exists referencedatamodel.join_referenceDataType_to_facet add constraint FK3vwe6oyjkdap164w7imcng9vx foreign key (classifier_id) references core.classifier;
alter table if exists referencedatamodel.join_referenceDataType_to_facet add constraint FKser4c5ad6dkspbnyjl2r1yuj3 foreign key (referenceDataType_id) references referencedatamodel.reference_data_type;
alter table if exists referencedatamodel.join_referenceDataType_to_facet add constraint FK7j8ag77c03icvomcohocy682d foreign key (annotation_id) references core.annotation;
alter table if exists referencedatamodel.join_referenceDataType_to_facet add constraint FKag55g7g8434y1497a6jmldxlr foreign key (semantic_link_id) references core.semantic_link;
alter table if exists referencedatamodel.join_referenceDataType_to_facet add constraint FKbw5w6fr1vaf9v0pcu7qs81nvu foreign key (reference_file_id) references core.reference_file;
alter table if exists referencedatamodel.join_referenceDataType_to_facet add constraint FKggbf0ml2ou4b2k525xrb1mxq6 foreign key (metadata_id) references core.metadata;
alter table if exists referencedatamodel.join_referenceEnumerationValue_to_facet add constraint FKp2io00cx587eojmbl5v27g7m3 foreign key (classifier_id) references core.classifier;
alter table if exists referencedatamodel.join_referenceEnumerationValue_to_facet add constraint FKclc83k4qxd0yxfspwkkttsjmj foreign key (referenceEnumerationValue_id) references referencedatamodel.reference_enumeration_value;
alter table if exists referencedatamodel.join_referenceEnumerationValue_to_facet add constraint FK2cfjn7dvabjkphwvne3jmhu24 foreign key (annotation_id) references core.annotation;
alter table if exists referencedatamodel.join_referenceEnumerationValue_to_facet add constraint FK87toxbm4bddbchculnipo9876 foreign key (semantic_link_id) references core.semantic_link;
alter table if exists referencedatamodel.join_referenceEnumerationValue_to_facet add constraint FKemx1xs8y5xnl1a6kdu18mp3us foreign key (reference_file_id) references core.reference_file;
alter table if exists referencedatamodel.join_referenceEnumerationValue_to_facet add constraint FKq50iqxdtfqwh3x6mdaepsx143 foreign key (metadata_id) references core.metadata;
alter table if exists referencedatamodel.reference_data_element add constraint FK72aidiwlq9doq630milqmpt0h foreign key (reference_data_type_id) references referencedatamodel.reference_data_type;
alter table if exists referencedatamodel.reference_data_element add constraint FK5s8ym98wxlmji2cwd5c2uqx51 foreign key (reference_data_model_id) references referencedatamodel.reference_data_model;
alter table if exists referencedatamodel.reference_data_element add constraint FKfmyjc00b03urjiavamg30vryh foreign key (breadcrumb_tree_id) references core.breadcrumb_tree;
alter table if exists referencedatamodel.reference_data_element_reference_summary_metadata add constraint FKcamqn6r8ruu47dfcwd8t5khre foreign key (reference_summary_metadata_id) references referencedatamodel.reference_summary_metadata;
alter table if exists referencedatamodel.reference_data_element_reference_summary_metadata add constraint FKh9lqs0iqoqr674lvpfl01m4f foreign key (reference_data_element_reference_summary_metadata_id) references referencedatamodel.reference_data_element;
alter table if exists referencedatamodel.reference_data_model add constraint FKk0dbj4ejwa3rpnm87ten7l650 foreign key (breadcrumb_tree_id) references core.breadcrumb_tree;
alter table if exists referencedatamodel.reference_data_model add constraint FK8dvr6bt8lf5xtces9vstu3h9i foreign key (folder_id) references core.folder;
alter table if exists referencedatamodel.reference_data_model add constraint FK7jnsebhp01jrvj1cnoiglnk36 foreign key (authority_id) references core.authority;
alter table if exists referencedatamodel.reference_data_model_reference_summary_metadata add constraint FKe9f307usil36627eebfyncmas foreign key (reference_summary_metadata_id) references referencedatamodel.reference_summary_metadata;
alter table if exists referencedatamodel.reference_data_model_reference_summary_metadata add constraint FK7018d6f5ebbikvy7ka0mmlwug foreign key (reference_data_model_reference_summary_metadata_id) references referencedatamodel.reference_data_model;
alter table if exists referencedatamodel.reference_data_type add constraint FKn6ied2qohp1b9guvwcsskng2b foreign key (reference_data_model_id) references referencedatamodel.reference_data_model;
alter table if exists referencedatamodel.reference_data_type add constraint FK21bionqtblyjus0xdx0fpxsd0 foreign key (breadcrumb_tree_id) references core.breadcrumb_tree;
alter table if exists referencedatamodel.reference_data_type_reference_summary_metadata add constraint FKrydmedwui8knor4eiktsfrw1r foreign key (reference_summary_metadata_id) references referencedatamodel.reference_summary_metadata;
alter table if exists referencedatamodel.reference_data_type_reference_summary_metadata add constraint FKkps5yegy08ndnnd8ic3bgk56q foreign key (reference_data_type_summary_metadata_id) references referencedatamodel.reference_data_type;
alter table if exists referencedatamodel.reference_enumeration_value add constraint FKfcsl5wvgo4hhgd32kio4vsxke foreign key (reference_enumeration_type_id) references referencedatamodel.reference_data_type;
alter table if exists referencedatamodel.reference_enumeration_value add constraint FKdh4kk2d1frpb2rfep76o7d6v8 foreign key (breadcrumb_tree_id) references core.breadcrumb_tree;
alter table if exists referencedatamodel.reference_summary_metadata_report add constraint FKtm1k29089tgksd63i7yjaha8g foreign key (summary_metadata_id) references referencedatamodel.reference_summary_metadata;
alter table if exists referencedatamodel.reference_data_value add constraint FK3ru68cbfsr7cx03c1szowx23u FOREIGN KEY (reference_data_model_id) references referencedatamodel.reference_data_model;
alter table if exists referencedatamodel.reference_data_value add constraint FKbq7kc2ry1zi21fch17trnjtm8 FOREIGN KEY (breadcrumb_tree_id) references core.breadcrumb_tree;
alter table if exists referencedatamodel.reference_data_value add constraint FKuknlrsbwja5t5vd84ceulvn9p FOREIGN KEY (reference_data_element_id) references referencedatamodel.reference_data_element;
alter table if exists referencedatamodel.join_referencedatavalue_to_facet add constraint FK9kjgg5wj8npozbd0fuj982g6p FOREIGN KEY (classifier_id) references core.classifier;
alter table if exists referencedatamodel.join_referencedatavalue_to_facet add constraint FK477k0y3842t22demljgfjjryt FOREIGN KEY (referencedatavalue_id) references referencedatamodel.reference_data_value;
alter table if exists referencedatamodel.join_referencedatavalue_to_facet add constraint FKar18o3cu7vcu1ivvmd8087tgx FOREIGN KEY (semantic_link_id) references core.semantic_link;
alter table if exists referencedatamodel.join_referencedatavalue_to_facet add constraint FKbbb79ed9t0u7v1gw3l4eksed7 FOREIGN KEY (annotation_id) references core.annotation;
alter table if exists referencedatamodel.join_referencedatavalue_to_facet add constraint FKenneayqmmakd3jjtz0rc9ww2z FOREIGN KEY (reference_file_id) references core.reference_file;
alter table if exists referencedatamodel.join_referencedatavalue_to_facet add constraint FKs3h18ie0xp4vrwnmvgwajqkt8 FOREIGN KEY (metadata_id) references core.metadata;  
