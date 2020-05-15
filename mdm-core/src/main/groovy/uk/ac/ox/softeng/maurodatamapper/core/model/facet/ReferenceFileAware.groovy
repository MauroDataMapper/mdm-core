package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem

import groovy.transform.CompileStatic
import groovy.transform.SelfType

/**
 * @since 30/01/2020
 */
@SelfType(CatalogueItem)
@CompileStatic
trait ReferenceFileAware {
    abstract Set<ReferenceFile> getReferenceFiles()

    CatalogueItem addToReferenceFiles(ReferenceFile add) {
        add.setCatalogueItem(this as CatalogueItem)
        addTo('referenceFiles', add)
    }

    CatalogueItem addToReferenceFiles(Map args) {
        addToReferenceFiles(new ReferenceFile(args))
    }

    CatalogueItem removeFromReferenceFiles(ReferenceFile referenceFiles) {
        removeFrom('referenceFiles', referenceFiles)
    }
}
