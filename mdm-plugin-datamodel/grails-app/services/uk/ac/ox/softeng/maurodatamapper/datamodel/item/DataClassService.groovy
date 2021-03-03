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
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelExtend
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelExtendService
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImport
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImportService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.facet.SummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceTypeService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.orm.hibernate.proxy.HibernateProxyHandler
import org.springframework.context.MessageSource

@Slf4j
@Transactional
class DataClassService extends ModelItemService<DataClass> {

    DataModelService dataModelService
    DataElementService dataElementService
    DataTypeService dataTypeService
    MessageSource messageSource
    ModelExtendService modelExtendService 
    ModelImportService modelImportService    
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
    boolean handlesPathPrefix(String pathPrefix) {
        pathPrefix == "dc"
    }

    void delete(UUID id) {
        delete(get(id))
    }
    /**
     * DataClass allows the import of DataClass and DataElement
     */
    @Override
    List<Class> importsDomains() {
        [DataElement, DataClass]
    }    

    /**
     * Does the importedModelItem belong to a DataClass belonging to a DataModel which is either finalised
     * or in the same collection as the importing DataModel?
     *
     * @param importingDataClass The DataClass which is importing the importedModelItem
     * @param importedModelItem The ModelItem which is being imported into importingDataClass
     *
     * @return boolean Is this import allowed by domain specific rules?
     */
    @Override
    boolean isImportableByCatalogueItem(CatalogueItem importingDataClass, CatalogueItem importedModelItem) {
        //DataModel importingDataModel = importingDataClass.getModel()
        DataModel importedFromDataModel = importedModelItem.getModel()

        importedFromDataModel.finalised

        //TODO add OR importedFromModel is in the same collection as importingDataModel
    }

    /**
     * DataClass can extend DataClass
     */
    @Override
    List<Class> extendsDomains() {
        [DataClass]
    }    

    /**
     * Does the extendedDataClass meet domain specific rules for extension?
     *
     * @param extendingDataClass The DataClass which is extending the extendedDataClass
     * @param extendedDataClass The DataClass which is being extended by extendingDataClass
     *
     * @return boolean Is this extend allowed by domain specific rules?
     */
    @Override
    boolean isExtendableByCatalogueItem(DataClass extendingDataClass, DataClass extendedDataClass) {
        //1. The extended DataClass must directly belong to the same DataModel as the extending DataClass, or
        (extendingDataClass.dataModelId == extendedDataClass.dataModelId) ||
        //2. the extended DataClass is imported into the same DataModel as owns the extending DataClass, or
        (extendingDataClass.dataModel.modelImports.any {it.catalogueItemId == extendedDataClass.dataModelId}) ||
        //3. TODO the two DataClasses belong to the same collection, or
        //
        //4. the extended DataClass belongs to a DataModel which is finalised
        (extendedDataClass.dataModel.finalised)
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
        dataModel.trackChanges() // Discard any latent changes to the DataModel as we dont want them
        dataClass.delete(flush: flush)
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
            deleteAllFacetsByCatalogueItemIds(dataClassIds,
                                              'delete from datamodel.join_dataclass_to_facet where dataclass_id in :ids')

            log.trace('Removing {} DataClasses', dataClassIds.size())
            sessionFactory.currentSession
                .createSQLQuery('DELETE FROM datamodel.data_class WHERE data_model_id = :id')
                .setParameter('id', dataModelId)
                .executeUpdate()

            log.trace('DataClasses removed')
        }
    }

    void removeSummaryMetadataFromCatalogueItem(UUID catalogueItemId, SummaryMetadata summaryMetadata) {
        removeFacetFromDomain(catalogueItemId, summaryMetadata.id, 'summaryMetadata')
    }

    @Override
    void deleteAllFacetDataByCatalogueItemIds(List<UUID> catalogueItemIds) {
        super.deleteAllFacetDataByCatalogueItemIds(catalogueItemIds)
        summaryMetadataService.deleteAllByCatalogueItemIds(catalogueItemIds)
    }

    @Override
    DataClass updateFacetsAfterInsertingCatalogueItem(DataClass catalogueItem) {
        super.updateFacetsAfterInsertingCatalogueItem(catalogueItem)
        if (catalogueItem.summaryMetadata) {
            catalogueItem.summaryMetadata.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            SummaryMetadata.saveAll(catalogueItem.summaryMetadata)
        }
        if (catalogueItem.modelImports) {
            catalogueItem.modelImports.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            ModelImport.saveAll(catalogueItem.modelImports)
        }   
        if (catalogueItem.modelExtends) {
            catalogueItem.modelExtends.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            ModelExtend.saveAll(catalogueItem.modelExtends)
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

        List<Classifier> classifiers = dataClasses.collectMany { it.classifiers ?: [] } as List<Classifier>
        if (classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(classifiers)
        }

        Collection<DataClass> alreadySaved = dataClasses.findAll { it.ident() && it.isDirty() }
        Collection<DataClass> notSaved = dataClasses.findAll { !it.ident() }

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
            Collection<DataClass> parentIsSaved = notSaved.findAll { !it.parentDataClass || it.parentDataClass.id }
            log.trace('Ready to save on first run {}', parentIsSaved.size())
            while (parentIsSaved) {
                parentIsSaved.each { dc ->
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
                parentIsSaved = notSaved.findAll { it.parentDataClass && it.parentDataClass.id }
                log.trace('Ready to save on subsequent run {}', parentIsSaved.size())
            }
        }
        dataElements
    }

    void batchSave(List<DataClass> dataClasses) {
        long start = System.currentTimeMillis()
        log.trace('Performing batch save of {} DataClasses', dataClasses.size())

        DataClass.saveAll(dataClasses)
        dataClasses.each { updateFacetsAfterInsertingCatalogueItem(it) }

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.trace('Batch save took {}', Utils.getTimeString(System.currentTimeMillis() - start))
    }

    private void removeAssociations(DataClass dataClass) {
        removeSemanticLinks(dataClass)
        removeReferenceTypes(dataClass)
        dataClass.breadcrumbTree.removeFromParent()
        dataClass.dataModel.removeFromDataClasses(dataClass)
        dataClass.dataClasses?.each { removeAssociations(it) }
    }

    private void removeSemanticLinks(DataClass dataClass) {
        List<SemanticLink> semanticLinks = semanticLinkService.findAllByCatalogueItemId(dataClass.id)
        semanticLinks.each { semanticLinkService.delete(it) }
    }

    private void removeReferenceTypes(DataClass dataClass) {
        List<ReferenceType> referenceTypes = []
        referenceTypes += dataClass.referenceTypes.findAll()
        referenceTypes.each { dataTypeService.delete(it) }
    }

    private void removeAllDataElementsWithNoLabel(DataClass dataClass) {
        List<DataElement> dataElements = []
        dataElements += dataClass.dataElements.findAll { !it.label }
        dataElements.each { dataElementService.delete(it) }
    }

    private void removeAllDataElementsWithSameLabel(DataClass dataClass) {

        if (dataClass.dataElements) {
            Map<String, List<DataElement>> identicalDataElements = dataClass.dataElements.groupBy { it.label }.findAll { it.value.size() > 1 }
            identicalDataElements.each { label, dataElements ->
                for (int i = 1; i < dataElements.size(); i++) {
                    dataElementService.delete(dataElements[i])
                }
            }
        }
    }

    private void ensureChildDataClassesHaveUniqueNames(DataClass dataClass) {
        if (dataClass.dataClasses) {
            dataClass.dataClasses.groupBy { it.label }.findAll { it.value.size() > 1 }.each { label, dataClasses ->
                dataClasses.eachWithIndex { DataClass child, int i ->
                    child.label = "${child.label}-$i"
                }
            }
        }
    }

    private void collapseReferenceTypes(DataClass dataClass) {
        if (!dataClass.referenceTypes || dataClass.referenceTypes.size() == 1) return
        DataModel dataModel = dataClass.dataModel
        Map<String, List<ReferenceType>> labelGroupedReferenceTypes = dataClass.referenceTypes.groupBy { it.label }

        labelGroupedReferenceTypes.findAll { it.value.size() > 1 }.each { label, labelReferenceTypes ->
            Map<String, List<ReferenceType>> dmGrouped = labelReferenceTypes.groupBy { it.dataModel ? 'dataModel' : 'noDataModel' }

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
        dataClass.dataClasses?.each { dc ->
            setCreatedBy(creator, dc)
        }

        dataClass.dataElements?.each { de ->
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
            dataClass.dataClasses.each { dc ->
                checkImportedDataClassAssociations(importingUser, dataModel, dc, matchDataTypes)
            }
        }
        if (dataClass.dataElements) {
            dataClass.fullSortOfChildren(dataClass.dataElements)
            dataClass.dataElements.each { de ->
                de.createdBy = importingUser.emailAddress
                de.buildPath()
                dataElementService.checkFacetsAfterImportingCatalogueItem(de)
            }
            if (matchDataTypes) dataElementService.matchUpDataTypes(dataModel, dataClass.dataElements)
        }
    }

    DataClass findSameLabelTree(DataModel dataModel, DataClass searchFor) {
        dataModel.dataClasses.find { hasSameLabelTree(it, searchFor) }
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

    def findAllByDataModelIdAndParentDataClassId(UUID dataModelId, UUID dataClassId, Map paginate = [:], boolean includeImported = false, boolean includeExtends = false) {
        DataClass.withFilter(DataClass.byDataModelIdAndParentDataClassId(dataModelId, dataClassId, includeImported, includeExtends), paginate).list(paginate)
    }

    def findAllWhereRootDataClassOfDataModelId(UUID dataModelId, Map paginate = [:], boolean includeImported = false) {
        DataClass.withFilter(DataClass.byRootDataClassOfDataModelId(dataModelId, includeImported), paginate).list(paginate)
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

    def findAllByDataModelId(UUID dataModelId, Map paginate = [:], boolean includeImported = false) {
        DataClass.withFilter(DataClass.byDataModelId(dataModelId, includeImported), paginate).list(paginate)
    }

    def findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(UUID dataModelId, String searchTerm, Map paginate = [:], boolean includeImported = false) {
        DataClass.byDataModelIdAndLabelIlikeOrDescriptionIlike(dataModelId, searchTerm, includeImported).list(paginate)
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
        dataModel.dataClasses.find { !it.parentDataClass && it.label == label.trim() }
    }

    DataClass findDataClass(DataClass parentDataClass, String label) {
        parentDataClass.dataClasses.find { it.label == label.trim() }
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

    DataClass copyDataClassMatchingAllReferenceTypes(DataModel copiedDataModel, DataClass original, User copier, UserSecurityPolicyManager
        userSecurityPolicyManager, Serializable parentDataClassId) {
        DataClass copiedDataClass = copyDataClass(copiedDataModel, original, copier, userSecurityPolicyManager, parentDataClassId)
        log.debug('Copied required DataClass, now checking for reference classes which haven\'t been matched or added')
        matchUpAndAddMissingReferenceTypeClasses(copiedDataModel, original.dataModel, copier, userSecurityPolicyManager)
        copiedDataClass

    }

    @Override
    DataClass copy(Model copiedDataModel, DataClass original, UserSecurityPolicyManager userSecurityPolicyManager,
                   UUID parentDataClassId = null) {
        copyDataClass(copiedDataModel as DataModel, original, userSecurityPolicyManager.user, userSecurityPolicyManager, parentDataClassId)
    }

    DataClass copyDataClass(DataModel copiedDataModel, DataClass original, User copier,
                            UserSecurityPolicyManager userSecurityPolicyManager,
                            Serializable parentDataClassId = null, boolean copySummaryMetadata = false) {
        if (!original) throw new ApiInternalException('DCSXX', 'Cannot copy non-existent DataClass')

        DataClass copy = new DataClass(
            minMultiplicity: original.minMultiplicity,
            maxMultiplicity: original.maxMultiplicity
        )

        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager, copySummaryMetadata)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        copiedDataModel.addToDataClasses(copy)

        if (parentDataClassId) {
            get(parentDataClassId).addToDataClasses(copy)
        }

        if (!copy.validate()) //save(validate: false, copy) else
            throw new ApiInvalidModelException('DCS01', 'Copied DataClass is invalid', copy.errors, messageSource)

        original.referenceTypes.each { refType ->
            ReferenceType referenceType = copiedDataModel.referenceTypes.find { it.label == refType.label }
            if (!referenceType) {
                referenceType = new ReferenceType(createdBy: copier.emailAddress, label: refType.label)
                copiedDataModel.addToDataTypes(referenceType)
            }
            copy.addToReferenceTypes(referenceType)
        }

        copy.dataClasses = []
        original.dataClasses.each { child ->
            copy.addToDataClasses(copyDataClass(copiedDataModel, child, copier, userSecurityPolicyManager))
        }
        copy.dataElements = []
        original.dataElements.each { element ->
            copy.addToDataElements(dataElementService.copyDataElement(copiedDataModel, element, copier, userSecurityPolicyManager))
        }

        copy

    }

    @Override
    DataClass copyCatalogueItemInformation(DataClass original,
                                           DataClass copy,
                                           User copier,
                                           UserSecurityPolicyManager userSecurityPolicyManager,
                                           boolean copySummaryMetadata = false) {
        copy = super.copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager)
        if (copySummaryMetadata) {
            summaryMetadataService.findAllByCatalogueItemId(original.id).each {
                copy.addToSummaryMetadata(label: it.label, summaryMetadataType: it.summaryMetadataType, createdBy: copier.emailAddress)
            }
        }

        modelImportService.findAllByCatalogueItemId(original.id).each { 
            copy.addToModelImports(it.importedCatalogueItemDomainType,
                                   it.importedCatalogueItemId,
                                   copier) 
        }       

        modelExtendService.findAllByCatalogueItemId(original.id).each { 
            copy.addToModelExtends(it.extendedCatalogueItemDomainType,
                                   it.extendedCatalogueItemId,
                                   copier) 
        }                
        copy
    }

    void matchUpAndAddMissingReferenceTypeClasses(DataModel copiedDataModel, DataModel originalDataModel, User copier,
                                                  UserSecurityPolicyManager userSecurityPolicyManager) {
        Set<ReferenceType> emptyReferenceTypes = findAllEmptyReferenceTypes(copiedDataModel)
        if (!emptyReferenceTypes) return
        log.debug('Found {} empty reference types', emptyReferenceTypes.size())
        // Copy all the missing reference classes
        emptyReferenceTypes.each { rt ->
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
        referenceTypes.addAll(dataClass.dataElements.dataType.findAll { it.instanceOf(ReferenceType) })
        dataClass.dataClasses.each {
            referenceTypes.addAll(getAllNestedReferenceTypes(it))
        }
        referenceTypes
    }


    private Set<ReferenceType> findAllEmptyReferenceTypes(DataModel dataModel) {
        dataModel.referenceTypes.findAll { !(it as ReferenceType).referenceClass } as Set<ReferenceType>
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
    boolean hasTreeTypeModelItems(DataClass dataClass, boolean forDiff, boolean includeImported = false) {
        dataClass.dataClasses || (dataClass.dataElements && forDiff) || (dataClass.modelImports && includeImported)
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataClass dataClass, boolean forDiff = false, boolean includeImported = false) {
        (DataClass.byDataModelIdAndParentDataClassId(dataClass.dataModel.id, dataClass.id, includeImported).list() +
         (forDiff ? DataElement.byDataClassId(dataClass.id).list() : []) as List<ModelItem>)
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
        DataClass.byClassifierId(classifier.id).list().findAll { userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id) }
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
        List<SemanticLink> alreadyExistingLinks = semanticLinkService.findAllBySourceCatalogueItemIdInListAndTargetCatalogueItemIdInListAndLinkType(
            dataClasses*.id, fromDataClasses*.id, SemanticLinkType.IS_FROM)
        dataClasses.each { de ->
            fromDataClasses.each { fde ->
                // If no link already exists then add a new one
                if (!alreadyExistingLinks.any { it.catalogueItemId == de.id && it.targetCatalogueItemId == fde.id }) {
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
        List<SemanticLink> links = semanticLinkService.findAllBySourceCatalogueItemIdInListAndTargetCatalogueItemIdInListAndLinkType(
            dataClasses*.id, fromDataClasses*.id, SemanticLinkType.IS_FROM)
        semanticLinkService.deleteAll(links)
    }

    void setDataClassIsFromDataClass(DataClass source, DataClass target, User user) {
        source.addToSemanticLinks(linkType: SemanticLinkType.IS_FROM, createdBy: user.getEmailAddress(), targetCatalogueItem: target)
    }

    /**
     * Find a DataClass which is labeled with label and whose parent is parentCatalogueItem. The parentCatalogueItem
     * can be a DataModel or DataClass.
     * @param parentCatalogueItem The DataModel or DataClass which is the parent of the DataClass being sought
     * @param label The label of the DataClass being sought
     */
    @Override
    DataClass findByParentAndLabel(CatalogueItem parentCatalogueItem, String label) {
        findDataClass(parentCatalogueItem, label)
    }

    /**
     * Find a DataClass by label.
     * @param label
     * @return The found DataClass
     */
    DataClass findByLabel(String label) {
        DataClass.findByLabel(label)
    }

    /**
     * When a DataClass is imported into a DataClass or DataModel, we also want to import any DataTypes
     * belonging to the DataElements of the imported DataClass.
     *
     * @param currentUser The user doing the import
     * @param imported The resource that was imported
     *
     */
    @Override
    void additionalModelImports(User currentUser, ModelImport imported) {

        //The DataClass that was imported
        DataClass dataClass = imported.importedCatalogueItem
        
        dataClass.dataElements.each {dataElement ->
            DataType dataType = dataElement.dataType

            //The CatalogueItem (which will be a DataClass) into which the DataElement was imported
            CatalogueItem catalogueItem = imported.catalogueItem

            //The DataModel to which that DataClass belongs
            DataModel dataModel
            if (catalogueItem instanceof ModelItem) {
                dataModel = catalogueItem.getModel()
            } else {
                dataModel = catalogueItem
            }

            if (!dataModel.findDataTypeByLabelAndType(dataType.label, dataType.domainType)) {
                //Create a new ModelImport for the DataType into DataModel
                ModelImport modelImportDataType = new ModelImport(catalogueItem          : dataModel,
                                                                  importedCatalogueItem  : dataType,
                                                                  createdByUser          : currentUser)
        
                //Save the additional model import, indicating that this is an additional rather than
                //principal import and so should fail silently if it already exists.
                modelImportService.saveResource(currentUser, modelImportDataType, true) 
            }
        }
                                                 
    }
}
