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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.merge.FieldPatchData
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.service.SummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.model.PersistentEntity

@Slf4j
@Transactional
class ModelDataTypeService extends ModelItemService<ModelDataType> implements SummaryMetadataAwareService {

    SummaryMetadataService summaryMetadataService
    DataTypeService dataTypeService
    DataModelService dataModelService
    PathService pathService

    @Override
    boolean handlesPathPrefix(String pathPrefix) {
        // Have to override to ensure we type DataTypeService
        false
    }

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
    List<ModelDataType> findAllByClassifier(Classifier classifier) {
        ModelDataType.byClassifierId(ModelDataType, classifier.id).list()
    }

    @Override
    List<ModelDataType> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {
            userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)
        }
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ModelDataType.simpleName
    }

    @Override
    ModelDataType copy(Model copiedDataModel, ModelDataType original, CatalogueItem nonModelParent, UserSecurityPolicyManager userSecurityPolicyManager) {
        dataTypeService.copy(copiedDataModel, original, nonModelParent, userSecurityPolicyManager) as ModelDataType
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
            results = ModelDataType.luceneLabelSearch(ModelDataType, searchTerm, readableIds.toList(), dataModelService.getAllReadablePathNodes(readableIds)).results
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

    @Override
    PersistentEntity getPersistentEntity() {
        grailsApplication.mappingContext.getPersistentEntity(DataType.name)
    }


    @Override
    List<ModelDataType> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        ModelDataType.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<ModelDataType> findAllByMetadataNamespace(String namespace, Map pagination) {
        ModelDataType.byMetadataNamespace(namespace).list(pagination)
    }

    /**
     * Special handler to apply a modification patch to a ModelDataType.modelResourceId and ModelDataType.modelResourceDomainType.
     * See ModelDataType.diff for how the MergeDiff is constructed. We want to set the correct modelResourceId, but this is difficult
     * when merging the MDT. In the diff we set a mergeField called modelResourcePath. In this method we use that modelResourcePath
     * to determine the ID of the correct model to point to.
     * @param modificationPatch
     * @param targetDomain
     * @param fieldName
     * @return
     */
    @Override
    boolean handlesModificationPatchOfField(FieldPatchData modificationPatch, MdmDomain targetBeingPatched, ModelDataType targetDomain, String fieldName) {
        if (fieldName == 'modelResourcePath') {

            MdmDomain modelResource = null
            if (targetBeingPatched.domainType == VersionedFolder.simpleName) {
                // If the modelResourcePath is a path to something internal to the source VF then this should also exist in the
                // target VF, either because it existed in the target VF before branching occurred, or because it has already been merged from
                // the source VF to target VF. So find a resource with the same label in the target VF, and use this.
                modelResource =
                    pathService.findResourceByPathFromRootResource(targetBeingPatched, Path.from(modificationPatch.sourceValue), targetBeingPatched.modelIdentifier)
            }

            if (!modelResource) {
                // Otherwise, the modelResourcePath is pointing to something external to the source VF. Look up the modelResource directly.
                // Note that pathService.findResourceByPath does not check security on the pathed resource
                // Look up the resource.
                // Note that pathService.findResourceByPath does not check security on the pathed resource
                modelResource = pathService.findResourceByPath(Path.from(modificationPatch.sourceValue))
            }

            if (modelResource) {
                targetDomain.modelResourceId = modelResource.id
                targetDomain.modelResourceDomainType = modelResource.domainType
                return true
            } else {
                throw new ApiInternalException('MDTS01', "Cannot find modelResource with path ${modificationPatch.sourceValue}")
            }
        }

        false
    }
}
