package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.search.Lucene

class CreatedAfterFilter extends DateTimeSearchParamFilter {

    boolean doesApply(SearchParams searchParams) {
        searchParams.createdAfter
    }

    Closure getClosure(SearchParams searchParams) {
        Lucene.defineAdditionalLuceneQuery {
            above 'dateCreated', getOffsetDateTimeFromDate(searchParams.createdAfter)
        }
    }
}