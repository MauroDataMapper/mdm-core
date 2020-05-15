package uk.ac.ox.softeng.maurodatamapper.core.similarity

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

/**
 * @since 07/04/2020
 */
class SimilarityPair<K extends CatalogueItem> {

    K item
    Float similarity

    SimilarityPair(K item, similarity) {
        this.item = item
        this.similarity = similarity
    }
}
