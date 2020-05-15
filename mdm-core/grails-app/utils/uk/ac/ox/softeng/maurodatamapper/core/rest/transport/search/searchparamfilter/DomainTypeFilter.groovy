package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.search.Lucene

class DomainTypeFilter implements SearchParamFilter {

    boolean doesApply(SearchParams searchParams) {
        searchParams.domainType
    }

    Closure getClosure(SearchParams searchParams) {
        Lucene.defineAdditionalLuceneQuery {
            keyword 'domainType', searchParams.domainType
        }
    }
}