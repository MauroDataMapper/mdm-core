package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.search.Lucene

class UpdatedBeforeFilter extends DateTimeSearchParamFilter {

    boolean doesApply(SearchParams searchParams) {
        searchParams.lastUpdatedBefore
    }

    Closure getClosure(SearchParams searchParams) {
        Lucene.defineAdditionalLuceneQuery {
            below 'lastUpdated', getOffsetDateTimeFromDate(searchParams.lastUpdatedBefore.toInstant())
        }
    }
}