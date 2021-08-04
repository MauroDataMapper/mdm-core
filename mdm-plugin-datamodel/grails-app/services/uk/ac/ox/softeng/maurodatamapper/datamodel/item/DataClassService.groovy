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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInvalidModelException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.traits.service.SummaryMetadataAwareService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.hibernate.StaleStateException
import org.springframework.context.MessageSource
import org.springframework.orm.hibernate5.HibernateOptimisticLockingFailureException

@Slf4j
@Transactional
class DataClassService extends ModelItemService<DataClass> implements SummaryMetadataAwareService {

    DataModelService dataModelService
    DataElementService dataElementService
    DataTypeService dataTypeService
    MessageSource messageSource
    SummaryMetadataService summaryMetadataService
    ReferenceTypeService referenceTypeService

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
    List<DataClass> getAll(Collection<UUID> ids) {
        DataClass.getAll(ids).findAll()
    }

    @Override
    DataClass save(Map args, DataClass domain) {
        // If not previously saved then allow a deep save and/or datatype save
        if (!domain.ident()) {
            if (args.deepSave) {
                saveDataClassHierarchy(domain)
            }

            // If ignore datatypes then skip this bit or DC already been saved as this is designed to handle full builds or copies
            if (args.saveDataTypes) {
                saveDataTypesUsedInDataClass(domain)
            }
        }
        super.save(args, domain)
    }

    void saveDataTypesUsedInDataClass(DataClass dataClass) {
        // Make sure all datatypes are saved
        Set<DataType> dataTypes = extractAllUsedNewOrDirtyDataTypes(dataClass)
        log.debug('{} new or dirty used datatypes inside dataclass', dataTypes.size())
        // Validation should have already been done
        dataTypes.each {it.skipValidation(true)}
        dataTypeService.saveAll(dataTypes, false)
    }

    Set<DataType> extractAllUsedNewOrDirtyDataTypes(DataClass dataClass) {
        Set<DataType> dataTypes = dataClass.dataElements.collect {it.dataType}.findAll {it.isDirty() || !it.ident()}.toSet()
        dataTypes.addAll(dataClass.dataClasses.collect {extractAllUsedNewOrDirtyDataTypes(it)}.flatten().toSet() as Collection<DataType>)
        dataTypes
    }

    /**
     * Save the dataclasses in the hierarchy of this dataclass in advance of saving the content of the DataClass.
     * This is necessary to handle reference types used by DataElements where the ReferenceClass is defined inside the DataClass
     * @param dataClass
     */
    void saveDataClassHierarchy(DataClass dataClass) {
        // If nothing in this dataclass then no need to save hierarchy
        if (!dataClass.dataElements && !dataClass.dataClasses) return

        // Preserve the content
        Set<DataClass> dataClasses = new HashSet<>(dataClass.dataClasses ?: [])
        dataClass.dataClasses?.clear()
        Set<DataElement> dataElements = new HashSet<>(dataClass.dataElements ?: [])
        dataClass.dataElements?.clear()
        Set<ReferenceType> referenceTypes = new HashSet<>(dataClass.referenceTypes ?: [])
        dataClass.referenceTypes?.clear()

        // Save the DC without flushing
        dataClass.save(flush: false, validate: false)

        // Recurse through the hierarchy
        dataClasses?.each {dc ->
            saveDataClassHierarchy(dc)
        }

        // Add the content back in
        dataClass.dataClasses = dataClasses
        dataClass.dataElements = dataElements
        dataClass.referenceTypes = referenceTypes
    }

    void delete(UUID id) {
        delete(get(id))
    }

    void delete(DataClass dataClass, boolean flush = false) {
        if (!dataClass) return
        DataModel dataModel = proxyHandler.unwrapIfProxy(dataClass.dataModel)
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
        try {
            // Discard any latent changes to the DataModel as we dont want them
            // But only if we're flushing otherwise we risk losing changes when this method is used from a DM context
            if (flush) dataModel.trackChanges()
            dataClass.delete(flush: false)
            // Use a proper session flush to prevent the exceptions below?
            if (flush) sessionFactory.currentSession.flush()
        } catch (HibernateOptimisticLockingFailureException | StaleStateException exception) {
            // updating the DM on a nested DC delete???
            log.error("We had another exception thrown: {}", exception.message)
        }
    }

    @Override
    void deleteAll(Collection<DataClass> dataClasses) {
        dataClasses.each {
            delete(it)
        }
    }

    void deleteAllByModelId(UUID dataModelId) {

        List<UUID> dataClassIds = DataClass.byDataModelId(dataModelId).id().list() as List<UUID>

        if (dataClassIds) {

            log.trace('Removing DataElements in {} DataClasses', dataClassIds.size())
            dataElementService.deleteAllByModelId(dataModelId)

            log.trace('Removing ReferenceTypes in {} DataClasses', dataClassIds.size())
            referenceTypeService.deleteAllByModelId(dataModelId)

            log.trace('Removing facets for {} DataClasses', dataClassIds.size())
            deleteAllFacetsByMultiFacetAwareIds(dataClassIds,
                                                'delete from datamodel.join_dataclass_to_facet where dataclass_id in :ids')

            log.trace('Removing {} DataClasses', dataClassIds.size())
            sessionFactory.currentSession
                .createSQLQuery('DELETE FROM datamodel.data_class WHERE data_model_id = :id')
                .setParameter('id', dataModelId)
                .executeUpdate()

            log.trace('DataClasses removed')
        }
    }

    @Override
    void deleteAllFacetDataByMultiFacetAwareIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByMultiFacetAwareIds(catalogueItemIds)
        summaryMetadataService.deleteAllByMultiFacetAwareItemIds(catalogueItemIds)
    }

    @Override
    DataClass updateFacetsAfterInsertingCatalogueItem(DataClass catalogueItem) {
        super.updateFacetsAfterInsertingCatalogueItem(catalogueItem)
        if (catalogueItem.summaryMetadata) {
            catalogueItem.summaryMetadata.each {
                if (!it.isDirty('multiFacetAwareItemId')) it.trackChanges()
                it.multiFacetAwareItemId = catalogueItem.getId()
            }
            SummaryMetadata.saveAll(catalogueItem.summaryMetadata)
        }

        catalogueItem
    }

    @Override
    DataClass checkFacetsAfterImportingCatalogueItem(DataClass catalogueItem) {
        super.checkFacetsAfterImportingCatalogueItem(catalogueItem)
        if (catalogueItem.summaryMetadata) {
            catalogueItem.summaryMetadata.each {sm ->
                sm.multiFacetAwareItemId = catalogueItem.id
                sm.createdBy = sm.createdBy ?: catalogueItem.createdBy
                sm.summaryMetadataReports.each {smr ->
                    smr.createdBy = catalogueItem.createdBy
                }
            }
        }
        catalogueItem
    }

    boolean isUnusedDataClass(DataClass dataClass) {
        if (dataClass.maxMultiplicity != null) return false
        if (dataClass.minMultiplicity != null) return false
        if (dataClass.referenceTypes) return false
        if (dataClass.dataClasses) return false
        true
    }

    Collection<DataElement> saveAllAndGetDataElements(Collection<DataClass> dataClasses) {

        List<Classifier> classifiers = dataClasses.collectMany {it.classifiers ?: []} as List<Classifier>
        if (classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(classifiers)
        }

        Collection<DataClass> alreadySaved = dataClasses.findAll {it.ident() && it.isDirty()}
        Collection<DataClass> notSaved = dataClasses.findAll {!it.ident()}

        Collection<DataElement> dataElements = []

        if (alreadySaved) {
            log.trace('Straight saving {} already saved DataClasses', alreadySaved.size())
            DataClass.saveAll(alreadySaved)
        }

        if (notSaved) {
            log.trace('Batch saving {} new DataClasses in batches of {}', notSaved.size(), DataClass.BATCH_SIZE)
            List batch = []
            int count = 0

            // Find all DCs which are either top level or have their parent DC already saved
            Collection<DataClass> parentIsSaved = notSaved.findAll {!it.parentDataClass || it.parentDataClass.id}
            log.trace('Ready to save on first run {}', parentIsSaved.size())
            while (parentIsSaved) {
                parentIsSaved.each {dc ->
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
                // Find all DCs which have a saved parent DC
                notSaved.removeAll(parentIsSaved)
                parentIsSaved = notSaved.findAll {it.parentDataClass && it.parentDataClass.id}
                log.trace('Ready to save on subsequent run {}', parentIsSaved.size())
            }
        }
        dataElements
    }

    private void removeAssociations(DataClass dataClass) {
        removeSemanticLinks(dataClass)
        removeReferenceTypes(dataClass)
        dataClass.breadcrumbTree.removeFromParent()
        dataClass.dataModel.removeFromDataClasses(dataClass)
        dataClass.dataClasses?.each {removeAssociations(it)}
    }

    private void removeSemanticLinks(DataClass dataClass) {
        List<SemanticLink> semanticLinks = semanticLinkService.findAllByMultiFacetAwareItemId(dataClass.id)
        semanticLinks.each {semanticLinkService.delete(it)}
    }

    private void removeReferenceTypes(DataClass dataClass) {
        List<ReferenceType> referenceTypes = new ArrayList<>(dataClass.referenceTypes.findAll())
        referenceTypes.each {dataTypeService.delete(it)}
    }

    private void removeAllDataElementsWithNoLabel(DataClass dataClass) {
        List<DataElement> dataElements = new ArrayList<>(dataClass.dataElements.findAll {!it.label})
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
            if (dmGrouped.dataModel) {
                // Merge all unowned reference types into the owned one
                dataTypeService.mergeDataTypes(dmGrouped.dataModel.first(), dmGrouped.noDataModel)
            } else {
                // If no datamodel owned, then we merge no datamodel and assign
                log.warn('No ReferenceType with label {} is owned by DataModel, merging existing and assigning to DataModel', label)
                ReferenceType keep = dataTypeService.mergeDataTypes(dmGrouped.dataModel)
                dataModel.addToDataTypes(keep)

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

    void checkImportedDataClassAssociations(User importingUser, DataModel dataModel, DataClass dataClass, boolean matchDataTypes = false) {
        dataModel.addToDataClasses(dataClass)
        dataClass.buildPath()
        dataClass.createdBy = importingUser.emailAddress
        checkFacetsAfterImportingCatalogueItem(dataClass)
        if (dataClass.dataClasses) {
            dataClass.fullSortOfChildren(dataClass.dataClasses)
            dataClass.dataClasses.each {dc ->
                checkImportedDataClassAssociations(importingUser, dataModel, dc, matchDataTypes)
            }
        }
        if (dataClass.dataElements) {
            dataClass.fullSortOfChildren(dataClass.dataElements)
            dataClass.dataElements.each {de ->
                de.createdBy = importingUser.emailAddress
                de.buildPath()
                dataElementService.checkFacetsAfterImportingCatalogueItem(de)
            }
            if (matchDataTypes) dataElementService.matchUpDataTypes(dataModel, dataClass.dataElements)
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

    DataClass findByDataModelIdAndId(UUID dataModelId, Serializable id) {
        DataClass.byDataModelId(dataModelId).idEq(id).find()
    }

    DataClass findByDataModelIdAndParentDataClassIdAndId(UUID dataModelId, UUID dataClassId, Serializable id) {
        DataClass.byDataModelIdAndParentDataClassId(dataModelId, dataClassId).idEq(id).find()
    }

    DataClass findByDataModelIdAndLabel(UUID dataModelId, String label) {
        DataClass.byDataModelId(dataModelId).eq('label', label).find()
    }

    DataClass findWhereRootDataClassOfDataModelIdAndId(UUID dataModelId, Serializable id) {
        DataClass.byRootDataClassOfDataModelId(dataModelId).idEq(id).find()
    }

    Boolean existsWhereRootDataClassOfDataModelIdAndId(UUID dataModelId, Serializable id) {
        DataClass.byRootDataClassOfDataModelId(dataModelId).idEq(id).count() == 1
    }

    def findAllByDataModelIdAndParentDataClassId(UUID dataModelId, UUID dataClassId, Map paginate = [:]) {
        DataClass.withFilter(DataClass.byDataModelIdAndParentDataClassId(dataModelId, dataClassId), paginate).
            list(paginate)
    }

    def findAllByDataModelIdAndParentDataClassIdIncludingImported(UUID dataModelId, UUID dataClassId, Map paginate = [:]) {
        DataClass.withFilter(DataClass.byDataModelIdAndParentDataClassIdIncludingImported(dataModelId, dataClassId), paginate).list(paginate)
    }

    def findAllWhereRootDataClassOfDataModelId(UUID dataModelId, Map paginate = [:]) {
        DataClass.withFilter(DataClass.byRootDataClassOfDataModelId(dataModelId), paginate).list(paginate)
    }

    def findAllWhereRootDataClassOfDataModelIdIncludingImported(UUID dataModelId, Map paginate = [:]) {
        DataClass.withFilter(DataClass.byRootDataClassOfDataModelIdIncludingImported(dataModelId), paginate).list(paginate)
    }

    List<ModelItem> findAllContentOfDataClassIdInDataModelId(UUID dataModelId, UUID dataClassId, Map paginate = [:]) {
        List<ModelItem> content = []
        content.addAll(DataClass.withFilter(DataClass.byDataModelIdAndChildOfDataClassId(dataModelId, dataClassId), paginate).list())
        content.addAll(dataElementService.findAllByDataClassId(dataClassId, paginate, [:]))
        content
    }

    Long countByDataModelId(UUID dataModelId) {
        DataClass.byDataModelId(dataModelId).count()
    }

    def findAllByDataModelId(UUID dataModelId, Map paginate = [:]) {
        DataClass.withFilter(DataClass.byDataModelId(dataModelId), paginate).list(paginate)
    }

    def findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(UUID dataModelId, String searchTerm, Map paginate = [:]) {
        DataClass.byDataModelIdAndLabelIlikeOrDescriptionIlike(dataModelId, searchTerm).list(paginate)
    }

    def findAllByDataModelIdIncludingImported(UUID dataModelId, Map paginate = [:]) {
        DataClass.withFilter(DataClass.byDataModelIdIncludingImported(dataModelId), paginate).list(paginate)
    }

    def findAllByDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(UUID dataModelId, String searchTerm, Map paginate = [:]) {
        DataClass.byDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(dataModelId, searchTerm).list(paginate)
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
        new DataClass(label: label, description: description, createdBy: createdBy.emailAddress, minMultiplicity: minMultiplicity,
                      maxMultiplicity: maxMultiplicity)
    }

    public DataClass findOrCreateDataClass(DataModel dataModel, String label, String description, User createdBy,
                                           Integer minMultiplicity = 1,
                                           Integer maxMultiplicity = 1) {
        DataClass dataClass = findDataClass(dataModel, label)
        if (!dataClass) {
            dataClass = createDataClass(label.trim(), description, createdBy, minMultiplicity, maxMultiplicity)
            dataModel.addToDataClasses(dataClass)
        }
        dataClass
    }

    public DataClass findOrCreateDataClass(DataClass parentDataClass, String label, String description, User createdBy,
                                           Integer minMultiplicity = 1, Integer maxMultiplicity = 1) {
        DataClass dataClass = findDataClass(parentDataClass, label)
        if (!dataClass) {
            dataClass = createDataClass(label.trim(), description, createdBy, minMultiplicity, maxMultiplicity)
            parentDataClass.addToDataClasses(dataClass)
            parentDataClass.dataModel.addToDataClasses(dataClass)
        }
        dataClass
    }

    DataClass findOrCreateDataClassByPath(DataModel dataModel, List<String> pathLabels, String description, User createdBy,
                                          Integer minMultiplicity = 1, Integer maxMultiplicity = 1) {
        if (pathLabels.size() == 1) {
            return findOrCreateDataClass(dataModel, pathLabels[0], description, createdBy, minMultiplicity, maxMultiplicity)
        }

        String parentLabel = pathLabels.remove(0)
        DataClass parent = findOrCreateDataClass(dataModel, parentLabel, '', createdBy)

        findOrCreateDataClassByPath(parent, pathLabels, description, createdBy, minMultiplicity, maxMultiplicity)
    }

    DataClass findOrCreateDataClassByPath(DataClass parentDataClass, List<String> pathLabels, String description, User createdBy,
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

    DataClass copyDataClassMatchingAllReferenceTypes(DataModel copiedDataModel, DataClass original, User copier,
                                                     UserSecurityPolicyManager userSecurityPolicyManager, UUID parentDataClassId,
                                                     CopyInformation copyInformation = null) {
        DataClass copiedDataClass = copyDataClass(copiedDataModel, original, copier,
                                                  userSecurityPolicyManager,
                                                  parentDataClassId ? get(parentDataClassId) : null,
                                                  false,
                                                  copyInformation)
        log.debug('Copied required DataClass, now checking for reference classes which haven\'t been matched or added')
        matchUpAndAddMissingReferenceTypeClasses(copiedDataModel, original.dataModel, copier, userSecurityPolicyManager)
        copiedDataClass

    }

    @Override
    DataClass copy(Model copiedDataModel, DataClass original, UUID parentDataClassId, UserSecurityPolicyManager userSecurityPolicyManager) {
        copy(copiedDataModel as DataModel, original, parentDataClassId ? get(parentDataClassId) : null, userSecurityPolicyManager)
    }

    @Override
    DataClass copy(Model copiedDataModel, DataClass original, CatalogueItem parentDataClass, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyDataClass(copiedDataModel as DataModel, original, userSecurityPolicyManager.user, userSecurityPolicyManager,
                      parentDataClass as DataClass,
                      false, null)
    }

    DataClass copyDataClass(DataModel copiedDataModel, DataClass original, User copier, UserSecurityPolicyManager userSecurityPolicyManager) {
        copyDataClass(copiedDataModel, original, copier, userSecurityPolicyManager, null, false, null)
    }

    DataClass copyDataClass(DataModel copiedDataModel, DataClass original, User copier,
                            UserSecurityPolicyManager userSecurityPolicyManager,
                            DataClass parentDataClass,
                            boolean copySummaryMetadata,
                            CopyInformation copyInformation) {

        if (!original) throw new ApiInternalException('DCSXX', 'Cannot copy non-existent DataClass')

        DataClass copy = new DataClass(
            minMultiplicity: original.minMultiplicity,
            maxMultiplicity: original.maxMultiplicity
        )

        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, copySummaryMetadata, copyInformation)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        copiedDataModel.addToDataClasses(copy)

        if (parentDataClass) {
            parentDataClass.addToDataClasses(copy)
        }

        if (!copy.validate()) //save(validate: false, copy) else
            throw new ApiInvalidModelException('DCS01', 'Copied DataClass is invalid', copy.errors, messageSource)

        original.referenceTypes.each {refType ->
            ReferenceType referenceType = copiedDataModel.referenceTypes.find {it.label == refType.label}
            if (!referenceType) {
                referenceType = new ReferenceType(createdBy: copier.emailAddress, label: refType.label)
                copiedDataModel.addToDataTypes(referenceType)
            }
            copy.addToReferenceTypes(referenceType)
        }

        copy.dataClasses = []
        original.dataClasses.each {child ->
            copy.addToDataClasses(copyDataClass(copiedDataModel, child, copier, userSecurityPolicyManager))
        }
        copy.dataElements = []
        original.dataElements.each {element ->
            copy.addToDataElements(dataElementService.copyDataElement(copiedDataModel, element, copier, userSecurityPolicyManager))
        }

        copy

    }


    DataClass copyCatalogueItemInformation(DataClass original,
                                           DataClass copy,
                                           User copier,
                                           UserSecurityPolicyManager userSecurityPolicyManager,
                                           boolean copySummaryMetadata, CopyInformation copyInformation) {

        copy = super.copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, copyInformation)
        if (copySummaryMetadata) {
            summaryMetadataService.findAllByMultiFacetAwareItemId(original.id).each {
                copy.addToSummaryMetadata(label: it.label, summaryMetadataType: it.summaryMetadataType, createdBy: copier.emailAddress)
            }
        }
        copy
    }

    @Override
    DataClass copyCatalogueItemInformation(DataClass original,
                                           DataClass copy,
                                           User copier,
                                           UserSecurityPolicyManager userSecurityPolicyManager, CopyInformation copyInformation = null) {
        copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, false, copyInformation)
    }

    void matchUpAndAddMissingReferenceTypeClasses(DataModel copiedDataModel, DataModel originalDataModel, User copier,
                                                  UserSecurityPolicyManager userSecurityPolicyManager) {
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
                copiedDataClass = copyDataClass(copiedDataModel, ort.referenceClass, copier, userSecurityPolicyManager)
            }
            copiedDataClass.addToReferenceTypes(rt)
        }
        // Recursively loop until no empty reference classes
        matchUpAndAddMissingReferenceTypeClasses(copiedDataModel, originalDataModel, copier, userSecurityPolicyManager)
    }


    @Deprecated(forRemoval = true)
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
        dataModel.referenceTypes.findAll {!(it as ReferenceType).referenceClass} as Set<ReferenceType>
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
    boolean hasTreeTypeModelItems(DataClass dataClass, boolean fullTreeRender) {
        dataClass.dataClasses || (dataClass.dataElements && fullTreeRender)
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataClass dataClass, boolean fullTreeRender = false) {
        (DataClass.byDataModelIdAndParentDataClassId(dataClass.dataModel.id, dataClass.id).list() +
         (fullTreeRender ? DataElement.byDataClassId(dataClass.id).list() : []) as List<ModelItem>)
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

    void addDataClassIsFromDataClasses(DataClass dataClass, Collection<DataClass> fromDataClasses, User user) {
        addDataClassesAreFromDataClasses([dataClass], fromDataClasses, user)
    }

    void addDataClassesAreFromDataClass(Collection<DataClass> dataClasses, DataClass fromDataClass, User user) {
        addDataClassesAreFromDataClasses(dataClasses, [fromDataClass], user)
    }

    void addDataClassesAreFromDataClasses(Collection<DataClass> dataClasses, Collection<DataClass> fromDataClasses, User user) {
        if (!dataClasses || !fromDataClasses) throw new ApiInternalException('DCSXX', 'No DataClasses or FromDataClasses exist to create links')
        List<SemanticLink> alreadyExistingLinks =
            semanticLinkService.findAllBySourceMultiFacetAwareItemIdInListAndTargetMultiFacetAwareItemIdInListAndLinkType(
                dataClasses*.id, fromDataClasses*.id, SemanticLinkType.IS_FROM)
        dataClasses.each {de ->
            fromDataClasses.each {fde ->
                // If no link already exists then add a new one
                if (!alreadyExistingLinks.any {it.multiFacetAwareItemId == de.id && it.targetMultiFacetAwareItemId == fde.id}) {
                    setDataClassIsFromDataClass(de, fde, user)
                }
            }
        }
    }

    void removeDataClassIsFromDataClasses(DataClass dataClass, Collection<DataClass> fromDataClasses) {
        removeDataClassesAreFromDataClasses([dataClass], fromDataClasses)
    }

    void removeDataClassesAreFromDataClass(Collection<DataClass> dataClasses, DataClass fromDataClass) {
        removeDataClassesAreFromDataClasses(dataClasses, [fromDataClass])
    }

    void removeDataClassesAreFromDataClasses(Collection<DataClass> dataClasses, Collection<DataClass> fromDataClasses) {
        if (!dataClasses || !fromDataClasses) throw new ApiInternalException('DCSXX', 'No DataClasses or FromDataClasses exist to remove links')
        List<SemanticLink> links = semanticLinkService.findAllBySourceMultiFacetAwareItemIdInListAndTargetMultiFacetAwareItemIdInListAndLinkType(
            dataClasses*.id, fromDataClasses*.id, SemanticLinkType.IS_FROM)
        semanticLinkService.deleteAll(links)
    }

    void setDataClassIsFromDataClass(DataClass source, DataClass target, User user) {
        source.addToSemanticLinks(linkType: SemanticLinkType.IS_FROM, createdBy: user.getEmailAddress(), targetMultiFacetAwareItem: target)
    }

    /**
     * Find a DataClass which is labeled with label and whose parent is parentCatalogueItem. The parentCatalogueItem
     * can be a DataModel or DataClass.
     * @param parentCatalogueItem The DataModel or DataClass which is the parent of the DataClass being sought
     * @param label The label of the DataClass being sought
     */
    @Override
    DataClass findByParentIdAndLabel(UUID parentId, String label) {
        DataClass dataClass = findByDataModelIdAndLabel(parentId, label)
        if (!dataClass) {
            dataClass = DataClass.byParentDataClassId(parentId).eq('label', label).get()
        }
        dataClass
    }

    /**
     * Find a DataClass by label.
     * @param label
     * @return The found DataClass
     */
    DataClass findByLabel(String label) {
        DataClass.findByLabel(label)
    }

    @Override
    List<DataClass> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination) {
        DataClass.byMetadataNamespaceAndKey(namespace, key).list(pagination)
    }

    @Override
    List<DataClass> findAllByMetadataNamespace(String namespace, Map pagination) {
        DataClass.byMetadataNamespace(namespace).list(pagination)
    }

    boolean isExtendableDataClassInSameModelOrInFinalisedModel(DataClass extendableDataClass, DataClass extendingDataClass) {
        isModelItemInSameModelOrInFinalisedModel(extendableDataClass, extendingDataClass)
    }

    boolean isDataClassBeingUsedAsExtension(DataClass dataClass) {
        DataClass.byExtendedDataClassId(dataClass.id).count()
    }

    boolean isDataClassBeingUsedAsImport(DataClass dataClass) {
        DataClass.byImportedDataClassId(dataClass.id).count() || DataModel.byImportedDataClassId(dataClass.id).count()
    }
}
