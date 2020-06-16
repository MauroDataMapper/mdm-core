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


import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.similarity.SimilarityResult
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModelService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.apache.lucene.search.Query
import org.hibernate.SessionFactory
import org.hibernate.search.engine.ProjectionConstants
import org.hibernate.search.jpa.FullTextEntityManager
import org.hibernate.search.jpa.FullTextQuery
import org.hibernate.search.jpa.Search
import org.hibernate.search.query.dsl.QueryBuilder

import javax.persistence.EntityManager

@Slf4j
@Transactional
class DataElementService extends ModelItemService<DataElement> {

    DataModelService dataModelService
    DataClassService dataClassService
    DataTypeService dataTypeService
    SessionFactory sessionFactory

    @Override
    DataElement get(Serializable id) {
        DataElement.get(id)
    }

    Long count() {
        DataElement.count()
    }

    @Override
    List<DataElement> list(Map args) {
        DataElement.list(args)
    }

    @Override
    List<DataElement> getAll(Collection<UUID> ids) {
        DataElement.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<DataElement> dataElements) {
        dataElements.each {delete(it)}
    }

    void delete(UUID id) {
        delete(get(id), true)
    }

    void delete(DataElement dataElement, boolean flush = false) {
        if (!dataElement) return
        dataElement.breadcrumbTree.removeFromParent()
        dataElement.dataType = null
        dataElement.dataClass?.removeFromDataElements(dataElement)
        dataElement.delete(flush: flush)
    }

    @Override
    DataElement save(DataElement catalogueItem) {
        catalogueItem.save(flush: true)
        updateFacetsAfterInsertingCatalogueItem(catalogueItem)
    }

    @Override
    boolean hasTreeTypeModelItems(DataElement catalogueItem) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(DataElement catalogueItem) {
        []
    }

    @Override
    DataElement findByIdJoinClassifiers(UUID id) {
        DataElement.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        DataElement.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<DataElement> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        DataElement.byClassifierId(classifier.id).list().findAll {userSecurityPolicyManager.userCanReadSecuredResourceId(DataModel, it.model.id)}
    }

    @Override
    Class<DataElement> getModelItemClass() {
        DataElement
    }

    @Override
    DataElement updateIndexForModelItemInParent(DataElement modelItem, CatalogueItem parent, int newIndex) {
        throw new ApiNotYetImplementedException('DESXX', 'DataElement Ordering')
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == DataElement.simpleName
    }


    @Override
    List<DataElement> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                 String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(DataModel)
        if (!readableIds) return []

        List<DataElement> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = DataElement.luceneLabelSearch(DataElement, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    def saveAll(Collection<DataElement> dataElements) {

        Collection<DataElement> alreadySaved = dataElements.findAll {it.ident() && it.isDirty()}
        Collection<DataElement> notSaved = dataElements.findAll {!it.ident()}

        if (alreadySaved) {
            log.trace('Straight saving {} dataelements', alreadySaved.size())
            DataElement.saveAll(alreadySaved)
        }

        if (notSaved) {
            log.trace('Batch saving {} dataelements', notSaved.size())
            List batch = []
            int count = 0

            notSaved.each {de ->

                batch += de
                count++
                if (count % DataElement.BATCH_SIZE == 0) {
                    batchSave(batch)
                    batch.clear()
                }
            }
            batchSave(batch)
            batch.clear()
        }
    }

    void batchSave(List<DataElement> dataElements) {
        long start = System.currentTimeMillis()
        log.trace('Batch saving {} dataElements', dataElements.size())

        DataElement.saveAll(dataElements)
        dataElements.each {updateFacetsAfterInsertingCatalogueItem(it)}

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.trace('Batch save took {}', Utils.getTimeString(System.currentTimeMillis() - start))
    }

    void matchUpDataTypes(DataModel dataModel, Collection<DataElement> dataElements) {
        if (dataModel.dataTypes == null) dataModel.dataTypes = [] as HashSet
        if (dataElements) {
            log.debug("Matching up {} DataElements to a possible {} DataTypes", dataElements.size(), dataModel.dataTypes.size())
            def grouped = dataElements.groupBy {it.dataType.label}.sort {a, b ->
                def res = a.value.size() <=> b.value.size()
                if (res == 0) res = a.key <=> b.key
                res
            }
            log.debug('Grouped {} DataElements by DataType label', grouped.size())
            grouped.each {label, elements ->
                log.trace('Matching {} elements to DataType label {}', elements.size(), label)
                DataType dataType = dataModel.findDataTypeByLabel(label)

                if (!dataType) {
                    log.debug('No DataType for {} in DataModel, using first DataElement DataType as base', label)
                    dataType = elements.first().dataType
                    dataModel.addToDataTypes(dataType)
                }
                elements.each {dataType.addToDataElements(it)}
            }
        }
    }

    DataElement findByDataClassIdAndId(Serializable dataClassId, Serializable id) {
        DataElement.byDataClassIdAndId(dataClassId, id).find()
    }

    DataElement findByDataTypeIdAndId(Serializable dataTypeId, Serializable id) {
        DataElement.byDataTypeIdAndId(dataTypeId, id).find()
    }

    List<DataElement> findAllByDataClassId(Serializable dataClassId, Map pagination = [:]) {
        findAllByDataClassId(dataClassId, pagination, pagination)
    }

    List<DataElement> findAllByDataClassId(Serializable dataClassId, Map filter, Map pagination) {
        DataElement.withFilter(DataElement.byDataClassId(dataClassId), filter).list(pagination)
    }

    List<DataElement> findAllByDataClassIdJoinDataType(Serializable dataClassId) {
        DataElement.byDataClassId(dataClassId).join('dataType').sort('label').list()
    }

    List<DataElement> findAllByDataTypeId(Serializable dataTypeId, Map pagination = [:]) {
        DataElement.withFilter(DataElement.byDataTypeId(dataTypeId), pagination).list(pagination)
    }

    List<DataElement> findAllByDataType(DataType dataType) {
        DataElement.byDataType(dataType).list()
    }

    List<DataElement> findAllByDataClass(DataClass dataClass) {
        DataElement.byDataClass(dataClass).list()
    }

    List<DataElement> findAllByDataModelId(Serializable dataModelId, Map pagination = [:]) {
        DataElement.byDataModelId(dataModelId).list(pagination)
    }

    List<DataElement> findAllByDataModelIdAndLabelIlike(Serializable dataModelId, String labelSearch, Map pagination = [:]) {
        DataElement.byDataModelIdAndLabelIlike(dataModelId, labelSearch).list(pagination)
    }

    Number countByDataClassId(Serializable dataClassId) {
        DataElement.byDataClassId(dataClassId).count()
    }

    Number countByDataTypeId(Serializable dataTypeId) {
        DataElement.byDataTypeId(dataTypeId).count()
    }

    Number countByDataModelId(Serializable dataModelId) {
        DataElement.byDataModelId(dataModelId).count()
    }

    DataElement findByDataClassPathAndLabel(DataModel dataModel, List<String> dataClassPath, String label) {
        DataClass dataClass = dataClassService.findDataClassByPath(dataModel, dataClassPath)
        dataClass ? findByDataClassIdAndLabel(dataClass.id, label) : null
    }

    DataElement findByDataClassIdAndLabel(Serializable dataClassId, String label) {
        DataElement.byDataClassIdAndLabel(dataClassId, label).get()
    }

    DataElement findOrCreateDataElementForDataClass(DataClass parentClass, String label, String description, User createdBy,
                                                    DataType dataType,
                                                    Integer minMultiplicity = 0, Integer maxMultiplicity = 1) {
        String cleanLabel = label.trim()
        DataElement dataElement = parentClass.findDataElement(cleanLabel)

        if (!dataElement) {
            dataElement = new DataElement(label: cleanLabel, description: description, createdBy: createdBy.emailAddress,
                                          minMultiplicity: minMultiplicity,
                                          maxMultiplicity: maxMultiplicity)

            if (!dataType.label) dataType.setLabel("$cleanLabel-dataType")
            dataType.addToDataElements(dataElement)
            parentClass.addToDataElements(dataElement)
        }
        if (dataElement.dataType.label != dataType.label) {
            return findOrCreateDataElementForDataClass(parentClass, "${cleanLabel}.1", description, createdBy, dataType, minMultiplicity,
                                                       maxMultiplicity)
        }
        dataElement
    }

    DataElement copyDataElement(DataModel copiedDataModel, DataElement original, User copier) {
        DataElement copy = new DataElement(minMultiplicity: original.minMultiplicity,
                                           maxMultiplicity: original.maxMultiplicity)

        copy = copyCatalogueItemInformation(original, copy, copier)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        DataType dataType = copiedDataModel.findDataTypeByLabel(original.dataType.label)

        // If theres no DataType then copy the original's DataType into the DataModel
        if (!dataType) {
            dataType = dataTypeService.copyDataType(copiedDataModel, original.dataType, copier)
        }

        copy.dataType = dataType

        copy
    }

    DataElementSimilarityResult findAllSimilarDataElementsInDataModel(DataModel dataModelToSearch, DataElement dataElementToCompare, maxResults = 5) {

        EntityManager entityManager = sessionFactory.createEntityManager()
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager)


        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
            .buildQueryBuilder()
            .forEntity(DataElement)
            .get()

        Query moreLikeThisQuery = queryBuilder
            .bool()
            .must(queryBuilder.moreLikeThis()
                      .excludeEntityUsedForComparison()
                      .comparingField('label').boostedTo(1f)
                      .andField('description').boostedTo(1f)
                      .andField('dataType.label').boostedTo(1f)
                      .andField('dataClass.label').boostedTo(1f)
                      .toEntity(dataElementToCompare)
                      .createQuery()
            )
            .createQuery()

        SimilarityResult similarityResult = new DataElementSimilarityResult(dataElementToCompare)

        FullTextQuery query = fullTextEntityManager
            .createFullTextQuery(moreLikeThisQuery, DataElement)
            .setMaxResults(maxResults)
            .setProjection(ProjectionConstants.THIS, ProjectionConstants.SCORE)

        query.enableFullTextFilter('idPathSecured').setParameter('allowedIds', [dataModelToSearch.id])

        query.getResultList().each {
            similarityResult.add(get(it[0].id), it[1])
        }

        similarityResult
    }


    DataElement findDataElementWithSameLabelTree(DataModel dataModel, DataElement original) {
        DataClass parent = dataClassService.findSameLabelTree(dataModel, original.dataClass)
        parent.findDataElement(original.label)
    }

    void addDataElementsAreFromDataElements(Collection<DataElement> sourceElements, Collection<DataElement> targetElements,
                                            User catalogueUser) {
        sourceElements.each {se ->
            addDataElementIsFromDataElements(se, targetElements, catalogueUser)
        }
    }

    void addDataElementsAreFromDataElement(Collection<DataElement> sourceElements, DataElement targetElement,
                                           User catalogueUser) {
        sourceElements.findAll {se -> !(targetElement.id in se.semanticLinks.collect {it.targetCatalogueItemId})}.each {se ->
            setCatalogueItemRefinesCatalogueItem(se, targetElement, catalogueUser)
        }
    }

    void addDataElementIsFromDataElements(DataElement sourceElement, Collection<DataElement> targetElements,
                                          User catalogueUser) {
        targetElements.findAll {te -> !(te.id in sourceElement.semanticLinks.collect {it.targetCatalogueItemId})}.each {te ->
            setCatalogueItemRefinesCatalogueItem(sourceElement, te, catalogueUser)
        }

    }

    void removeDataElementIsFromDataElements(DataElement sourceElement, Collection<DataElement> targetElements) {
        List<SemanticLink> links = []
        links += sourceElement.semanticLinks.findAll {it.targetCatalogueItemId in targetElements.collect {it.id}}
        links.each {
            sourceElement.removeFromSemanticLinks(it)
        }
    }

    void removeDataElementsAreFromDataElement(Collection<DataElement> sourceElements, DataElement targetElement) {
        sourceElements.each {sourceElement ->
            List<SemanticLink> links = []
            links += sourceElement.semanticLinks.findAll {it.targetCatalogueItemId == targetElement.id}
            links.each {
                sourceElement.removeFromSemanticLinks(it)
            }
        }
    }
}