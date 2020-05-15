package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams

interface SearchParamFilter {

    abstract boolean doesApply(SearchParams searchParams)

    abstract Closure getClosure(SearchParams searchParams)


}