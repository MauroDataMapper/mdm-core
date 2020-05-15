package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.transform.CompileStatic
import groovy.transform.SelfType

/**
 * @since 30/01/2020
 */
@SelfType(Model)
@CompileStatic
trait VersionLinkAware {
    abstract Set<VersionLink> getVersionLinks()

    Model addToVersionLinks(VersionLink add) {
        add.setModel(this as Model)
        addTo('versionLinks', add)
    }

    Model addToVersionLinks(Map args) {
        addToVersionLinks(new VersionLink(args))
    }

    Model removeFromVersionLinks(VersionLink versionLinks) {
        removeFrom('versionLinks', versionLinks)
    }
}
