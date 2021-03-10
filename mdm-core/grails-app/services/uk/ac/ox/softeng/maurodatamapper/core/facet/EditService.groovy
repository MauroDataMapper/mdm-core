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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

@Slf4j
class EditService {

    MessageSource messageSource
    @Autowired(required = false)
    List<ModelService> modelServices

    @Autowired(required = false)
    List<CatalogueItemService> catalogueItemServices

    Edit get(Serializable id) {
        Edit.get(Utils.toUuid(id))
    }

    List<Edit> list(Map pagination) {
        Edit.list(pagination)
    }

    Long count() {
        Edit.count()
    }

    Edit save(Edit edit) {
        edit.save(flush: true)
    }

    List<Edit> findAllByResource(String resourceDomainType, UUID resourceId, Map pagination = [sort: 'dateCreated', order: 'asc']) {
        Edit.findAllByResource(resourceDomainType, resourceId, pagination)
    }

    /**
     * Find the edits on a catalogue item, and on all the resources that belong to that catalogue item
     * @param catalogueItemDomainType
     * @param catalogueItemId
     * @param pagination
     * @return Map of edits on the catalogue item and edits on all resources which belong to the catalogue item
     */
    Map findAllByCatalogueItem(String catalogueItemDomainType,
                               UUID catalogueItemId,
                               Map pagination = [sort: 'dateCreated', order: 'asc']) {
        List exclude = []
        Map edits = [:]

        log.debug("findAllByCatalogueItem( {} {}", catalogueItemDomainType, catalogueItemId.toString())

        CatalogueItemService service = catalogueItemServices.find { it.handles(catalogueItemDomainType) }

        if (service) {
            CatalogueItem catalogueItem = service.get(catalogueItemId)
            if (catalogueItem) {
                edits = findAllByResourceAndDescendants(catalogueItem, exclude, pagination)
            }
        }

        edits
    }

    /**
     * Find the edits on an EditHistoryAware resource and on all resources which belong to it
     * @param resource
     * @param exclude For safety, a list of items alreday checked and to be excluded from further checking
     * @param pagination
     * @return Map of edits on the resource and edits on all resources which belong to the resource
     */
    Map findAllByResourceAndDescendants(EditHistoryAware resource,
                                        List exclude = [],
                                        Map pagination) {
        def allEdits = [:]
        def edits = [:]

        if (resource) {
            //Get edits directly recorded for this resource
            def resourceEdits = resource.getEdits()
            if (resourceEdits) {
                edits[resource.id] = resourceEdits
            }

            //Look for edits on all properties of this resource
            resource.getEditHistoryAwareDescendants().each {String propertyName ->
                def item = resource[propertyName]

                //Only look at this item if we haven't already done so
                if (item && !exclude.contains(item)) {
                    exclude += item
                    if (item instanceof EditHistoryAware) {
                        def itemEdits = findAllByResourceAndDescendants(item, exclude, pagination)
                        if (itemEdits) {
                            edits[propertyName] = itemEdits
                        }
                    } else if (item instanceof Collection) {
                        def itemEdits = []
                        item.each {member ->
                            if (member instanceof EditHistoryAware) {
                                def memberEdits = findAllByResourceAndDescendants(member, exclude, pagination)
                                if (memberEdits) {
                                    itemEdits += memberEdits
                                }
                            }
                        }
                        if (itemEdits) {
                            edits[propertyName] = itemEdits
                        }
                    }
                }
            }

            if (edits) {
                allEdits[resource.id] = edits
            }
        }

        allEdits
    }

    void createAndSaveEdit(UUID resourceId, String resourceDomainType, String description, User createdBy, boolean flush = false) {
        Edit edit = new Edit(resourceId: resourceId, resourceDomainType: resourceDomainType,
                             description: description, createdBy: createdBy.emailAddress)
        if (edit.validate()) {
            edit.save(flush: flush, validate: false)
        } else {
            throw new ApiInvalidModelException('ES01', 'Created Edit is invalid', edit.errors, messageSource)
        }
    }
}