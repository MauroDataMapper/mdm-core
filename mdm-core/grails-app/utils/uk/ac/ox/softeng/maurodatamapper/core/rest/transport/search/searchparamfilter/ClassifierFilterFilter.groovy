package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.search.Lucene

class ClassifierFilterFilter implements SearchParamFilter {

    boolean doesApply(SearchParams searchParams) {
        searchParams.classifierFilter
    }

    Closure getClosure(SearchParams searchParams) {
        Lucene.defineAdditionalLuceneQuery {
            must {
                searchParams.classifierFilter.each {clList ->
                    should {
                        clList.each {cl ->
                            keyword 'classifiers.label', cl.toString()
                        }
                    }
                }
            }
        }
    }
}