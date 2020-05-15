package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.search.Lucene

class ClassifiersFilter implements SearchParamFilter {

    boolean doesApply(SearchParams searchParams) {
        searchParams.classifiers
    }

    Closure getClosure(SearchParams searchParams) {
        Lucene.defineAdditionalLuceneQuery {
            must {
                searchParams.classifiers.each {cl ->
                    keyword 'classifiers.label', cl
                }
            }
        }
    }
}