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
package uk.ac.ox.softeng.maurodatamapper.dataflow

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponent
import uk.ac.ox.softeng.maurodatamapper.dataflow.component.DataClassComponentService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.grails.orm.hibernate.proxy.HibernateProxyHandler

class DataFlowService extends ModelItemService<DataFlow> {

    DataClassComponentService dataClassComponentService

    private static HibernateProxyHandler proxyHandler = new HibernateProxyHandler()

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

        semanticLinkService.deleteBySourceCatalogueItemAndTargetCatalogueItemAndLinkType(source, target, SemanticLinkType.IS_FROM)
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

    DataFlow findByTargetDataModelIdAndId(UUID dataModelId, Serializable id) {
        DataFlow.byTargetDataModelIdAndId(dataModelId, Utils.toUuid(id)).get()
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

        dataFlows.addAll buildTargetChain(allReadableDataFlows, allReadableDataFlows.findAll { it.refersToDataModelId(dataModelId) })
        dataFlows.addAll buildSourceChain(allReadableDataFlows, allReadableDataFlows.findAll { it.refersToDataModelId(dataModelId) })

        dataFlows.toList() as List<DataFlow>
    }

    def buildTargetChain(List<DataFlow> readableDataFlows, Collection<DataFlow> dataFlowChain) {

        Set<DataModel> targets = dataFlowChain.collect { it.target }.toSet()

        Set<DataFlow> targetDataFlows = readableDataFlows.findAll { it.source in targets }.toSet()

        if (targetDataFlows) {
            targetDataFlows = buildTargetChain(readableDataFlows - targetDataFlows, targetDataFlows)
        }

        dataFlowChain + targetDataFlows
    }

    def buildSourceChain(List<DataFlow> readableDataFlows, Collection<DataFlow> dataFlowChain) {

        Set<DataModel> sources = dataFlowChain.collect { it.source }.toSet()

        Set<DataFlow> sourceDataFlows = readableDataFlows.findAll { it.target in sources }.toSet()

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
    Class<DataFlow> getModelItemClass() {
        DataFlow
    }

    @Override
    boolean hasTreeTypeModelItems(DataFlow dataFlow) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataFlow dataFlow) {
        []
    }

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
    List<DataFlow> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        DataFlow.byClassifierId(classifier.id).list().findAll { userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id) }
    }

    @Override
    DataFlow updateIndexForModelItemInParent(DataFlow modelItem, CatalogueItem parent, int newIndex) {
        throw new ApiNotYetImplementedException('DCSXX', 'DataFlow Ordering')
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
}