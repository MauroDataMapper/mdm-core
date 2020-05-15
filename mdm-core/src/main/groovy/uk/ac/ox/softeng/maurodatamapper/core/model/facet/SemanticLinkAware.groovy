package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.transform.CompileStatic
import groovy.transform.SelfType

/**
 * @since 30/01/2020
 */
@SelfType(CatalogueItem)
@CompileStatic
trait SemanticLinkAware {
    abstract Set<SemanticLink> getSemanticLinks()

    CatalogueItem addToSemanticLinks(SemanticLink add) {
        add.setCatalogueItem(this as CatalogueItem)
        addTo('semanticLinks', add)
    }

    CatalogueItem addToSemanticLinks(Map args) {
        addToSemanticLinks(new SemanticLink(args))
    }

    CatalogueItem removeFromSemanticLinks(SemanticLink semanticLinks) {
        removeFrom('semanticLinks', semanticLinks)
    }
}
