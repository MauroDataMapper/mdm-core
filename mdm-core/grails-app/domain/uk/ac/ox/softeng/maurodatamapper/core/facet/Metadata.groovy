package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.CatalogueItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Metadata implements CatalogueItemAware, CreatorAware, Diffable<Metadata> {

    public final static Integer BATCH_SIZE = 5000

    UUID id

    String namespace
    String key
    String value

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        catalogueItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.catalogueItem && !obj.catalogueItem.ident()) return true
            ['default.null.message']
        }
        namespace blank: false
        key blank: false
        value blank: false
    }

    static mapping = {
        namespace type: 'text'
        key type: 'text'
        value type: 'text'
        catalogueItemId index: 'metadata_catalogue_item_idx'
        createdBy cascade: 'none', index: 'metadata_created_by_idx'
    }

    static search = {
        key index: 'yes', analyzer: 'wordDelimiter'
        value index: 'yes'
    }

    static transients = ['catalogueItem']

    Metadata() {
    }

    @Override
    String getDomainType() {
        Metadata.simpleName
    }


    @Override
    String toString() {
        "${getClass().getName()} : ${namespace}/${key} : ${id ?: '(unsaved)'}"
    }

    def beforeValidate() {
        value = value ?: 'N/A'
    }

    @Override
    String getEditLabel() {
        "Metadata:${namespace}:${key}"
    }

    @Override
    ObjectDiff<Metadata> diff(Metadata obj) {
        ObjectDiff.builder(Metadata)
            .leftHandSide(id.toString(), this)
            .rightHandSide(obj.id.toString(), obj)
            .appendString('namespace', this.namespace, obj.namespace)
            .appendString('key', this.key, obj.key)
            .appendString('value', this.value, obj.value)
    }

    @Override
    String getDiffIdentifier() {
        "${this.namespace}.${this.key}"
    }

    static Set<String> findAllDistinctKeysByNamespace(String namespace) {
        new DetachedCriteria<Metadata>(Metadata).eq('namespace', namespace)
            .distinct('key')
            .list() as Set<String>
    }

    static Set<String> findAllDistinctNamespaces() {
        new DetachedCriteria<Metadata>(Metadata)
            .distinct('namespace')
            .list() as Set<String>
    }

    static Set<String> findAllDistinctNamespacesIlike(String namespacePrefix) {
        new DetachedCriteria<Metadata>(Metadata)
            .distinct('namespace')
            .ilike('namespace', "${namespacePrefix}%")
            .list() as Set<String>
    }

    static DetachedCriteria<Metadata> byCatalogueItemId(Serializable catalogueItemId) {
        new DetachedCriteria<Metadata>(Metadata).eq('catalogueItemId', Utils.toUuid(catalogueItemId))
    }

    static DetachedCriteria<Metadata> byCatalogueItemIdAndId(Serializable catalogueItemId, Serializable resourceId) {
        byCatalogueItemId(catalogueItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<Metadata> byNamespace(String namespace) {
        new DetachedCriteria<Metadata>(Metadata).eq('namespace', namespace)
    }

    static DetachedCriteria<Metadata> byNamespaceAndKey(String namespace, String key) {
        new DetachedCriteria<Metadata>(Metadata).eq('namespace', namespace).eq('key', key)
    }

    static DetachedCriteria<Metadata> byNamespaceAndKeyAndValue(String namespace, String key, String value) {
        byNamespaceAndKey(namespace, key).eq('value', value)
    }

    static DetachedCriteria<Metadata> withFilter(DetachedCriteria<Metadata> criteria, Map filters) {
        if (filters.ns) criteria = criteria.ilike('namespace', "%${filters.ns}%")
        if (filters.key) criteria = criteria.ilike('key', "%${filters.key}%")
        if (filters.value) criteria = criteria.ilike('value', "%${filters.value}%")
        criteria
    }
}