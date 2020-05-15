package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.search.Lucene

class DomainTypesFilter implements SearchParamFilter {

    boolean doesApply(SearchParams searchParams) {
        searchParams.domainTypes
    }

    Closure getClosure(SearchParams searchParams) {
        Lucene.defineAdditionalLuceneQuery {
            should {
                searchParams.domainTypes.each {dt ->
                    keyword 'domainType', dt
                }
            }
        }
    }
}