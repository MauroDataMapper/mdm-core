/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.dataflow

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponent
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponentService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j

@Slf4j
@Transactional
class DataFlowService extends ModelItemService<DataFlow> {

    DataClassComponentService dataClassComponentService
    PathService pathService

    @Override
    DataFlow get(Serializable id) {
        DataFlow.get(id)
    }

    @Override
    List<DataFlow> getAll(Collection<UUID> ids) {
        DataFlow.getAll(ids).findAll()
    }

    List<DataFlow> list(Map args) {
        DataFlow.list(args)
    }

    Long count() {
        DataFlow.count()
    }

    void delete(Serializable id) {
        DataFlow dataFlow = get(id)
        if (dataFlow) delete(dataFlow)
    }

    void delete(DataFlow dataFlow, boolean flush = false) {
        if (!dataFlow) return
        DataModel source = proxyHandler.unwrapIfProxy(dataFlow.source) as DataModel
        DataModel target = proxyHandler.unwrapIfProxy(dataFlow.target) as DataModel

        dataFlow.source = source
        dataFlow.target = target

        List<DataClassComponent> dataClassComponents = dataClassComponentService.findAllByDataFlowId(dataFlow.id)
        dataClassComponentService.deleteAll(dataClassComponents)
        dataFlow.dataClassComponents = []

        semanticLinkService.deleteBySourceMultiFacetAwareItemAndTargetMultiFacetAwareItemAndLinkType(source, target, SemanticLinkType.IS_FROM)
        dataFlow.breadcrumbTree.removeFromParent()
        dataFlow.target = null
        dataFlow.source = null
        dataFlow.delete(flush: flush)
    }

    @Override
    void deleteAll(Collection<DataFlow> dataFlows) {
        dataFlows.each {
            delete(it)
        }
    }

    @Override
    void deleteAllByModelIds(Set<UUID> modelIds) {

        List<UUID> dataFlowIds = DataFlow.byDataModelIdInList(modelIds).id().list() as List<UUID>

        if (dataFlowIds) {

            log.trace('Removing DataClassComponent in {} DataFlows', dataFlowIds.size())
            dataClassComponentService.deleteAllByModelIds(modelIds)

            log.trace('Removing facets for {} DataFlows', dataFlowIds.size())
            deleteAllFacetsByMultiFacetAwareIds(dataFlowIds,
                                                'delete from dataflow.join_dataflow_to_facet where dataflow_id in :ids')

            log.trace('Removing {} DataFlows', dataFlowIds.size())

            Utils.executeInBatches(modelIds, { ids ->
                sessionFactory.currentSession
                        .createSQLQuery('DELETE FROM dataflow.data_flow WHERE source_id in :ids OR target_id in :ids')
                        .setParameter('ids', ids)
                        .executeUpdate()
            })
            log.trace('DataFlows removed')
        }
    }

    DataFlow findByTargetDataModelIdAndId(UUID dataModelId, Serializable id) {
        DataFlow.byTargetDataModelIdAndId(dataModelId, Utils.toUuid(id)).get()
    }

    boolean existsByTargetDataModelIdAndId(UUID dataModelId, Serializable id) {
        DataFlow.byTargetDataModelIdAndId(dataModelId, Utils.toUuid(id)).count() == 1
    }

    boolean existsBySourceDataModelIdAndId(UUID dataModelId, Serializable id) {
        DataFlow.bySourceDataModelIdAndId(dataModelId, Utils.toUuid(id)).count() == 1
    }

    List<DataFlow> findAllReadableByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        if (!userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)) return []
        DataFlow.byDataModelIdInList(
            userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        ).list(pagination)
    }

    List<DataFlow> findAllReadableBySourceDataModel(UserSecurityPolicyManager userSecurityPolicyManager, UUID dataModelId, Map pagination = [:]) {
        if (!userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)) return []
        DataFlow.bySourceDataModelIdAndTargetDataModelIdInList(
            dataModelId,
            userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        ).list(pagination)

    }

    List<DataFlow> findAllReadableByTargetDataModel(UserSecurityPolicyManager userSecurityPolicyManager, UUID dataModelId, Map pagination = [:]) {
        if (!userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)) return []
        DataFlow.byTargetDataModelIdAndSourceDataModelIdInList(
            dataModelId,
            userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        ).list(pagination)
    }

    List<DataFlow> findAllReadableChainedByDataModel(UserSecurityPolicyManager userSecurityPolicyManager, UUID dataModelId) {
        if (!userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)) return []
        List<DataFlow> allReadableDataFlows = DataFlow.byDataModelIdInList(
            userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        ).list()

        Set<DataFlow> dataFlows = [] as Set

        dataFlows.addAll buildTargetChain(allReadableDataFlows, allReadableDataFlows.findAll {it.refersToDataModelId(dataModelId)})
        dataFlows.addAll buildSourceChain(allReadableDataFlows, allReadableDataFlows.findAll {it.refersToDataModelId(dataModelId)})

        dataFlows.toList() as List<DataFlow>
    }

    def buildTargetChain(List<DataFlow> readableDataFlows, Collection<DataFlow> dataFlowChain) {

        Set<DataModel> targets = dataFlowChain.collect {it.target}.toSet()

        Set<DataFlow> targetDataFlows = readableDataFlows.findAll {it.source in targets}.toSet()

        if (targetDataFlows) {
            targetDataFlows = buildTargetChain(readableDataFlows - targetDataFlows, targetDataFlows)
        }

        dataFlowChain + targetDataFlows
    }

    def buildSourceChain(List<DataFlow> readableDataFlows, Collection<DataFlow> dataFlowChain) {

        Set<DataModel> sources = dataFlowChain.collect {it.source}.toSet()

        Set<DataFlow> sourceDataFlows = readableDataFlows.findAll {it.target in sources}.toSet()

        if (sourceDataFlows) {
            sourceDataFlows = buildSourceChain(readableDataFlows - sourceDataFlows, sourceDataFlows)
        }

        dataFlowChain + sourceDataFlows
    }

    /*
    TODO dataflow copying
             void copyDataFlowToNewTarget(DataFlow original, DataModel targetDataModel, CatalogueUser copier) {

                 DataFlow copy = new DataFlow()
                 copy = catalogueItemService.copyCatalogueItemInformation(original, copy, copier)

                 original.dataFlowComponents.each {dfc ->
                     copy.addToDataFlowComponents(dataFlowComponentService.copyDataFlowComponentToNewTarget(dfc, targetDataModel, copier))
                 }
                 original.source.addToSourceForDataFlows(copy)
                 targetDataModel.addToTargetForDataFlows(copy)
                 copy
             }

             DataFlow setCreatedBy(CatalogueUser catalogueUser, DataFlow dataFlow) {
                 dataFlow.createdBy = catalogueUser
                 dataFlow.dataFlowComponents.each {it.createdBy = catalogueUser}
                 dataFlow
             }

             DataFlow createDataFlow(CatalogueUser createdBy, String label, String description, String sourceDataModelLabel, DataModel
             targetDataModel) {
                 DataFlow dataFlow = new DataFlow(createdBy: createdBy, label: label, description: description)
                 dataModelService.findLatestByLabel(createdBy, sourceDataModelLabel)?.addToSourceForDataFlows(dataFlow)
                 targetDataModel?.addToTargetForDataFlows(dataFlow)
                 dataFlow
             }

             DataFlow replaceAndCreateDataFlow(CatalogueUser createdBy, String label, String description, String sourceDataModelLabel,
                                               String targetDataModelLabel) {

                 DataModel targetDataModel = dataModelService.findLatestByLabel(createdBy, targetDataModelLabel)

                 if (!targetDataModel) throw new ApiBadRequestException('DFS01', "No target DataModel with label ${targetDataModelLabel}")

                 DataFlow dataFlow = findByTargetDataModelAndLabel(targetDataModel.id, label)

                 if (dataFlow) {
                     if (dataFlow.source.label != sourceDataModelLabel) {
                         dataFlow.errors.rejectValue('label', 'invalid.dataflow.label.already.exists',
                                                     [label, targetDataModelLabel, dataFlow.source.label] as Object[],
                                                     'DataFlow [{0}] already exists between target [{1}] and source [{2}]')
                         return dataFlow
                     }
                     logger.info('Deleting DataFlow [{}] and replacing with new version', label)
                     delete(dataFlow)
                 }

                 createDataFlow(createdBy, label, description, sourceDataModelLabel, targetDataModel)
             }

             List<DataClassFlow> findAllDataClassFlowsForDataFlowId(Serializable dataFlowId) {
                 List<DataFlowComponent> dataFlowComponents = dataFlowComponentService.findAllByDataFlowId(dataFlowId)

                 List<DataClassFlow> dataClassFlows = []

                 dataFlowComponents.each {dfc ->
                     Set<DataClass> targetDataClasses = dfc.targetElements.collect {it.dataClass}.toSet()
                     Set<DataClass> sourceDataClasses = dfc.sourceElements.collect {it.dataClass}.toSet()

                     [sourceDataClasses, targetDataClasses].eachCombination {source, target ->
                         if (!dataClassFlows.any {it.sourceDataClass == source && it.targetDataClass == target}) {
                             dataClassFlows += new DataClassFlow(sourceDataClass: source, targetDataClass: target)
                         }
                     }
                 }

                 dataClassFlows
             }
         */

    @Override
    DataFlow findByIdJoinClassifiers(UUID id) {
        DataFlow.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        DataFlow.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<DataFlow> findAllByClassifier(Classifier classifier) {
        DataFlow.byClassifierId(classifier.id).list()
    }

    @Override
    List<DataFlow> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)}
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        false
    }

    @Override
    List<DataFlow> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                              String searchTerm, String domainType) {
        []
    }

    ObjectDiff<DataFlow> diff(DataFlow thisDataFlow, DataFlow otherDataFlow) {
        thisDataFlow.diff(otherDataFlow, 'none', null,null)
    }

    /**
     * When importing a DataFlow, do checks and setting of required values as follows:
     * (1) Set the createdBy of the DataFlow to be the importing user
     * (2) Check facets
     * (3) Use path service to lookup the source and target data models, throwing an exception if either cannot be found
     * (4) Use DataClassComponentService to check associations on all DataClassComponents
     *
     *
     * @param importingUser The importing user, who will be used to set createdBy
     * @param dataFlow The DataFlow to be imported
     * @param bindingMap The binding map, which is necessary for looking up source and target, plus dataClassComponents
     */
    void checkImportedDataFlowAssociations(User importingUser, DataFlow dataFlow, Map bindingMap = [:]) {
        //Note: The pathService requires a UserSecurityPolicyManager.
        //Assumption is that if we got this far then it is OK to read the Source DataModel because either (i) we came via a controller in which case
        //the user's ability to import a DataFlow has already been tested, or (ii) we are calling this method from a service test spec in which
        //case it is OK to read.

        dataFlow.createdBy = importingUser.emailAddress
        checkFacetsAfterImportingCatalogueItem(dataFlow)

        //source and target data model are imported by use of a path like "dm:my-data-model"
        Path sourcePath = Path.from( bindingMap.source.path)
        DataModel sourceDataModel = pathService.findResourceByPathFromRootClass(DataModel, sourcePath) as DataModel

        if (sourceDataModel) {
            dataFlow.source = sourceDataModel
        } else {
            throw new ApiBadRequestException('DFI01', "Source DataModel retrieval for ${sourcePath} failed")
        }

        Path targetPath = Path.from( bindingMap.target.path)
        DataModel targetDataModel = pathService.findResourceByPathFromRootClass(DataModel,targetPath) as DataModel

        if (targetDataModel) {
            dataFlow.target = targetDataModel
        } else {
            throw new ApiBadRequestException('DFI02', "Target DataModel retrieval for ${targetPath} failed")
        }

        //Check associations for the dataClassComponents
        if (dataFlow.dataClassComponents) {
            dataFlow.dataClassComponents.each {dcc ->
                dataClassComponentService.checkImportedDataClassComponentAssociations(importingUser, dataFlow, dcc)
            }

        }
    }

    @Override
    List<DataFlow> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        DataFlow.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<DataFlow> findAllByMetadataNamespace(String namespace, Map pagination) {
        DataFlow.byMetadataNamespace(namespace).list(pagination)
    }
}