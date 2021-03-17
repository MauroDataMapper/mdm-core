/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.datamodel.facet

import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import groovy.util.logging.Slf4j

@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class ModelImport implements CreatorAware {

    UUID id
    UUID catalogueItemId
    String catalogueItemDomainType
    CatalogueItem catalogueItem
    ModelItem importedModelItem
    UUID importedModelItemId
    String importedModelItemDomainType

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        catalogueItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.catalogueItem && !obj.catalogueItem.ident()) return true
            ['default.null.message']
        }
        catalogueItemDomainType nullable: false, blank: false

        /**
         * Custom constraint validation on the importedModelItemId to prevent duplicate imports.
         * A duplicate is defined as two records having the same modelItemId and importedModelItemId.
         * Using a unique constraint did not work as it fails in cases where the modelItem has not yet been created
         * (which occurs when copying a catalogue item and its facets).
         * So only when dealing with an already existing catalogue item the valdiator checks for an already
         * existing ModelImport.
         */
        importedModelItemId nullable: false, validator: {UUID importedModelItemId, ModelImport instance ->
            //If the catalogue item does not yet have an ID then this is a new catalogue item, 
            //so duplication is not possible
            if (!instance.catalogueItem.ident()) return true

            //Else this is an existing catalogue item, in which case check that the import does not
            //already exist
            ModelImport.byNotAlreadyImported(instance).count() ? ['default.not.unique.message'] : true
        }
        importedModelItemDomainType nullable: false, blank: false
    }

    static mapping = {
        batchSize 20
        catalogueItemId index: 'model_import_catalogue_item_idx'
        importedModelItemId index: 'model_import_imported_catalogue_item_idx'
    }

    // Required to prevent bidirectional mapping to catalogue items
    static mappedBy = [
        'modelItem'        : 'none',
        'importedModelItem': 'none'
    ]

    static transients = ['importedModelItem', 'modelItem', 'catalogueItem']

    ModelImport() {
    }

    @Override
    String getDomainType() {
        ModelImport.simpleName
    }


    String getEditLabel() {
        "ModelImport:${importedModelItemDomainType}:${importedModelItemId}"
    }

    void setImportedModelItem(ModelItem modelItem) {
        if (!modelItem) return
        importedModelItem = modelItem
        importedModelItemDomainType = modelItem.domainType
        importedModelItemId = modelItem.id
    }

    void setCatalogueItem(CatalogueItem catalogueItem) {
        this.catalogueItem = catalogueItem
        catalogueItemId = catalogueItem.id
        catalogueItemDomainType = catalogueItem.domainType
    }

    static DetachedCriteria<ModelImport> by() {
        new DetachedCriteria<ModelImport>(ModelImport)
    }

    static DetachedCriteria<ModelImport> byCatalogueItemId(Serializable catalogueItemId) {
        by().eq('catalogueItemId', Utils.toUuid(catalogueItemId))
    }

    static DetachedCriteria<ModelImport> byCatalogueItemIdAndId(Serializable catalogueItemId, Serializable resourceId) {
        byCatalogueItemId(catalogueItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<ModelImport> byCatalogueItemIdAndImportedModelItemId(Serializable catalogueItemId, Serializable importedModelItemId) {
        byCatalogueItemId(catalogueItemId)
            .eq('importedModelItemId', Utils.toUuid(importedModelItemId))
    }

    static DetachedCriteria<ModelImport> byAnyItemId(Serializable modelItemId) {
        by().or {
            eq 'catalogueItemId', Utils.toUuid(modelItemId)
            eq 'importedModelItemId', Utils.toUuid(modelItemId)
        }
    }

    static DetachedCriteria<ModelImport> byNotAlreadyImported(ModelImport instance) {
        DetachedCriteria criteria = by()
            .eq('catalogueItemId', instance.catalogueItemId)
            .eq('extendedModelItemId', instance.importedModelItemId)
        instance.id ? criteria.ne('id', instance.id) : criteria
    }

    static DetachedCriteria<ModelImport> withFilter(DetachedCriteria<ModelImport> criteria, Map filters) {
        criteria
    }

    /**
     * Select the distinct IDs of catalogue items which are imported into the catalogue item with the specified ID.
     * Used by other domains to select which of themselves have been imported into a particular model.
     * Will do a query like 'DISTINCT(imported_catalogue_item_id) FROM core.model_import WHERE catalogue_item_id = modelItemId'
     *
     * @param modelItemId The ID of the catalogue item which imported another catalogue item
     * @return DetachedCriteria<ModelImport>  Projection for distinct imported_catalogue_item_id
     */
    static DetachedCriteria<ModelImport> importedByCatalogueItemId(Serializable catalogueItemId) {
        byCatalogueItemId(catalogueItemId)
        .projections {
            distinct('importedModelItemId')
        }
    }    
}