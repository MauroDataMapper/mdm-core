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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.hibernate.SessionFactory
import org.springframework.context.MessageSource

@Slf4j
@Transactional
class DataClassService extends ModelItemService<DataClass> {

    DataModelService dataModelService
    DataElementService dataElementService
    DataTypeService dataTypeService
    SemanticLinkService semanticLinkService
    MessageSource messageSource
    SessionFactory sessionFactory

    private static HibernateProxyHandler proxyHandler = new HibernateProxyHandler();

    DataClass get(Serializable id) {
        DataClass.get(id)
    }

    List<DataClass> list(Map args) {
        DataClass.list(args)
    }

    Long count() {
        DataClass.count()
    }

    @Override
    DataClass save(DataClass dataClass) {
        dataClass.save(flush: true)
        updateFacetsAfterInsertingCatalogueItem(dataClass)
    }

    @Override
    List<DataClass> getAll(Collection<UUID> ids) {
        DataClass.getAll(ids).findAll()
    }

    void delete(DataClass dataClass, boolean flush = false) {
        if (!dataClass) return
        DataModel dataModel = proxyHandler.unwrapIfProxy(dataClass.dataModel)
        dataModel.breadcrumbTree = proxyHandler.unwrapIfProxy(dataModel.breadcrumbTree)
        dataClass.dataModel = dataModel
        if (dataClass.parentDataClass) {
            DataClass parent = dataClass.parentDataClass
            parent.removeFromDataClasses(dataClass)
            parent.trackChanges()
        }
        removeAssociations(dataClass)
        List<DataElement> dataElements = dataElementService.findAllByDataClass(dataClass)
        dataElementService.deleteAll(dataElements)
        dataClass.dataElements = []
        dataModel.trackChanges() // Discard any latent changes to the DataModel as we dont want them
        dataClass.delete(flush: flush)
    }

    @Override
    void deleteAll(Collection<DataClass> dataClasses) {
        dataClasses.each {
            delete(it)
        }
    }

    boolean isUnusedDataClass(DataClass dataClass) {
        if (dataClass.maxMultiplicity != null) return false
        if (dataClass.minMultiplicity != null) return false
        if (dataClass.referenceTypes) return false
        if (dataClass.dataClasses) return false
        true
    }

    private Collection<DataElement> saveAllAndGetDataElements(Collection<DataClass> dataClasses) {

        Collection<DataClass> alreadySaved = dataClasses.findAll {it.ident() && it.isDirty()}
        Collection<DataClass> notSaved = dataClasses.findAll {!it.ident()}

        Collection<DataElement> dataElements = []

        if (alreadySaved) {
            log.trace('Straight saving {} dataClasses', alreadySaved.size())
            DataClass.saveAll(alreadySaved)
        }

        if (notSaved) {
            log.trace('Batch saving {} datatypes', notSaved.size())
            List batch = []
            int count = 0
            notSaved.each {dc ->
                dataElements.addAll dc.dataElements ?: []

                dc.dataClasses?.clear()
                dc.dataElements?.clear()
                dc.referenceTypes?.clear()

                batch.add dc
                count++
                if (count % DataClass.BATCH_SIZE == 0) {
                    batchSave(batch)
                    batch.clear()
                }
            }
            batchSave(batch)
            batch.clear()
        }
        dataElements
    }

    Collection<DataElement> hierarchySaveAllAndGetDataElements(Collection<DataClass> dataClasses) {

        Collection<DataElement> dataElements = []

        // No parent
        List<DataClass> parents = dataClasses.findAll {!it.parentDataClass}
        dataElements.addAll(saveAllAndGetDataElements(parents))

        while (parents) {

            List<DataClass> children = dataClasses.findAll {it.parentDataClass in parents}
            if (children) {
                dataElements.addAll(saveAllAndGetDataElements(children))
            }
            parents = children

        }

        dataElements
    }

    void batchSave(List<DataClass> dataClasses) {
        long start = System.currentTimeMillis()
        log.trace('Batch saving {} dataClasses', dataClasses.size())

        DataClass.saveAll(dataClasses)
        dataClasses.each {updateFacetsAfterInsertingCatalogueItem(it)}

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.trace('Batch save took {}', Utils.getTimeString(System.currentTimeMillis() - start))
    }

    private void removeAssociations(DataClass dataClass) {
        removeSemanticLinks(dataClass)
        removeReferenceTypes(dataClass)
        dataClass.breadcrumbTree.removeFromParent()
        dataClass.dataModel.removeFromDataClasses(dataClass)
        dataClass.dataClasses?.each {removeAssociations(it)}
    }

    private void removeSemanticLinks(DataClass dataClass) {
        List<SemanticLink> semanticLinks = semanticLinkService.findAllByCatalogueItemId(dataClass.id)
        semanticLinks.each {semanticLinkService.delete(it)}
    }

    private void removeReferenceTypes(DataClass dataClass) {
        List<ReferenceType> referenceTypes = []
        referenceTypes += dataClass.referenceTypes.findAll()
        referenceTypes.each {dataTypeService.delete(it)}
    }

    private void removeAllDataElementsWithNoLabel(DataClass dataClass) {
        List<DataElement> dataElements = []
        dataElements += dataClass.dataElements.findAll {!it.label}
        dataElements.each {dataElementService.delete(it)}
    }

    private void removeAllDataElementsWithSameLabel(DataClass dataClass) {

        if (dataClass.dataElements) {
            Map<String, List<DataElement>> identicalDataElements = dataClass.dataElements.groupBy {it.label}.findAll {it.value.size() > 1}
            identicalDataElements.each {label, dataElements ->
                for (int i = 1; i < dataElements.size(); i++) {
                    dataElementService.delete(dataElements[i])
                }
            }
        }
    }

    private void ensureChildDataClassesHaveUniqueNames(DataClass dataClass) {
        if (dataClass.dataClasses) {
            dataClass.dataClasses.groupBy {it.label}.findAll {it.value.size() > 1}.each {label, dataClasses ->
                dataClasses.eachWithIndex {DataClass child, int i ->
                    child.label = "${child.label}-$i"
                }
            }
        }
    }

    private void collapseReferenceTypes(DataClass dataClass) {
        if (!dataClass.referenceTypes || dataClass.referenceTypes.size() == 1) return
        DataModel dataModel = dataClass.dataModel
        Map<String, List<ReferenceType>> labelGroupedReferenceTypes = dataClass.referenceTypes.groupBy {it.label}

        labelGroupedReferenceTypes.findAll {it.value.size() > 1}.each {label, labelReferenceTypes ->
            Map<String, List<ReferenceType>> dmGrouped = labelReferenceTypes.groupBy {it.dataModel ? 'dataModel' : 'noDataModel'}

            // There will be only 1 datamodel owned type as we've already merged datamodel owned datatypes
            // If no datamodel owned, then we merge no datamodel and assign
            if (!dmGrouped.dataModel) {
                log.warn('No ReferenceType with label {} is owned by DataModel, merging existing and assigning to DataModel', label)
                ReferenceType keep = dataTypeService.mergeDataTypes(dmGrouped.dataModel)
                dataModel.addToDataTypes(keep)
            } else {
                // Merge all unowned reference types into the owned one
                dataTypeService.mergeDataTypes(dmGrouped.dataModel.first(), dmGrouped.noDataModel)
            }
        }
    }

    private void setCreatedBy(User creator, DataClass dataClass) {
        dataClass.createdBy = creator.emailAddress
        dataClass.dataClasses?.each {dc ->
            setCreatedBy(creator, dc)
        }

        dataClass.dataElements?.each {de ->
            de.createdBy = creator.emailAddress
        }
    }

    void checkImportedDataClassAssociations(User importingUser, DataModel dataModel, DataClass dataClass) {
        dataModel.addToDataClasses(dataClass)
        dataClass.createdBy = importingUser.emailAddress
        checkFacetsAfterImportingCatalogueItem(dataClass)
        if (dataClass.dataClasses) {
            dataClass.dataClasses.each {dc ->
                checkImportedDataClassAssociations(importingUser, dataModel, dc)
            }
        }
        if (dataClass.dataElements) {
            dataClass.dataElements.each {de ->
                de.createdBy = importingUser.emailAddress
                dataElementService.checkFacetsAfterImportingCatalogueItem(de)
            }
            dataElementService.matchUpDataTypes(dataModel, dataClass.dataElements)
        }
    }

    DataClass findSameLabelTree(DataModel dataModel, DataClass searchFor) {
        dataModel.dataClasses.find {hasSameLabelTree(it, searchFor)}
    }

    private boolean hasSameLabelTree(DataClass left, DataClass right) {
        if (left.label == right.label) {
            // Both children of datamodel so same items
            if (!left.parentDataClass && !right.parentDataClass) return true
            // Both have parents
            if (left.parentDataClass && right.parentDataClass) return hasSameLabelTree(left.parentDataClass, right.parentDataClass)
            // Anything else means not same parent tree
        }
        false
    }

    DataClass findByDataModelIdAndId(Serializable dataModelId, Serializable id) {
        DataClass.byDataModelId(dataModelId).idEq(id).find()
    }

    DataClass findByParentDataClassIdAndId(Serializable dataClassId, Serializable id) {
        DataClass.byParentDataClassId(dataClassId).idEq(id).find()
    }

    DataClass findWhereRootDataClassOfDataModelIdAndId(Serializable dataModelId, Serializable id) {
        DataClass.byRootDataClassOfDataModelId(dataModelId).idEq(id).find()
    }

    Boolean existsWhereRootDataClassOfDataModelIdAndId(Serializable dataModelId, Serializable id) {
        DataClass.byRootDataClassOfDataModelId(dataModelId).idEq(id).count() == 1
    }

    def findAllByParentDataClassId(Serializable dataClassId, Map paginate = [:]) {
        DataClass.withFilter(DataClass.byParentDataClassId(dataClassId), paginate).list(paginate)
    }

    def findAllWhereRootDataClassOfDataModelId(Serializable dataModelId, Map paginate = [:]) {
        DataClass.withFilter(DataClass.byRootDataClassOfDataModelId(dataModelId), paginate).list(paginate)
    }

    List<ModelItem> findAllContentOfDataClassId(Serializable dataClassId, Map paginate = [:]) {
        List<ModelItem> content = []
        content.addAll(DataClass.withFilter(DataClass.byChildOfDataClassId(dataClassId), paginate).list())
        content.addAll(dataElementService.findAllByDataClassId(dataClassId, paginate, [:]))
        content
    }

    def findAllByDataModelId(Serializable dataModelId, Map paginate = [:]) {
        DataClass.withFilter(DataClass.byDataModelId(dataModelId), paginate).list(paginate)
    }

    def findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(Serializable dataModelId, String searchTerm, Map paginate = [:]) {
        DataClass.byDataModelIdAndLabelIlikeOrDescriptionIlike(dataModelId, searchTerm).list(paginate)
    }

    private CatalogueItem findCommonParent(DataClass leftDataClass, DataClass rightDataClass) {

        // If they are they same then return
        if (leftDataClass.getParent().label == rightDataClass.label) return leftDataClass.getParent()

        // If either parent is datamodel, then common parent is the datamodel
        if (leftDataClass.getParent().instanceOf(DataModel)) return leftDataClass.getParent()
        if (rightDataClass.getParent().instanceOf(DataModel)) return rightDataClass.getParent()

        // Otherwise try to find the common parent of the parents
        findCommonParent(leftDataClass.getParent() as DataClass, rightDataClass.getParent() as DataClass)
    }

    private void moveDataClassToParent(DataClass dataClass, CatalogueItem parent) {
        if (parent.instanceOf(DataModel)) {
            dataClass.parentDataClass?.removeFromDataClasses(dataClass)
            parent.addToDataClasses(dataClass)
        } else if (parent.instanceOf(DataClass)) {
            dataClass.parentDataClass?.removeFromDataClasses(dataClass)
            parent.addToChildDataClasses(dataClass)
            parent.getDataModel().addToDataClasses(dataClass)
        }
    }

    private DataClass createDataClass(String label, String description, User createdBy, Integer minMultiplicity = 1,
                                      Integer maxMultiplicity = 1) {
        new DataClass(label: label, description: description, createdBy: createdBy, minMultiplicity: minMultiplicity,
                      maxMultiplicity: maxMultiplicity)
    }

    private DataClass findOrCreateDataClass(DataModel dataModel, String label, String description, User createdBy,
                                            Integer minMultiplicity = 1,
                                            Integer maxMultiplicity = 1) {
        DataClass dataClass = findDataClass(dataModel, label)
        if (!dataClass) {
            dataClass = createDataClass(label.trim(), description, createdBy, minMultiplicity, maxMultiplicity)
            dataModel.addToDataClasses(dataClass)
        }
        dataClass
    }

    private DataClass findOrCreateDataClass(DataClass parentDataClass, String label, String description, User createdBy,
                                            Integer minMultiplicity = 1, Integer maxMultiplicity = 1) {
        DataClass dataClass = findDataClass(parentDataClass, label)
        if (!dataClass) {
            dataClass = createDataClass(label.trim(), description, createdBy, minMultiplicity, maxMultiplicity)
            parentDataClass.addToChildDataClasses(dataClass)
            parentDataClass.dataModel.addToDataClasses(dataClass)
        }
        dataClass
    }

    private DataClass findOrCreateDataClassByPath(DataModel dataModel, List<String> pathLabels, String description, User createdBy,
                                                  Integer minMultiplicity = 1, Integer maxMultiplicity = 1) {
        if (pathLabels.size() == 1) {
            return findOrCreateDataClass(dataModel, pathLabels[0], description, createdBy, minMultiplicity, maxMultiplicity)
        }

        String parentLabel = pathLabels.remove(0)
        DataClass parent = findOrCreateDataClass(dataModel, parentLabel, '', createdBy)

        findOrCreateDataClassByPath(parent, pathLabels, description, createdBy, minMultiplicity, maxMultiplicity)
    }

    private DataClass findOrCreateDataClassByPath(DataClass parentDataClass, List<String> pathLabels, String description, User createdBy,
                                                  Integer minMultiplicity = 1, Integer maxMultiplicity = 1) {
        if (pathLabels.size() == 1) {
            return findOrCreateDataClass(parentDataClass, pathLabels[0], description, createdBy, minMultiplicity, maxMultiplicity)
        }

        String parentLabel = pathLabels.remove(0)
        DataClass parent = findOrCreateDataClass(parentDataClass, parentLabel, '', createdBy)
        findOrCreateDataClassByPath(parent, pathLabels, description, createdBy, minMultiplicity, maxMultiplicity)
    }

    DataClass findDataClass(DataModel dataModel, String label) {
        dataModel.dataClasses.find {!it.parentDataClass && it.label == label.trim()}
    }

    DataClass findDataClass(DataClass parentDataClass, String label) {
        parentDataClass.dataClasses.find {it.label == label.trim()}
    }

    DataClass findDataClassByPath(DataModel dataModel, List<String> pathLabels) {
        if (!pathLabels) return null
        if (pathLabels.size() == 1) {
            return findDataClass(dataModel, pathLabels[0])
        }

        String parentLabel = pathLabels.remove(0)
        DataClass parent = findDataClass(dataModel, parentLabel)
        if (!parent) throw new ApiBadRequestException('DCS01', "Cannot find DataClass for path [${parentLabel}|${pathLabels.join('|')}] " +
                                                               "as DataClass [${parentLabel}] does not exist")
        findDataClassByPath(parent, pathLabels)
    }

    DataClass findDataClassByPath(DataClass parentDataClass, List<String> pathLabels) {
        if (pathLabels.size() == 1) {
            return findDataClass(parentDataClass, pathLabels[0])
        }

        String parentLabel = pathLabels.remove(0)
        DataClass parent = findDataClass(parentDataClass, parentLabel)
        if (!parent) throw new ApiBadRequestException('DCS02', "Cannot find DataClass for path section [${parentLabel}|${pathLabels.join('|')}] " +
                                                               "as DataClass [${parentLabel}] does not exist")
        findDataClassByPath(parent, pathLabels)
    }

    DataClass copyDataClassMatchingAllReferenceTypes(DataModel copiedDataModel, DataClass original, User copier, Serializable parentDataClassId) {
        DataClass copiedDataClass = copyDataClass(copiedDataModel, original, copier, parentDataClassId)
        log.debug('Copied required DataClass, now checking for reference classes which haven\'t been matched or added')
        matchUpAndAddMissingReferenceTypeClasses(copiedDataModel, original.dataModel, copier)
        copiedDataClass

    }

    DataClass copyDataClass(DataModel copiedDataModel, DataClass original, User copier, Serializable parentDataClassId = null) {

        if (!original) throw new ApiInternalException('DCSXX', 'Cannot copy non-existent DataClass')

        DataClass copy = new DataClass(
            minMultiplicity: original.minMultiplicity,
            maxMultiplicity: original.maxMultiplicity
        )

        copy = copyCatalogueItemInformation(original, copy, copier)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        copiedDataModel.addToDataClasses(copy)

        if (parentDataClassId) {
            get(parentDataClassId).addToDataClasses(copy)
        }

        if (copy.validate()) copy.save(validate: false)
        else throw new ApiInvalidModelException('DC01', 'Copied DataClass is invalid', copy.errors, messageSource)

        copy.trackChanges()
        // Assuming DC is valid then we can start adding all its components

        original.referenceTypes.each {refType ->
            ReferenceType referenceType = copiedDataModel.findDataTypeByLabel(refType.label) as ReferenceType
            if (!referenceType) {
                referenceType = new ReferenceType(createdBy: copier.emailAddress, label: refType.label)
                copiedDataModel.addToDataTypes(referenceType)
            }
            copy.addToReferenceTypes(referenceType)
        }

        copy.dataClasses = []
        original.dataClasses.each {child ->
            copy.addToDataClasses(copyDataClass(copiedDataModel, child, copier))
        }
        copy.dataElements = []
        original.dataElements.each {element ->
            copy.addToDataElements(dataElementService.copyDataElement(copiedDataModel, element, copier))
        }

        copy

    }

    void matchUpAndAddMissingReferenceTypeClasses(DataModel copiedDataModel, DataModel originalDataModel, User copier) {
        Set<ReferenceType> emptyReferenceTypes = findAllEmptyReferenceTypes(copiedDataModel)
        if (!emptyReferenceTypes) return
        log.debug('Found {} empty reference types', emptyReferenceTypes.size())
        // Copy all the missing reference classes
        emptyReferenceTypes.each {rt ->
            ReferenceType ort = originalDataModel.findDataTypeByLabel(rt.label) as ReferenceType
            String originalDataClassPath = buildPath(ort.referenceClass)
            DataClass copiedDataClass = findDataClassByPath(copiedDataModel, originalDataClassPath.split(/\|/).toList())
            if (!copiedDataClass) {
                log.debug('Creating DataClass {} for referenceType {}', ort.referenceClass.label, rt.label)
                copiedDataClass = copyDataClass(copiedDataModel, ort.referenceClass, copier)
            }
            copiedDataClass.addToReferenceTypes(rt)
        }
        // Recursively loop until no empty reference classes
        matchUpAndAddMissingReferenceTypeClasses(copiedDataModel, originalDataModel, copier)
    }


    @Deprecated
    private Set<ReferenceType> getAllNestedReferenceTypes(DataClass dataClass) {
        Set<ReferenceType> referenceTypes = []
        referenceTypes.addAll(dataClass.referenceTypes ?: [])
        referenceTypes.addAll(dataClass.dataElements.dataType.findAll {it.instanceOf(ReferenceType)})
        dataClass.dataClasses.each {
            referenceTypes.addAll(getAllNestedReferenceTypes(it))
        }
        referenceTypes
    }


    private Set<ReferenceType> findAllEmptyReferenceTypes(DataModel dataModel) {
        dataModel.dataTypes.findAll {
            it.instanceOf(ReferenceType) && !(it as uk.ac.ox
                .softeng.maurodatamapper.datamodel.item.datatype.ReferenceType).referenceClass
        } as Set<ReferenceType>
    }


    String buildPath(DataClass dataClass) {
        if (!dataClass.parentDataClass) return dataClass.label
        "${buildPath(dataClass.parentDataClass)}|${dataClass.label}"
    }

    @Override
    Class<DataClass> getModelItemClass() {
        DataClass
    }

    @Override
    boolean hasTreeTypeModelItems(DataClass dataClass) {
        dataClass.dataClasses
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataClass dataClass) {
        DataClass.byParentDataClassId(dataClass.id).list()
    }

    @Override
    DataClass findByIdJoinClassifiers(UUID id) {
        DataClass.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        DataClass.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<DataClass> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        DataClass.byClassifierId(classifier.id).list().findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)}
    }

    @Override
    DataClass updateIndexForModelItemInParent(DataClass modelItem, CatalogueItem parent, int newIndex) {
        throw new ApiNotYetImplementedException('DCSXX', 'DataClass Ordering')
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        !domainType || domainType == DataClass.simpleName
    }

    @Override
    List<DataClass> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                               String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<DataClass> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = DataClass.luceneLabelSearch(DataClass, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }
}