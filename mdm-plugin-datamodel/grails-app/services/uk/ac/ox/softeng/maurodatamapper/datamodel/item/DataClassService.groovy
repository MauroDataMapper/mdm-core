/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
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
import uk.ac.ox.softeng.maurodatamapper.gorm.HQLPagedResultList
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.hibernate.Session
import org.springframework.context.MessageSource

import static org.grails.orm.hibernate.cfg.GrailsHibernateUtil.ARGUMENT_SORT

@Slf4j
@Transactional
class DataClassService extends ModelItemService<DataClass> implements SummaryMetadataAwareService {

    DataModelService dataModelService
    DataElementService dataElementService
    DataTypeService dataTypeService
    MessageSource messageSource
    SummaryMetadataService summaryMetadataService
    ReferenceTypeService referenceTypeService

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
    DataClass validate(DataClass dataClass) {
        dataClass.validate()
        dataClass
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
        if (args.deepSave) {
            this.updateDataClassHierarchyAfterInsert(args, domain)
        }

        super.save(args, domain)
    }

    void updateDataClassHierarchyAfterInsert(Map args, DataClass dataClass) {
        if (dataClass.dataElements) {
            dataClass.dataElements.each {de ->
                dataElementService.save(de)
            }
        }
        if (dataClass.dataClasses) {
            dataClass.dataClasses.each {dc ->
                updateDataClassHierarchyAfterInsert(args, dc)
            }
        }
        // Make sure imported elements are correctly associated
        updateImportedElements([dataClass])
    }

    Collection<DataType> saveDataTypesUsedInDataClass(DataClass dataClass) {
        // Make sure all datatypes are saved
        Set<DataType> dataTypes = extractAllUsedNewOrDirtyDataTypes(dataClass)
        log.debug('{} new or dirty used datatypes inside dataclass', dataTypes.size())
        // Validation should have already been done
        dataTypes.each {it.skipValidation(true)}
        dataTypeService.saveAll(dataTypes, false) as Collection<DataType>
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
        DataModel dataModel = dataClass.dataModel
        dataModel.merge()
        dataModel.lock()
        if (dataClass.parentDataClass) {
            DataClass parent = dataClass.parentDataClass
            parent.removeFromDataClasses(dataClass)
        }
        removeAssociations(dataClass)
        List<DataElement> dataElements = dataElementService.findAllByDataClass(dataClass)
        dataElementService.deleteAll(dataElements)
        dataClass.dataElements = []
        try {
            dataClass.delete(flush: flush)
        } catch (Exception exception) {
            throw new ApiInternalException('DCSXX', 'Failed to delete the DataClass', exception)
        }
    }

    @Override
    void deleteAll(Collection<DataClass> dataClasses) {
        dataClasses.each {
            delete(it)
        }
    }

    @Override
    void deleteAllByModelIds(Set<UUID> dataModelIds) {

        List<UUID> dataClassIds = DataClass.byDataModelIdInList(dataModelIds).id().list() as List<UUID>

        if (dataClassIds) {

            log.trace('Removing DataElements in {} DataClasses', dataClassIds.size())
            dataElementService.deleteAllByModelIds(dataModelIds)

            log.trace('Removing ReferenceTypes in {} DataClasses', dataClassIds.size())
            referenceTypeService.deleteAllByModelIds(dataModelIds)

            log.trace('Removing facets for {} DataClasses', dataClassIds.size())
            deleteAllFacetsByMultiFacetAwareIds(dataClassIds,
                                                'delete from datamodel.join_dataclass_to_facet where dataclass_id in :ids')

            log.trace('Removing {} DataClasses', dataClassIds.size())
            sessionFactory.currentSession
                .createSQLQuery('DELETE FROM datamodel.data_class WHERE data_model_id IN :ids')
                .setParameter('ids', dataModelIds)
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

    Collection<DataElement> saveAll(Collection<DataClass> dataClasses, Integer maxBatchSize, boolean batching) {
        if (!dataClasses) return []

        List<Classifier> classifiers = dataClasses.collectMany {it.classifiers ?: []} as List<Classifier>
        if (classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(classifiers)
        }

        Collection<DataClass> alreadySaved = dataClasses.findAll {it.ident() && it.isDirty()}
        Collection<DataClass> notSaved = dataClasses.findAll {!it.ident()}

        Map<String, Object> gatheredContentsMap = [:]

        if (alreadySaved) {
            log.trace('Straight saving {} already saved DataClasses', alreadySaved.size())
            DataClass.saveAll(alreadySaved)
        }

        if (notSaved) {
            // Determine the ideal batch size and then use the smaller of the 2 values of the requested batch size and the ideal one
            int idealBatchSize = determineIdealBatchSize(notSaved)
            int batchSizeToUse = Math.min(maxBatchSize, idealBatchSize)
            log.debug('Batch saving {} new {} in batches of {}', notSaved.size(), getDomainClass().simpleName, batchSizeToUse)
            List batch = []
            int count = 0

            // Find all DCs which are either top level or have their parent DC already saved
            Collection<DataClass> parentIsSaved = notSaved.findAll {!it.parentDataClass || it.parentDataClass.id}
            log.trace('Ready to save on first run {}', parentIsSaved.size())
            while (parentIsSaved) {
                parentIsSaved.each {dc ->

                    gatherContents(gatheredContentsMap, dc)

                    batch << dc
                    count++
                    if (count % batchSizeToUse == 0) {
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
        returnGatheredContents(gatheredContentsMap)
    }

    @Override
    void batchSave(List<DataClass> modelItems) {
        super.batchSave(modelItems)
        updateImportedElements(modelItems)
        Session currentSession = sessionFactory.currentSession
        currentSession.flush()
        currentSession.clear()
    }

    void updateImportedElements(List<DataClass> modelItems) {
        // Make sure all the opposing sides
        modelItems.each {dc ->
            if (dc.importedDataClasses) {
                dc.importedDataClasses.each {idc ->
                    idc.attach()
                    idc.addToImportingDataClasses(dc)
                    idc.save(validate: false)
                }
                updateImportRelevantMetadata(dc, dc.importedDataClasses)
            }
            if (dc.importedDataElements) {
                dc.importedDataElements.each {ide ->
                    ide.attach()
                    ide.addToImportingDataClasses(dc)
                    ide.save(validate: false)
                }
                updateImportRelevantMetadata(dc, dc.importedDataElements)
            }
        }
    }

    void updateImportRelevantMetadata(DataClass savedDataClass, Collection<ModelItem> importedElements) {
        // Find all MD which is attached to the imported elements with a key that needs replacing for the path of the DC
        // Update the replace required path to the id
        List<Metadata> importRelevantMetadata = metadataService.findAllByMultiFacetAwareItemIdInListAndNamespaceLike(importedElements*.id, "%REPLACE_${savedDataClass.path}%")
        importRelevantMetadata.each {md ->
            md.namespace = md.namespace.replace("REPLACE_${savedDataClass.path}".toString(), savedDataClass.id.toString())
            md.value = md.value.replace("REPLACE_${savedDataClass.path}".toString(), savedDataClass.id.toString())
            metadataService.save(md)
        }
    }

    @Override
    void preBatchSaveHandling(List<DataClass> modelItems) {
        modelItems.each {dc ->
            dc.dataClasses?.clear()
            dc.dataElements?.clear()
            dc.referenceTypes?.clear()
            dc.parentDataClass?.attach()
        }
    }

    @Override
    void gatherContents(Map<String, Object> gatheredContents, DataClass modelItem) {
        List<DataElement> dataElements = gatheredContents.getOrDefault('dataElements', [])
        dataElements.addAll(modelItem.dataElements ?: [])
        gatheredContents.dataElements = dataElements
    }

    @Override
    List<DataElement> returnGatheredContents(Map<String, Object> gatheredContents) {
        gatheredContents.getOrDefault('dataElements', []) as List<DataElement>
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
        dataClass.checkPath()
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
                de.checkPath()
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

    boolean existsByDataModelIdAndId(UUID dataModelId, Serializable id) {
        DataClass.byDataModelId(dataModelId).idEq(id).count() == 1
    }

    DataClass findByDataModelIdAndId(UUID dataModelId, Serializable id) {
        DataClass.byDataModelId(dataModelId).idEq(id).find()
    }

    DataClass findByDataModelIdAndParentDataClassIdAndId(UUID dataModelId, UUID dataClassId, Serializable id) {
        DataClass.byDataModelIdAndParentDataClassId(dataModelId, dataClassId).idEq(id).find()
    }

    DataClass findByDataModelIdAndParentDataClassIdAndLabel(UUID dataModelId, UUID dataClassId, String label) {
        DataClass.byDataModelIdAndParentDataClassId(dataModelId, dataClassId).eq('label', label).find()
    }

    DataClass findByDataModelIdAndLabel(UUID dataModelId, String label) {
        DataClass.byDataModelId(dataModelId).eq('label', label).find()
    }

    DataClass findByDataModelIdAndNullParentDataClassAndLabel(UUID dataModelId, String label) {
        DataClass.byDataModelId(dataModelId).isNull('parentDataClass').eq('label', label).find()
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

    def findAllWhereRootDataClassOfDataModelId(UUID dataModelId, Map paginate = [:]) {
        DataClass.withFilter(DataClass.byRootDataClassOfDataModelId(dataModelId), paginate).list(paginate)
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

    List<DataClass> findAllByImportingDataModelId(UUID dataModelId) {
        DataClass.byImportingDataModelId(dataModelId).list()
    }

    List<DataClass> findAllByImportingDataClassId(UUID dataClassId) {
        DataClass.byImportingDataClassId(dataClassId).list()
    }

    List<DataClass> findAllByExtendedDataClassId(UUID dataClassId) {
        DataClass.byExtendedDataClassId(dataClassId).list()
    }

    List<DataClass> findAllByExtendingDataClassId(UUID dataClassId) {
        DataClass.byExtendingDataClassId(dataClassId).list()
    }

    List<DataClass> findAllByImportingDataClassIds(List<UUID> dataClassIds) {
        if (!dataClassIds) return []
        DataClass.byImportingDataClassIdInList(dataClassIds).list()
    }

    def findAllByDataModelIdAndLabelIlikeOrDescriptionIlike(UUID dataModelId, String searchTerm, Map paginate = [:]) {
        DataClass.byDataModelIdAndLabelIlikeOrDescriptionIlike(dataModelId, searchTerm).list(paginate)
    }

    def findAllWhereRootDataClassOfDataModelIdIncludingImported(UUID dataModelId, Map paginate = [:]) {
        findAllWhereRootDataClassOfDataModelIdIncludingImported(dataModelId, paginate, paginate)
    }

    def findAllWhereRootDataClassOfDataModelIdIncludingImported(UUID dataModelId, Map filters, Map pagination) {
        Map<String, Object> queryParams = [dataModelId: dataModelId]
        findAllDataClassesByHQLQuery('''
FROM DataClass dc
LEFT JOIN dc.importingDataModels idm
WHERE
(
    (dc.dataModel.id = :dataModelId AND dc.parentDataClass.id is null)
    OR
    idm.id = :dataModelId
)''', queryParams, filters, pagination)
    }

    HQLPagedResultList findAllByDataModelIdIncludingImported(UUID dataModelId, Map paginate = [:]) {
        findAllByDataModelIdIncludingImported dataModelId, paginate, paginate
    }

    HQLPagedResultList findAllByDataModelIdIncludingImported(UUID dataModelId, Map filters, Map pagination) {
        Map<String, Object> queryParams = [dataModelId: dataModelId]
        findAllDataClassesByHQLQuery('''
FROM DataClass dc
LEFT JOIN dc.importingDataModels idm
WHERE
(
    dc.dataModel.id = :dataModelId
    OR
    idm.id = :dataModelId
)''', queryParams, filters, pagination)
    }

    HQLPagedResultList findAllByDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(UUID dataModelId, String searchTerm, Map paginate = [:]) {
        findAllByDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(dataModelId, searchTerm, paginate, paginate)
    }

    HQLPagedResultList findAllByDataModelIdAndLabelIlikeOrDescriptionIlikeIncludingImported(UUID dataModelId, String searchTerm, Map filters, Map pagination) {

        Map<String, Object> queryParams = [dataModelId: dataModelId,
                                           searchTerm : "%${searchTerm}%"]

        findAllDataClassesByHQLQuery('''
FROM DataClass dc
LEFT JOIN dc.importingDataModels idm
WHERE
(
    dc.dataModel.id = :dataModelId
    OR
    idm.id = :dataModelId
)
AND (
    lower(dc.label) like lower(:searchTerm)
    OR
    lower(dc.description) like lower(:searchTerm)
)''', queryParams, filters, pagination)
    }

    HQLPagedResultList findAllByDataModelIdAndParentDataClassIdIncludingImported(UUID dataModelId, UUID dataClassId, Map paginate = [:]) {
        findAllByDataModelIdAndParentDataClassIdIncludingImported(dataModelId, dataClassId, paginate, paginate)
    }

    HQLPagedResultList findAllByDataModelIdAndParentDataClassIdIncludingImported(UUID dataModelId, UUID dataClassId, Map filters, Map pagination) {

        Map<String, Object> queryParams = [dataClassId: dataClassId, dataModelId: dataModelId]
        findAllDataClassesByHQLQuery('''
FROM DataClass dc
LEFT JOIN dc.importingDataClasses idc
WHERE
(
    (dc.dataModel.id = :dataModelId AND dc.parentDataClass.id = :dataClassId)
    OR
    idc.id = :dataClassId
)''', queryParams, filters, pagination)
    }

    private HQLPagedResultList<DataClass> findAllDataClassesByHQLQuery(String baseQuery, Map<String, Object> queryParams, Map filters, Map pagination) {
        queryParams.putAll(extractFiltersAsHQLParameters(filters))

        String filteredQuery = applyHQLFilters(baseQuery, 'dc', filters)
        // Cannot sort DCs including imported using idx combined with any other field
        String sortedQuery = applyHQLSort(filteredQuery, 'dc', pagination[ARGUMENT_SORT] ?: ['label': 'asc'], pagination, true)

        new HQLPagedResultList<DataClass>(DataClass)
            .list("SELECT DISTINCT dc ${sortedQuery}".toString())
            .count("SELECT COUNT(DISTINCT dc.id) ${filteredQuery}".toString())
            .queryParams(queryParams)
            .paginate(pagination)
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

    void moveDataClassToParent(DataClass dataClass, CatalogueItem parent) {
        dataClass.parentDataClass?.removeFromDataClasses(dataClass)
        parent.addToDataClasses(dataClass)
    }

    DataClass createDataClass(String label, String description, User createdBy, Integer minMultiplicity = 1,
                              Integer maxMultiplicity = 1) {
        new DataClass(label: label, description: description, createdBy: createdBy.emailAddress, minMultiplicity: minMultiplicity,
                      maxMultiplicity: maxMultiplicity)
    }

    DataClass findOrCreateDataClass(DataModel dataModel, String label, String description, User createdBy,
                                    Integer minMultiplicity = 1,
                                    Integer maxMultiplicity = 1) {
        DataClass dataClass = findDataClass(dataModel, label)
        if (!dataClass) {
            dataClass = createDataClass(label.trim(), description, createdBy, minMultiplicity, maxMultiplicity)
            dataModel.addToDataClasses(dataClass)
        }
        dataClass
    }

    DataClass findOrCreateDataClass(DataClass parentDataClass, String label, String description, User createdBy,
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

    /**
     * pathLabels represents a hierarchy of Data Classes in sourceDataModel. Ensure that the same hierarchy exists in
     * targetDataModel, by copying classes from sourceDataModel where necessary.
     * @param sourceDataModel
     * @param targetDataModel
     * @param pathLabels
     * @return
     */
    DataClass findOrCopyDataClassHierarchyForReferenceTypeByPath(User user,
                                                                 UserSecurityPolicyManager userSecurityPolicyManager,
                                                                 DataModel sourceDataModel,
                                                                 DataModel targetDataModel,
                                                                 List<String> pathLabels,
                                                                 DataClass parentDataClassInSource,
                                                                 DataClass parentDataClassInTarget) {
        DataClass sourceDataClass
        DataClass targetDataClass

        if (!pathLabels) return null

        // If no parent class then assume we are looking in the data model
        if (parentDataClassInSource) {
            sourceDataClass = findDataClass(parentDataClassInSource, pathLabels[0])
            targetDataClass = findDataClass(parentDataClassInTarget, pathLabels[0])
        } else {
            sourceDataClass = findDataClass(sourceDataModel, pathLabels[0])
            targetDataClass = findDataClass(targetDataModel, pathLabels[0])
        }

        if (!targetDataClass) {
            targetDataClass = new DataClass(
                minMultiplicity: sourceDataClass.minMultiplicity,
                maxMultiplicity: sourceDataClass.maxMultiplicity
            )

            targetDataClass = copyModelItemInformation(sourceDataClass, targetDataClass, user, userSecurityPolicyManager, false, null)

            targetDataModel.addToDataClasses(targetDataClass)
            if (parentDataClassInTarget) {
                parentDataClassInTarget.addToDataClasses(targetDataClass)
            }

            if (!targetDataClass.validate())
                throw new ApiInvalidModelException('DCS06', 'Copied DataClass is invalid', targetDataClass.errors, messageSource)

            save(flush: false, validate: false, targetDataClass)
        }


        pathLabels.removeAt(0)

        if (pathLabels.size() == 0) {
            return targetDataClass
        } else {
            return findOrCopyDataClassHierarchyForReferenceTypeByPath(user,
                                                                      userSecurityPolicyManager,
                                                                      sourceDataModel,
                                                                      targetDataModel,
                                                                      pathLabels,
                                                                      sourceDataClass,
                                                                      targetDataClass)
        }
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

        copy = copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, copySummaryMetadata, copyInformation)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        copiedDataModel.addToDataClasses(copy)

        if (parentDataClass) {
            parentDataClass.addToDataClasses(copy)
        }

        if (!copy.validate()) //save(validate: false, copy) else
            throw new ApiInvalidModelException('DCS01', 'Copied DataClass is invalid', copy.errors, messageSource)

        List<ReferenceType> referenceTypes = ReferenceType.by().eq('referenceClass.id', original.id).list()

        referenceTypes.sort().each {refType ->
            ReferenceType referenceType = copiedDataModel.referenceTypes.find {it.label == refType.label}
            if (!referenceType) {
                referenceType = new ReferenceType(createdBy: copier.emailAddress, label: refType.label)
                copiedDataModel.addToDataTypes(referenceType)
            }
            copy.addToReferenceTypes(referenceType)
        }

        copy.dataClasses = []

        List<DataClass> dataClasses = DataClass.byParentDataClassId(original.id).join('classifiers').list()
        CopyInformation dataClassCache = cacheFacetInformationForCopy(dataClasses.collect {it.id}, new CopyInformation(copyIndex: true))
        dataClasses.sort().each {child ->
            copyDataClass(copiedDataModel, child, copier, userSecurityPolicyManager, copy, copySummaryMetadata, dataClassCache)
        }
        copy.dataElements = []

        List<DataElement> dataElements = DataElement.byDataClassId(original.id).join('classifiers').list()
        CopyInformation dataElementCache = cacheFacetInformationForCopy(dataElements.collect {it.id}, new CopyInformation(copyIndex: true))
        dataElements.sort().each {element ->
            copy.addToDataElements(
                dataElementService
                    .copyDataElement(copiedDataModel, element, copier, userSecurityPolicyManager, copySummaryMetadata, dataElementCache))
        }

        List<DataClass> importedDataClasses = findAllByImportingDataClassId(original.id)
        copyImportedElements(copy, original, importedDataClasses, 'importedDataClasses', copier)

        List<DataClass> extendedDataClasses = findAllByExtendingDataClassId(original.id)
        copyImportedElements(copy, original, extendedDataClasses, 'extendedDataClasses', copier)

        List<DataElement> importedDataElements = dataElementService.findAllByImportingDataClassId(original.id)
        copyImportedElements(copy, original, importedDataElements, 'importedDataElements', copier)

        copy
    }

    DataClass copyModelItemInformation(DataClass original,
                                       DataClass copy,
                                       User copier,
                                       UserSecurityPolicyManager userSecurityPolicyManager,
                                       boolean copySummaryMetadata, CopyInformation copyInformation) {

        copy = super.copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, copyInformation)
        if (copySummaryMetadata) {
            copy = copySummaryMetadataFromOriginal(original, copy, copier, copyInformation)
        }
        copy
    }

    @Override
    DataClass copyModelItemInformation(DataClass original,
                                       DataClass copy,
                                       User copier,
                                       UserSecurityPolicyManager userSecurityPolicyManager, CopyInformation copyInformation = null) {
        copyModelItemInformation(original, copy, copier, userSecurityPolicyManager, false, copyInformation)
    }

    void copyImportedElements(DataClass copiedDataClass, DataClass originalDataClass, Collection<ModelItem> importedElements, String field, User copier) {
        if (!importedElements) return

        // Add all imported elements to the copy
        importedElements.each {ie ->
            copiedDataClass.addTo(field, ie)
        }

        // Find all MD which is attached to the imported elements with a key containing the original id
        // This will get all MD which pertains to the element wrt the importing object
        // However at this point we dont have a path for the DC so we need to set to "replace"
        List<Metadata> importRelevantMetadata = metadataService.findAllByMultiFacetAwareItemIdInListAndNamespaceLike(importedElements*.id, "%${originalDataClass.id}%")
        importRelevantMetadata.each {md ->
            String newNs = md.namespace.replace(originalDataClass.id.toString(), "REPLACE_${copiedDataClass.path}")
            String value = md.value
            if (md.value == originalDataClass.path.toString()) value = copiedDataClass.path
            else if (md.value == originalDataClass.id.toString()) value = "REPLACE_${copiedDataClass.path}"
            Metadata copiedMetadata = new Metadata(namespace: newNs, key: md.key, value: value, createdBy: copier.emailAddress)
            importedElements.find {it.id == md.multiFacetAwareItemId}.addToMetadata(copiedMetadata)
            metadataService.save(copiedMetadata)

        }
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

            DataClass copiedDataClass = findOrCopyDataClassHierarchyForReferenceTypeByPath(copier,
                                                                                           userSecurityPolicyManager,
                                                                                           originalDataModel,
                                                                                           copiedDataModel,
                                                                                           originalDataClassPath.split(/\|/).toList(),
                                                                                           null,
                                                                                           null)
            copiedDataClass.addToReferenceTypes(rt)
        }
        // Recursively loop until no empty reference classes
        matchUpAndAddMissingReferenceTypeClasses(copiedDataModel, originalDataModel, copier, userSecurityPolicyManager)
    }

    private Set<ReferenceType> findAllEmptyReferenceTypes(DataModel dataModel) {
        dataModel.referenceTypes.findAll {!(it as ReferenceType).referenceClass} as Set<ReferenceType>
    }

    String buildPath(DataClass dataClass) {
        if (!dataClass.parentDataClass) return dataClass.label
        "${buildPath(dataClass.parentDataClass)}|${dataClass.label}"
    }

    @Override
    boolean isCatalogueItemImportedIntoCatalogueItem(CatalogueItem catalogueItem, DataClass owningDataClass) {
        if (!(catalogueItem instanceof DataClass)) return false
        owningDataClass.id && ((DataClass) catalogueItem).parentDataClass?.id != owningDataClass.id
    }

    @Override
    boolean hasTreeTypeModelItems(DataClass dataClass, boolean fullTreeRender, boolean includeImportedDataClasses) {
        dataClass.dataClasses || (includeImportedDataClasses ? dataClass.importedDataClasses : false) || (dataClass.dataElements && fullTreeRender)
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataClass dataClass, boolean fullTreeRender, boolean includeImportedDataClasses) {
        ((includeImportedDataClasses ? DataClass.byDataModelIdAndParentDataClassIdIncludingImported(dataClass.dataModel.id, dataClass.id).list()
                                     : DataClass.byDataModelIdAndParentDataClassId(dataClass.dataModel.id, dataClass.id).list()) +
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
    List<DataClass> findAllByClassifier(Classifier classifier) {
        DataClass.byClassifierId(classifier.id).list()
    }

    @Override
    List<DataClass> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        findAllByClassifier(classifier).findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)}
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
            log.debug('Performing hs label search')
            long start = System.currentTimeMillis()
            results =
                DataClass
                    .labelHibernateSearch(DataClass, searchTerm, readableIds.toList(), dataModelService.getAllReadablePaths(readableIds)).results
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
        DataClass dataClass = findByDataModelIdAndNullParentDataClassAndLabel(parentId, label)
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

    @Override
    void propagateContentsInformation(DataClass catalogueItem, DataClass previousVersionCatalogueItem) {
        previousVersionCatalogueItem.dataClasses.each {previousChildDataClass ->
            DataClass childDataClass = catalogueItem.dataClasses.find {it.label == previousChildDataClass.label}
            if (childDataClass) {
                propagateDataFromPreviousVersion(childDataClass, previousChildDataClass)
            }
        }

        previousVersionCatalogueItem.dataElements.each {previousDataElement ->
            DataElement dataElement = catalogueItem.dataElements.find {it.label == previousDataElement.label}
            if (dataElement) {
                dataElementService.propagateDataFromPreviousVersion(dataElement, previousDataElement)
            }
        }

        previousVersionCatalogueItem.summaryMetadata.each {previousSummaryMetadata ->
            if (catalogueItem.summaryMetadata.any {it.label == previousSummaryMetadata.label}) return
            SummaryMetadata summaryMetadata = new SummaryMetadata(label: previousSummaryMetadata.label,
                                                                  description: previousSummaryMetadata.description,
                                                                  summaryMetadataType: previousSummaryMetadata.summaryMetadataType)

            previousSummaryMetadata.summaryMetadataReports.each {previousSummaryMetadataReport ->
                summaryMetadata.addToSummaryMetadataReports(reportDate: previousSummaryMetadataReport.reportDate,
                                                            reportValue: previousSummaryMetadataReport.reportValue,
                                                            createdBy: previousSummaryMetadataReport.createdBy
                )
            }
            catalogueItem.addToSummaryMetadata(summaryMetadata)
        }
    }

    @Override
    CopyInformation cacheFacetInformationForCopy(List<UUID> originalIds, CopyInformation copyInformation = null) {
        CopyInformation cachedInformation = super.cacheFacetInformationForCopy(originalIds, copyInformation)
        cacheSummaryMetadataInformationForCopy(originalIds, cachedInformation)
    }

    /**
     *
     * @param sourceDataModel
     * @param targetDataModel
     * @param dataElementInSource
     * @param pathInTarget
     * @param user
     * @param userSecurityPolicyManager
     * @return
     */
    DataClass subset(DataModel sourceDataModel, DataModel targetDataModel, DataElement dataElementInSource,
                     Path pathInTarget, UserSecurityPolicyManager userSecurityPolicyManager,
                     DataClass parentDataClassInSource = null, DataClass parentDataClassInTarget = null) {

        if (pathInTarget.size() < 2)
            throw new ApiInternalException('DCS03', "Path ${pathInTarget} was shorter than expected")

        // Get the first node, which should be a dc
        PathNode dataClassNode = pathInTarget.getPathNodes()[0]

        DataClass sourceDataClass = parentDataClassInSource ?
                                    findByDataModelIdAndParentDataClassIdAndLabel(sourceDataModel.id, parentDataClassInSource.id,
                                                                                  dataClassNode.identifier) :
                                    findByDataModelIdAndLabel(sourceDataModel.id, dataClassNode.identifier)

        if (!sourceDataClass)
            throw new ApiInternalException('DCS04', 'Source Data Class does not exist')

        DataClass targetDataClass = parentDataClassInTarget ?
                                    parentDataClassInTarget.getDataClasses().find {it.label == dataClassNode.identifier} :
                                    targetDataModel.getChildDataClasses().find {it.label == dataClassNode.identifier}

        if (!targetDataClass) {
            //Create it
            targetDataClass = new DataClass(
                minMultiplicity: sourceDataClass.minMultiplicity,
                maxMultiplicity: sourceDataClass.maxMultiplicity
            )

            targetDataClass = copyModelItemInformation(sourceDataClass, targetDataClass, userSecurityPolicyManager.user, userSecurityPolicyManager)

            targetDataModel.addToDataClasses(targetDataClass)
            if (parentDataClassInTarget) {
                parentDataClassInTarget.addToDataClasses(targetDataClass)
            }

            if (!targetDataClass.validate())
                throw new ApiInvalidModelException('DCS05', 'Subsetted DataClass is invalid', targetDataClass.errors, messageSource)

            save(flush: true, validate: false, targetDataClass)
        }

        // Get the next node, which could be a dc or de
        PathNode nextNode = pathInTarget.getPathNodes()[1]

        if (nextNode.prefix == 'dc') {
            targetDataClass =
                subset(sourceDataModel, targetDataModel, dataElementInSource, pathInTarget.getChildPath(), userSecurityPolicyManager,
                       sourceDataClass, targetDataClass)
        } else if (nextNode.prefix == 'de') {
            DataElement dataElementInTarget =
                dataElementService.copyDataElement(targetDataModel, dataElementInSource, userSecurityPolicyManager.user, userSecurityPolicyManager)
            targetDataClass.addToDataElements(dataElementInTarget)
            matchUpAndAddMissingReferenceTypeClasses(targetDataModel, sourceDataModel, userSecurityPolicyManager.user, userSecurityPolicyManager)
            if (!dataElementService.validate(dataElementInTarget)) {
                throw new ApiInvalidModelException(
                    'DCS06',
                    "dataElementInTarget ${dataElementInTarget.id} failed validation",
                    dataElementInTarget.errors,
                    messageSource)
            }
            dataElementService.save(flush: true, validate: false, dataElementInTarget)
        } else {
            throw new ApiInternalException('DCS07', "Unexpected node prefix ${nextNode.prefix}")
        }

        validate(targetDataClass)

        targetDataClass
    }

    boolean validateImportAddition(DataClass instance, ModelItem importingItem) {
        if (instance.id == importingItem.id) {
            instance.errors.reject('invalid.imported.dataclass.into.self',
                                   [importingItem.id].toArray(),
                                   'DataClass [{0}] cannot be imported into itself')
        }
        UUID owningDataClassId = importingItem.instanceOf(DataClass) ? importingItem.parentDataClass?.id : importingItem.dataClass.id
        if (owningDataClassId == instance.id) {
            instance.errors.reject('invalid.imported.modelitem.same.dataclass',
                                   [importingItem.class.simpleName, importingItem.id].toArray(),
                                   '{0} [{1}] to be imported belongs to the DataClass already')
        }
        if (importingItem.model.id != instance.model.id &&
            !importingItem.model.finalised &&
            !dataModelService.areModelsInsideSameVersionedFolder(instance.model, importingItem.model)) {
            instance.errors.reject('invalid.imported.modelitem.model.not.finalised',
                                   [importingItem.class.simpleName, importingItem.id].toArray(),
                                   '{0} [{1}] to be imported does not belong to a finalised DataModel or reside inside the same VersionedFolder')
        }
        !instance.hasErrors()
    }

    boolean validateImportRemoval(DataClass instance, ModelItem importingItem) {
        UUID owningDataClassId = importingItem.instanceOf(DataClass) ? importingItem.parentDataClass?.id : importingItem.dataClass.id
        if (owningDataClassId == instance.id) {
            instance.errors.reject('invalid.imported.deletion.modelitem.same.dataclass',
                                   [importingItem.class.simpleName, importingItem.id].toArray(),
                                   '{0} [{1}] belongs to the DataClass and cannot be removed as an import')
        }
        !instance.hasErrors()
    }
}
