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

import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.ModelItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria
import grails.rest.Resource
import groovy.util.logging.Slf4j

@Slf4j
@Resource(readOnly = false, formats = ['json', 'xml'])
class ModelExtend implements ModelItemAware, CreatorAware {

    UUID id
    ModelItem extendedModelItem
    UUID extendedModelItemId
    String extendedModelItemDomainType

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        modelItemId nullable: true, validator: {val, obj ->
            if (val) return true
            if (!val && obj.modelItem && !obj.modelItem.ident()) return true
            ['default.null.message']
        }
        modelItemDomainType nullable: false, blank: false

        /**
         * Custom constraint validation on the extendedModelItemId to prevent duplicate extends.
         * A duplicate is defined as two records having the same modelItemId and extendedModelItemId.
         * Using a unique constraint did not work as it fails in cases where the modelItem has not yet been created
         * (which occurs when copying a catalogue item and its facets).
         * So only when dealing with an already existing catalogue item the valdiator checks for an already
         * existing ModelExtend.
         */
        extendedModelItemId nullable: false, validator: {UUID extendedModelItemId, ModelExtend instance ->
            //If the catalogue item does not yet have an ID then this is a new catalogue item, 
            //so duplication is not possible
            if (!instance.modelItem.ident()) return true

            //Else this is an existing catalogue item, in which case check that the extend does not
            //already exist
            ModelExtend.byNotAlreadyExtended(instance).count() ? ['default.not.unique.message'] : true

        }
        extendedModelItemDomainType nullable: false, blank: false
    }

    static mapping = {
        batchSize 20
        modelItemId index: 'model_extend_catalogue_item_idx'
        extendedModelItemId index: 'model_extend_extended_catalogue_item_idx'
    }

    // Required to prevent bidirectional mapping to catalogue items
    static mappedBy = [
        'modelItem'        : 'none',
        'extendedModelItem': 'none'
    ]

    static transients = ['extendedModelItem', 'modelItem']

    ModelExtend() {
    }

    @Override
    String getDomainType() {
        ModelExtend.simpleName
    }


    @Override
    String getEditLabel() {
        "ModelExtend:${extendedModelItemDomainType}:${extendedModelItemId}"
    }

    void setExtendedModelItem(ModelItem modelItem) {
        if (!modelItem) {
            return
        }

        extendedModelItem = modelItem
        extendedModelItemDomainType = modelItem.domainType
        extendedModelItemId = modelItem.id
    }

    static DetachedCriteria<ModelExtend> by() {
        new DetachedCriteria<ModelExtend>(ModelExtend)
    }

    static DetachedCriteria<ModelExtend> byModelItemId(Serializable modelItemId) {
        by().eq('modelItemId', Utils.toUuid(modelItemId))
    }

    static DetachedCriteria<ModelExtend> byModelItemIdAndId(Serializable modelItemId, Serializable resourceId) {
        byModelItemId(modelItemId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<ModelExtend> byModelItemIdAndExtendedModelItemId(Serializable modelItemId, Serializable extendedModelItemId) {
        byModelItemId(modelItemId)
            .eq('extendedModelItemId', Utils.toUuid(extendedModelItemId))
    }

    static DetachedCriteria<ModelExtend> byAnyModelItemId(Serializable modelItemId) {
        by().or {
            eq 'modelItemId', Utils.toUuid(modelItemId)
            eq 'extendedModelItemId', Utils.toUuid(modelItemId)
        }
    }

    static DetachedCriteria<ModelExtend> withFilter(DetachedCriteria<ModelExtend> criteria, Map filters) {
        criteria
    }

    static DetachedCriteria<ModelExtend> byNotAlreadyExtended(ModelExtend instance) {
        DetachedCriteria criteria = by()
            .eq('modelItemId', instance.modelItemId)
            .eq('extendedModelItemId', instance.extendedModelItemId)
        instance.id ? criteria.ne('id', instance.id) : criteria
    }

    /**
     * Select the distinct IDs of catalogue items which are extended by the catalogue item with the specified ID.
     * Used by other domains to select which of themselves have been extended into a particular model.
     * Will do a query like 'DISTINCT(extended_catalogue_item_id) FROM core.model_extend WHERE catalogue_item_id = modelItemId'
     *
     * @param modelItemId The ID of the catalogue item which extended another catalogue item
     * @return DetachedCriteria<ModelExtend>     Projection for distinct extended_catalogue_item_id
     */
    static DetachedCriteria<ModelExtend> extendedByModelItemId(Serializable modelItemId) {
        byModelItemId(modelItemId)
            .projections {
                distinct('extendedModelItemId')
            }
    }
}