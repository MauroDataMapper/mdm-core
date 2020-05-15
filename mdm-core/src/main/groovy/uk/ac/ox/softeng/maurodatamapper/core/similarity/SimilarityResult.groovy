package uk.ac.ox.softeng.maurodatamapper.core.similarity

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType

abstract class SimilarityResult<K extends CatalogueItem> {

    K source

    // Use an ArrayList to keep the results in the correct order
    ArrayList<SimilarityPair<K>> results

    SimilarityResult(K source) {
        this.source = source
        results = new ArrayList<>()
    }

    void add(K target, Float f) {
        results.add(new SimilarityPair(target, f))
    }

    int size() {
        results.size()
    }

    def each(@DelegatesTo(List) @ClosureParams(value = SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.core.similarity.SimilarityPair')
                 Closure closure) {
        results.each(closure)
    }

    SimilarityPair<K> first() {
        results.first()
    }
}
