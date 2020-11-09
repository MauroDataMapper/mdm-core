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
class ModelDataTypeService extends ModelItemService<ModelDataType> {

    @Override
    ModelDataType get(Serializable id) {
        ModelDataType.get(id)
    }

    Long count() {
        ModelDataType.count()
    }

    @Override
    List<ModelDataType> list(Map args) {
        ModelDataType.list(args)
    }

    @Override
    List<ModelDataType> getAll(Collection<UUID> ids) {
        ModelDataType.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<ModelDataType> catalogueItems) {
        ModelDataType.deleteAll(catalogueItems)
    }

    @Override
    void delete(ModelDataType modelDataType) {
        delete(modelDataType, true)
    }

    void delete(ModelDataType modelDataType, boolean flush) {
        modelDataType.delete(flush: flush)
    }

    @Override
    boolean hasTreeTypeModelItems(ModelDataType catalogueItem) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(ModelDataType catalogueItem) {
        []
    }

    @Override
    ModelDataType findByIdJoinClassifiers(UUID id) {
        ModelDataType.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        ModelDataType.byClassifierId(ModelDataType, classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<ModelDataType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        ModelDataType.byClassifierId(ModelDataType, classifier.id).list().findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)
        }
    }

    @Override
    Class<ModelDataType> getModelItemClass() {
        ModelDataType
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ModelDataType.simpleName
    }

    @Override
    List<ModelDataType> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                   String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        log.debug('Performing lucene label search')
        long start = System.currentTimeMillis()
        List<ModelDataType> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            results = ModelDataType.luceneLabelSearch(ModelDataType, searchTerm, readableIds.toList()).results
        }
        log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        results
    }

    ModelDataType createDataType(String label, String description, User createdBy, UUID modelResourceId, String modelResourceDomainId) {
        createDataType(label, description, createdBy.emailAddress, modelResourceId, modelResourceDomainId)
    }

    ModelDataType createDataType(String label, String description, String createdByEmailAddress, UUID modelResourceId, String modelResourceDomainId) {
        new ModelDataType(label: label, description: description, createdBy: createdByEmailAddress, modelResourceId: modelResourceId,
                          modelResourceDomainId: modelResourceDomainId)
    }

    ModelDataType findOrCreateDataTypeForDataModel(DataModel dataModel, String label, String description, User createdBy,
                                                   UUID modelResourceId, String modelResourceDomainId) {
        findOrCreateDataTypeForDataModel(dataModel, label, description, createdBy.emailAddress, modelResourceId, modelResourceDomainId)
    }

    ModelDataType findOrCreateDataTypeForDataModel(DataModel dataModel, String label, String description, String createdByEmailAddress,
                                                   UUID modelResourceId, String modelResourceDomainId) {
        String cleanLabel = label.trim()
        ModelDataType modelDataType = dataModel.findDataTypeByLabelAndType(cleanLabel, DataType.MODEL_DATA_DOMAIN_TYPE) as ModelDataType
        if (!modelDataType) {
            modelDataType = createDataType(cleanLabel, description, createdByEmailAddress, modelResourceId, modelResourceDomainId)
            dataModel.addToDataTypes(modelDataType)
        }
        modelDataType
    }
}
