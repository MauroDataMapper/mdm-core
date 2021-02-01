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


import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.file.CatalogueFileService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.CatalogueItemAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.DetachedCriteria
import org.springframework.beans.factory.annotation.Autowired

class ReferenceFileService implements CatalogueFileService<ReferenceFile>, CatalogueItemAwareService<ReferenceFile> {

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    @Autowired(required = false)
    List <ContainerService> containerServices

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
        CatalogueItemService service = findCatalogueItemService(file.catalogueItemDomainType)
        service.removeReferenceFileFromCatalogueItem(file.catalogueItemId, file)
        file.delete(flush: flush)
    }

    @Override
    void saveCatalogueItem(ReferenceFile referenceFile) {
        if (!referenceFile) return
        CatalogueItemService catalogueItemService = findCatalogueItemService(referenceFile.catalogueItemDomainType)
        catalogueItemService.save(referenceFile.catalogueItem)
    }

    @Override
    void addFacetToDomain(ReferenceFile facet, String domainType, UUID domainId) {
        if (!facet) return
        CatalogueItem domain = findCatalogueItemByDomainTypeAndId(domainType, domainId)
        facet.catalogueItem = domain
        domain.addToReferenceFiles(facet)
    }

    @Override
    ReferenceFile createNewFile(String name, byte[] contents, String type, User user) {
        createNewFileBase(name, contents, type, user.emailAddress)
    }

    @Override
    ReferenceFile resizeImage(ReferenceFile catalogueFile, int size) {
        ReferenceFile referenceFile = resizeImageBase(catalogueFile, size)
        referenceFile.catalogueItemDomainType = catalogueFile.catalogueItemDomainType
        referenceFile.catalogueItemId = catalogueFile.catalogueItemId
        referenceFile
    }

    @Override
    ReferenceFile findByCatalogueItemIdAndId(UUID catalogueItemId, Serializable id) {
        ReferenceFile.byCatalogueItemIdAndId(catalogueItemId, id).get()
    }

    @Override
    List<ReferenceFile> findAllByCatalogueItemId(UUID catalogueItemId, Map pagination) {
        ReferenceFile.byCatalogueItemId(catalogueItemId).list(pagination)
    }

    @Override
    DetachedCriteria<ReferenceFile> getBaseDeleteCriteria() {
        ReferenceFile.by()
    }

}