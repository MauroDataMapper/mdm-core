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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.VersionLinkAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class VersionLink implements MultiFacetItemAware {

    UUID id

    @BindUsing({ obj, source -> VersionLinkType.findFromMap(source) })
    VersionLinkType linkType
    VersionLinkAware targetModel
    UUID targetModelId
    String targetModelDomainType

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        multiFacetAwareItemId nullable: true, validator: {val, obj ->
            if (!val && !obj.multiFacetAwareItem) return ['default.null.message']
            if (val == obj.targetModelId && obj.multiFacetAwareItemDomainType == obj.targetModelDomainType) {
                return ['invalid.same.property.message', 'targetModel']
            }
        }
    }

    static mapping = {
        batchSize 20
        multiFacetAwareItemId index: 'version_link_catalogue_item_idx'
        targetModelId index: 'version_link_target_model_idx'
    }

    // Required to prevent bidirectional mapping to catalogue items
    static mappedBy = [
        'multiFacetAwareItem': 'none',
        'targetModel'        : 'none'
    ]

    static transients = ['targetModel', 'model', 'multiFacetAwareItem']

    VersionLink() {
    }

    @Override
    String getDomainType() {
        VersionLink.simpleName
    }

    @Override
    String getPathPrefix() {
        'vl'
    }

    @Override
    String getPathIdentifier() {
        "${linkType}.${targetModelId}"
    }

    @Override
    String getEditLabel() {
        "VersionLink:${linkType}:${targetModelId}"
    }

    void setModel(VersionLinkAware model) {
        setMultiFacetAwareItem(model as MultiFacetAware)
    }

    VersionLinkAware getModel() {
        multiFacetAwareItem as VersionLinkAware
    }

    UUID getModelId() {
        multiFacetAwareItemId
    }

    String getModelDomainType() {
        multiFacetAwareItemDomainType
    }

    void setTargetModel(VersionLinkAware targetModel) {
        this.targetModel = targetModel
        this.targetModelDomainType = targetModel.domainType
        this.targetModelId = targetModel.id
    }

    static DetachedCriteria<VersionLink> by() {
        new DetachedCriteria<VersionLink>(VersionLink)
    }

    static DetachedCriteria<VersionLink> byModelId(Serializable modelId) {
        by().eq('multiFacetAwareItemId', Utils.toUuid(modelId))
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

    static DetachedCriteria<VersionLink> byAnyModelId(Serializable multiFacetAwareItemId) {
        by().or {
            eq 'multiFacetAwareItemId', Utils.toUuid(multiFacetAwareItemId)
            eq 'targetModelId', Utils.toUuid(multiFacetAwareItemId)
        }
    }

    static DetachedCriteria<VersionLink> byAnyModelIdInList(List<UUID> multiFacetAwareItemIds) {
        by().or {
            inList 'multiFacetAwareItemId', multiFacetAwareItemIds
            inList 'targetModelId', multiFacetAwareItemIds
        }
    }

    static DetachedCriteria<VersionLink> bySourceModelAndLinkType(VersionAware source, VersionLinkType linkType) {
        by().eq('multiFacetAwareItemId', source.id)
            .eq('linkType', linkType)
    }

    static DetachedCriteria<VersionLink> bySourceModelAndTargetModelAndLinkType(VersionAware source, VersionAware target,
                                                                                VersionLinkType linkType) {
        by().eq('multiFacetAwareItemId', source.id)
            .eq('targetModelId', target.id)
            .eq('linkType', linkType)
    }

    static DetachedCriteria<VersionLink> withFilter(DetachedCriteria<VersionLink> criteria, Map filters) {
        if (filters.linkType) criteria = criteria.eq('linkType', VersionLinkType.findForLabel(filters.linkType))
        criteria
    }
}