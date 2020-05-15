package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.search.Lucene

class CreatedBeforeFilter extends DateTimeSearchParamFilter {

    boolean doesApply(SearchParams searchParams) {
        searchParams.createdBefore
    }

    Closure getClosure(SearchParams searchParams) {
        Lucene.defineAdditionalLuceneQuery {
            below 'dateCreated', getOffsetDateTimeFromDate(searchParams.createdBefore)
        }
    }

}