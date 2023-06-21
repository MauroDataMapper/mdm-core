delete from terminology.join_term_to_facet where (select count(*) from terminology.term where term.id = join_term_to_facet.term_id) = 0;

delete from core.metadata  where multi_facet_aware_item_domain_type = 'Term' and (select count(*) from terminology.term where term.id = metadata.multi_facet_aware_item_id) = 0;
delete from core.rule  where multi_facet_aware_item_domain_type = 'Term' and (select count(*) from terminology.term where term.id = rule.multi_facet_aware_item_id) = 0;
delete from core.annotation  where multi_facet_aware_item_domain_type = 'Term' and (select count(*) from terminology.term where term.id = annotation.multi_facet_aware_item_id) = 0;
delete from core.reference_file  where multi_facet_aware_item_domain_type = 'Term' and (select count(*) from terminology.term where term.id = reference_file.multi_facet_aware_item_id) = 0;
delete from core.semantic_link  where multi_facet_aware_item_domain_type = 'Term' and (select count(*) from terminology.term where term.id = semantic_link.multi_facet_aware_item_id) = 0;

