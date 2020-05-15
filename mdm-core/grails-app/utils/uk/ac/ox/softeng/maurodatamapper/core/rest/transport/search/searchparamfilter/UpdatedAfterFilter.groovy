package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.search.Lucene

class UpdatedAfterFilter extends DateTimeSearchParamFilter {

    boolean doesApply(SearchParams searchParams) {
        searchParams.lastUpdatedAfter
    }

    Closure getClosure(SearchParams searchParams) {
        Lucene.defineAdditionalLuceneQuery {
            above 'lastUpdated', getOffsetDateTimeFromDate(searchParams.lastUpdatedAfter)
        }
    }


}