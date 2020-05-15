package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.CatalogueFileConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.file.CatalogueFile
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.CatalogueItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class ReferenceFile implements CatalogueFile, CatalogueItemAware {

    UUID id

    static constraints = {
        CallableConstraints.call(CatalogueFileConstraints, delegate)
        catalogueItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.catalogueItem && !obj.catalogueItem.ident()) return true
            ['default.null.message']
        }
    }

    static mapping = {
        createdBy cascade: 'none', index: 'referencefile_created_by_idx'
    }

    static transients = ['catalogueItem']

    ReferenceFile() {
    }

    @Override
    String getDomainType() {
        ReferenceFile.simpleName
    }


    def beforeValidate() {
        fileSize = fileContents?.size()
    }

    @Override
    String getEditLabel() {
        "${domainType}:${fileName}"
    }

    static DetachedCriteria<ReferenceFile> byCatalogueItemId(Serializable catalogueItemId) {
        new DetachedCriteria<ReferenceFile>(ReferenceFile).eq('catalogueItemId', Utils.toUuid(catalogueItemId))
    }

    static DetachedCriteria<ReferenceFile> byCatalogueItemIdAndId(Serializable catalogueItemId, Serializable id) {
        byCatalogueItemId(catalogueItemId).idEq(Utils.toUuid(id))
    }

    static DetachedCriteria<ReferenceFile> withFilter(DetachedCriteria<ReferenceFile> criteria, Map filters) {
        withBaseFilter(criteria, filters)
    }
}