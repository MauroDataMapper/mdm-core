/*
 * Copyright 2020 University of Oxford
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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.CatalogueItemAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class VersionLink implements CatalogueItemAware, CreatorAware {

    UUID id

    @BindUsing({obj, source -> VersionLinkType.findFromMap(source)})
    VersionLinkType linkType
    Model targetModel
    UUID targetModelId
    String targetModelDomainType

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        catalogueItemId nullable: true, validator: {val, obj ->
            if (!val && !obj.catalogueItem) return ['default.null.message']
            if (val == obj.targetModelId && obj.catalogueItemDomainType == obj.targetModelDomainType) {
                return ['invalid.same.property.message', 'targetModel']
            }
        }
    }

    static mapping = {
        batchSize 20
        catalogueItemId index: 'version_link_catalogue_item_idx'
        targetModelId index: 'version_link_target_model_idx'
    }

    // Required to prevent bidirectional mapping to catalogue items
    static mappedBy = [
        'catalogueItem': 'none',
        'targetModel'  : 'none'
    ]

    static transients = ['targetModel', 'model', 'catalogueItem']

    VersionLink() {
    }

    @Override
    String getDomainType() {
        VersionLink.simpleName
    }


    @Override
    String getEditLabel() {
        "VersionLink:${linkType}:${targetModelId}"
    }

    void setModel(Model model) {
        setCatalogueItem(model)
    }

    Model getModel() {
        catalogueItem as Model
    }

    UUID getModelId() {
        catalogueItemId
    }

    String getModelDomainType() {
        catalogueItemDomainType
    }

    void setTargetModel(Model targetModel) {
        this.targetModel = targetModel
        this.targetModelDomainType = targetModel.domainType
        this.targetModelId = targetModel.id
    }

    static DetachedCriteria<VersionLink> by() {
        new DetachedCriteria<VersionLink>(VersionLink)
    }

    static DetachedCriteria<VersionLink> byModelId(Serializable modelId) {
        by().eq('catalogueItemId', Utils.toUuid(modelId))
    }

    static DetachedCriteria<VersionLink> byModelIdAndLinkType(Serializable modelId, VersionLinkType linkType) {
        byModelId(modelId).eq('linkType', linkType)
    }

    static DetachedCriteria<VersionLink> byModelIdAndId(Serializable modelId, Serializable resourceId) {
        byModelId(modelId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<VersionLink> byTargetModelId(Serializable modelId) {
        by().eq('targetModelId', Utils.toUuid(modelId))
    }

    static DetachedCriteria<VersionLink> byTargetModelIdAndId(Serializable modelId, Serializable resourceId) {
        byTargetModelId(modelId).idEq(Utils.toUuid(resourceId))
    }

    static DetachedCriteria<VersionLink> byAnyModelId(Serializable catalogueItemId) {
        by().or {
            eq 'catalogueItemId', Utils.toUuid(catalogueItemId)
            eq 'targetModelId', Utils.toUuid(catalogueItemId)
        }
    }

    static DetachedCriteria<VersionLink> byAnyModelIdInList(List<UUID> catalogueItemIds) {
        by().or {
            inList 'catalogueItemId', catalogueItemIds
            inList 'targetModelId', catalogueItemIds
        }
    }

    static DetachedCriteria<VersionLink> bySourceModelAndLinkType(Model source, VersionLinkType linkType) {
        by().eq('catalogueItemId', source.id)
            .eq('linkType', linkType)
    }

    static DetachedCriteria<VersionLink> bySourceModelAndTargetModelAndLinkType(Model source, Model target,
                                                                                VersionLinkType linkType) {
        by().eq('catalogueItemId', source.id)
            .eq('targetModelId', target.id)
            .eq('linkType', linkType)
    }

    static DetachedCriteria<VersionLink> withFilter(DetachedCriteria<VersionLink> criteria, Map filters) {
        if (filters.linkType) criteria = criteria.eq('linkType', VersionLinkType.findForLabel(filters.linkType))
        criteria
    }
}