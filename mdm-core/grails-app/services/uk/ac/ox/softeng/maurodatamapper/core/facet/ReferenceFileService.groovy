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
import uk.ac.ox.softeng.maurodatamapper.core.model.file.CatalogueFileService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetAwareService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.DetachedCriteria

class ReferenceFileService implements CatalogueFileService<ReferenceFile>, MultiFacetItemAwareService<ReferenceFile> {

    ReferenceFile get(Serializable id) {
        ReferenceFile.get(id)
    }

    List<ReferenceFile> list(Map args) {
        ReferenceFile.list(args)
    }

    Long count() {
        ReferenceFile.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(ReferenceFile file, boolean flush = false) {
        if (!file) return
        MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(file.multiFacetAwareItemDomainType)
        service.removeReferenceFileFromMultiFacetAware(file.multiFacetAwareItemId, file)
        file.delete(flush: flush)
    }

    @Override
    void saveMultiFacetAwareItem(ReferenceFile referenceFile) {
        if (!referenceFile) return
        MultiFacetAwareService service = findServiceForMultiFacetAwareDomainType(referenceFile.multiFacetAwareItemDomainType)
        service.save(referenceFile.multiFacetAwareItem)
    }

    @Override
    void addFacetToDomain(ReferenceFile facet, String domainType, UUID domainId) {
        if (!facet) return
        MultiFacetAware domain = findMultiFacetAwareItemByDomainTypeAndId(domainType, domainId)
        facet.multiFacetAwareItem = domain
        domain.addToReferenceFiles(facet)
    }

    @Override
    ReferenceFile createNewFile(String name, byte[] contents, String type, User user) {
        createNewFileBase(name, contents, type, user.emailAddress)
    }

    @Override
    ReferenceFile resizeImage(ReferenceFile catalogueFile, int size) {
        ReferenceFile referenceFile = resizeImageBase(catalogueFile, size)
        referenceFile.multiFacetAwareItemDomainType = catalogueFile.multiFacetAwareItemDomainType
        referenceFile.multiFacetAwareItemId = catalogueFile.multiFacetAwareItemId
        referenceFile
    }

    @Override
    ReferenceFile findByMultiFacetAwareItemIdAndId(UUID multiFacetAwareItemId, Serializable id) {
        ReferenceFile.byMultiFacetAwareItemIdAndId(multiFacetAwareItemId, id).get()
    }

    @Override
    List<ReferenceFile> findAllByMultiFacetAwareItemId(UUID multiFacetAwareItemId, Map pagination) {
        ReferenceFile.byMultiFacetAwareItemId(multiFacetAwareItemId).list(pagination)
    }

    @Override
    DetachedCriteria<ReferenceFile> getBaseDeleteCriteria() {
        ReferenceFile.by()
    }

}