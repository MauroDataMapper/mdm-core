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
package uk.ac.ox.softeng.maurodatamapper.core.traits.service

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.VersionLinkAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import groovy.transform.SelfType

/**
 * @since 29/04/2021
 */
@SelfType([MultiFacetAwareService, MdmDomainService])
trait VersionLinkAwareService<K extends VersionLinkAware> {

    abstract Class<K> getVersionLinkAwareClass()

    abstract boolean handles(String domainType)

    abstract VersionLinkService getVersionLinkService()

    abstract List<K> findAllModelsByIdInList(List<UUID> ids, Map pagination)

    abstract List<UUID> getAllModelIds()

    abstract List<K> getAll(Collection<UUID> ids)

    void removeVersionLinkFromModel(UUID versionLinkAwareId, VersionLink versionLink) {
        removeFacetFromDomain(versionLinkAwareId, versionLink.id, 'versionLinks')
    }

    List<UUID> findAllSupersededModelIds(List<K> models) {
        findAllSupersededIds(models.collect { (it as MdmDomain).id })
    }

    List<K> findAllDocumentationSupersededModels(Map pagination) {
        List<UUID> ids = findAllDocumentSupersededIds(getAllModelIds())
        findAllModelsByIdInList(ids, pagination)
    }

    List<K> findAllModelSupersededModels(Map pagination) {
        List<UUID> ids = findAllModelSupersededIds(getAllModelIds())
        findAllModelsByIdInList(ids, pagination)
    }

    List<UUID> findAllExcludingDocumentSupersededIds(List<UUID> readableIds) {
        readableIds - findAllDocumentSupersededIds(readableIds)
    }

    List<UUID> findAllExcludingModelSupersededIds(List<UUID> readableIds) {
        readableIds - findAllModelSupersededIds(readableIds)
    }

    List<UUID> findAllExcludingDocumentAndModelSupersededIds(List<UUID> readableIds) {
        readableIds - findAllSupersededIds(readableIds)
    }

    List<UUID> findAllSupersededIds(List<UUID> readableIds) {
        (findAllDocumentSupersededIds(readableIds) + findAllModelSupersededIds(readableIds)).toSet().toList()
    }

    List<UUID> findAllDocumentSupersededIds(List<UUID> readableIds) {
        versionLinkService.filterModelIdsWhereModelIdIsDocumentSuperseded(getVersionLinkAwareClass().simpleName, readableIds)
    }

    List<UUID> findAllModelSupersededIds(List<UUID> readableIds) {
        // All versionLinks which are targets of model version links
        List<VersionLink> modelVersionLinks = versionLinkService.findAllByTargetMultiFacetAwareItemIdInListAndIsModelSuperseded(readableIds)

        // However they are only superseded if the source of this link is finalised
        modelVersionLinks.findAll {
            VersionAware sourceModel = get(it.multiFacetAwareItemId) as VersionAware
            sourceModel.finalised
        }.collect {it.targetModelId}
    }
}