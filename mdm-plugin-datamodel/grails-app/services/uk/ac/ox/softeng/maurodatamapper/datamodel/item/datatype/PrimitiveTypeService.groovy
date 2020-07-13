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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class PrimitiveTypeService extends ModelItemService<PrimitiveType> {

    public static final String DEFAULT_TEXT_TYPE_LABEL = 'Text'
    public static final String DEFAULT_TEXT_TYPE_DESCRIPTION = 'Text Data Type'

    @Override
    PrimitiveType get(Serializable id) {
        PrimitiveType.get(id)
    }

    Long count() {
        PrimitiveType.count()
    }

    @Override
    List<PrimitiveType> list(Map args) {
        PrimitiveType.list(args)
    }

    @Override
    List<PrimitiveType> getAll(Collection<UUID> ids) {
        PrimitiveType.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<PrimitiveType> catalogueItems) {
        PrimitiveType.deleteAll(catalogueItems)
    }

    @Override
    void delete(PrimitiveType primitiveType) {
        delete(primitiveType, true)
    }

    void delete(PrimitiveType primitiveType, boolean flush) {
        primitiveType.delete(flush: flush)
    }

    @Override
    PrimitiveType save(PrimitiveType catalogueItem) {
        catalogueItem.save(flush: true)
    }


    @Override
    boolean hasTreeTypeModelItems(PrimitiveType catalogueItem) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(PrimitiveType catalogueItem) {
        []
    }

    @Override
    PrimitiveType findByIdJoinClassifiers(UUID id) {
        PrimitiveType.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        PrimitiveType.byClassifierId(PrimitiveType, classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<PrimitiveType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        PrimitiveType.byClassifierId(PrimitiveType, classifier.id).list().findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)
        }
    }

    @Override
    Class<PrimitiveType> getModelItemClass() {
        PrimitiveType
    }

    @Override
    PrimitiveType updateIndexForModelItemInParent(PrimitiveType modelItem, CatalogueItem parent, int newIndex) {
        throw new ApiNotYetImplementedException('ETSXX', 'PrimitiveType Ordering')
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == PrimitiveType.simpleName
    }

    @Override
    List<PrimitiveType> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                   String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        log.debug('Performing lucene label search')
        long start = System.currentTimeMillis()
        List<PrimitiveType> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            results = PrimitiveType.luceneLabelSearch(PrimitiveType, searchTerm, readableIds.toList()).results
        }
        log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        results
    }

    PrimitiveType createDataType(String label, String description, User createdBy, String units = null) {
        new PrimitiveType(label: label, description: description, createdBy: createdBy, units: units)
    }

    public PrimitiveType findOrCreateDataTypeForDataModel(DataModel dataModel, String label, String description, User createdBy,
                                                           String units = null) {
        String cleanLabel = label.trim()
        PrimitiveType primitiveType = dataModel.findDataTypeByLabelAndType(cleanLabel, DataType.PRIMITIVE_DOMAIN_TYPE) as PrimitiveType
        if (!primitiveType) {
            primitiveType = createDataType(cleanLabel, description, createdBy, units)
            dataModel.addToDataTypes(primitiveType)
        }
        primitiveType
    }
}
