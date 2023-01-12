/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.dataflow.component

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class DataClassComponentService extends ModelItemService<DataClassComponent> {

    DataElementComponentService dataElementComponentService
    PathService pathService

    @Override
    DataClassComponent get(Serializable id) {
        DataClassComponent.get(id)
    }

    @Override
    List<DataClassComponent> getAll(Collection<UUID> ids) {
        DataClassComponent.getAll(ids).findAll()
    }

    List<DataClassComponent> list(Map args) {
        DataClassComponent.list(args)
    }

    Long count() {
        DataClassComponent.count()
    }

    void delete(Serializable id) {
        DataClassComponent dataClassComponent = get(id)
        if (dataClassComponent) delete(dataClassComponent)
    }

    void delete(DataClassComponent dataClassComponent, boolean flush = false) {
        if (!dataClassComponent) return
        DataFlow dataFlow = proxyHandler.unwrapIfProxy(dataClassComponent.dataFlow) as DataFlow
        dataClassComponent.dataFlow = dataFlow

        List<DataElementComponent> dataElementComponents = dataElementComponentService.findAllByDataClassComponentId(
            dataClassComponent.id
        )
        dataElementComponentService.deleteAll(dataElementComponents)
        dataClassComponent.dataElementComponents = []

        dataClassComponent.dataFlow.removeFromDataClassComponents(dataClassComponent)
        dataClassComponent.breadcrumbTree.removeFromParent()

        dataClassComponent.sourceDataClasses.each { source ->
            dataClassComponent.sourceDataClasses.each { target ->
                semanticLinkService.deleteBySourceMultiFacetAwareItemAndTargetMultiFacetAwareItemAndLinkType(source, target, SemanticLinkType.IS_FROM)
            }
        }

        dataFlow.trackChanges() // Discard any latent changes to the DataFlow as we dont want them
        dataClassComponent.delete(flush: flush)
    }

    @Override
    void deleteAll(Collection<DataClassComponent> dataClassComponents) {
        dataClassComponents.each {
            delete(it)
        }
    }

    @Override
    void deleteAllByModelIds(Set<UUID> modelIds) {

        List<UUID> dataClassComponentIds = DataClassComponent.by().where {
            dataFlow {
                or {
                    inList 'source.id', modelIds
                    inList 'target.id', modelIds
                }
            }
        }.id().list() as List<UUID>

        if (dataClassComponentIds) {

            log.trace('Removing DataElementComponents in {} DataClassComponents', dataClassComponentIds.size())
            dataElementComponentService.deleteAllByModelIds(modelIds)

            log.trace('Removing links to DataClasses in {} DataClassComponents', dataClassComponentIds.size())
            sessionFactory.currentSession
                .createSQLQuery('delete from dataflow.join_data_class_component_to_source_data_class where data_class_component_id in :ids')
                .setParameter('ids', dataClassComponentIds)
                .executeUpdate()

            sessionFactory.currentSession
                .createSQLQuery('delete from dataflow.join_data_class_component_to_target_data_class where data_class_component_id in :ids')
                .setParameter('ids', dataClassComponentIds)
                .executeUpdate()

            log.trace('Removing facets for {} DataClassComponents', dataClassComponentIds.size())
            deleteAllFacetsByMultiFacetAwareIds(dataClassComponentIds,
                                                'delete from dataflow.join_dataclasscomponent_to_facet where dataclasscomponent_id in :ids')

            log.trace('Removing {} DataClassComponents', dataClassComponentIds.size())
            sessionFactory.currentSession
                .createSQLQuery('delete from dataflow.data_class_component where id in :ids')
                .setParameter('ids', dataClassComponentIds)
                .executeUpdate()

            log.trace('DataClassComponents removed')
        }
    }

    DataClassComponent findByDataFlowIdAndId(UUID dataFlowId, Serializable id) {
        DataClassComponent.byDataFlowIdAndId(dataFlowId, Utils.toUuid(id)).get()
    }

    boolean existsByDataFlowIdAndId(UUID dataFlowId, Serializable id) {
        DataClassComponent.byDataFlowIdAndId(dataFlowId, Utils.toUuid(id)).count() == 1
    }

    List<DataClassComponent> findAllByDataFlowId(UUID dataFlowId, Map pagination = [:]) {
        DataClassComponent.byDataFlowId(dataFlowId).list(pagination)
    }

    List<DataClassComponent> findAllByDataFlowIdAndDataClassId(UUID dataFlowId, UUID dataClassId, Map pagination = [:]) {
        DataClassComponent.byDataFlowIdAndDataClassId(dataFlowId, dataClassId).list(pagination)
    }

    @Override
    DataClassComponent findByIdJoinClassifiers(UUID id) {
        DataClassComponent.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        DataClassComponent.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<DataClassComponent> findAllByClassifier(Classifier classifier) {
        DataClassComponent.byClassifierId(classifier.id).list()
    }

    @Override
    List<DataClassComponent> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)}
    }

    @Override
    List<DataClassComponent> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                        String searchTerm, String domainType) {
        []
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        false
    }

    DataClassComponent findOrCreateDataClassComponentForDataElementComponent(DataElementComponent dataElementComponent, User user) {
        throw new ApiNotYetImplementedException('DCCS', 'findOrCreateDataClassComponentForDataElementComponent')
    }

   /**
     * When importing a DataFlow, do checks and setting of DataClassComponents  as follows:
     * (1) Set the createdBy of the DataClassComponent to be the importing user
     * (2) Check facets
     * (3) Use path service to lookup the source and target data classes, throwing an exception if either cannot be found
     * (4) Use DataElementComponentService to check associations on all DataElementComponents
     *
     *
     * @param importingUser The importing user, who will be used to set createdBy
     * @param dataFlow The DataFlow to be imported
     * @param dataClassComponent The DataClassComponent to be imported
     */
    void checkImportedDataClassComponentAssociations(User importingUser, DataFlow dataFlow, DataClassComponent dataClassComponent) {

        dataClassComponent.createdBy = importingUser.emailAddress
        checkFacetsAfterImportingCatalogueItem(dataClassComponent)

        def rawSourceDataClasses = dataClassComponent.sourceDataClasses
        Set<DataClass> resolvedSourceDataClasses = []

        if (rawSourceDataClasses) {
            rawSourceDataClasses.each {sdc ->
                Path path = sdc.uncheckedPath
                DataClass sourceDataClass = pathService.findResourceByPathFromRootClass(DataModel, path) as DataClass

                if (sourceDataClass) {
                    resolvedSourceDataClasses.add(sourceDataClass)
                } else {
                    throw new ApiBadRequestException('DCCI01', "Source DataClass retrieval for ${path} failed")
                }
            }

            dataClassComponent.sourceDataClasses = resolvedSourceDataClasses
        }

        def rawTargetDataClasses = dataClassComponent.targetDataClasses
        Set<DataClass> resolvedTargetDataClasses = []

        if (rawTargetDataClasses) {
            rawTargetDataClasses.each {tdc ->
                Path path = tdc.uncheckedPath
                DataClass targetDataClass = pathService.findResourceByPathFromRootClass(DataModel, path) as DataClass

                if (targetDataClass) {
                    resolvedTargetDataClasses.add(targetDataClass)
                } else {
                    throw new ApiBadRequestException('DCCI02', "Target DataClass retrieval for ${path} failed")
                }
            }

            dataClassComponent.targetDataClasses = resolvedTargetDataClasses
        }

        if (dataClassComponent.dataElementComponents) {
            dataClassComponent.dataElementComponents.each { dec ->
                dataElementComponentService.checkImportedDataElementComponentAssociations(importingUser, dataFlow, dataClassComponent, dec)
            }
        }
    }

    @Override
    List<DataClassComponent> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        DataClassComponent.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<DataClassComponent> findAllByMetadataNamespace(String namespace, Map pagination) {
        DataClassComponent.byMetadataNamespace(namespace).list(pagination)
    }
}
