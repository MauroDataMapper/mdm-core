package uk.ac.ox.softeng.maurodatamapper.core.model.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileStatic
import groovy.transform.SelfType

/**
 * @since 30/01/2020
 */
@SelfType(CatalogueItem)
@CompileStatic
trait MetadataAware {

    abstract Set<Metadata> getMetadata()

    Set<Metadata> findMetadataByNamespace(String namespace) {
        metadata?.findAll {it.namespace == namespace} ?: [] as HashSet
    }

    Metadata findMetadataByNamespaceAndKey(String namespace, String key) {
        metadata?.find {it.namespace == namespace && it.key == key}
    }

    CatalogueItem addToMetadata(Metadata add) {
        Metadata existing = findMetadataByNamespaceAndKey(add.namespace, add.key)
        if (existing) {
            existing.value = add.value
            markDirty('metadata', existing)
            this as CatalogueItem
        } else {
            add.setCatalogueItem(this as CatalogueItem)
            addTo('metadata', add)
        }
    }

    CatalogueItem addToMetadata(Map args) {
        addToMetadata(new Metadata(args))
    }

    CatalogueItem addToMetadata(String namespace, String key, String value, User createdBy) {
        addToMetadata(namespace, key, value, createdBy.emailAddress)
    }

    CatalogueItem addToMetadata(String namespace, String key, String value, String createdBy) {
        addToMetadata(new Metadata(namespace: namespace, key: key, value: value, createdBy: createdBy))
    }

    CatalogueItem addToMetadata(String namespace, String key, String value) {
        addToMetadata(namespace: namespace, key: key, value: value)
    }

    CatalogueItem removeFromMetadata(Metadata metadata) {
        removeFrom('metadata', metadata)
    }
}