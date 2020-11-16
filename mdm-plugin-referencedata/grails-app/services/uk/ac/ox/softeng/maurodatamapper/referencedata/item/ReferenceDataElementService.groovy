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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.similarity.SimilarityResult
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadata
import uk.ac.ox.softeng.maurodatamapper.referencedata.facet.ReferenceSummaryMetadataService
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataTypeService
import uk.ac.ox.softeng.maurodatamapper.referencedata.similarity.DataElementSimilarityResult
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.apache.lucene.search.Query
import org.hibernate.search.engine.ProjectionConstants
import org.hibernate.search.jpa.FullTextEntityManager
import org.hibernate.search.jpa.FullTextQuery
import org.hibernate.search.jpa.Search
import org.hibernate.search.query.dsl.QueryBuilder

import javax.persistence.EntityManager

@Slf4j
@Transactional
class ReferenceDataElementService extends ModelItemService<ReferenceDataElement> {

    ReferenceDataTypeService referenceDataTypeService
    ReferenceSummaryMetadataService referenceSummaryMetadataService

    @Override
    ReferenceDataElement get(Serializable id) {
        ReferenceDataElement.get(id)
    }

    Long count() {
        ReferenceDataElement.count()
    }

    @Override
    List<ReferenceDataElement> list(Map args) {
        ReferenceDataElement.list(args)
    }

    @Override
    List<ReferenceDataElement> getAll(Collection<UUID> ids) {
        ReferenceDataElement.getAll(ids).findAll()
    }

    @Override
    void deleteAll(Collection<ReferenceDataElement> dataElements) {
        dataElements.each { delete(it) }
    }

    void delete(UUID id) {
        delete(get(id), true)
    }

    void delete(ReferenceDataElement dataElement, boolean flush = false) {
        if (!dataElement) return
        dataElement.breadcrumbTree.removeFromParent()
        dataElement.referenceDataType = null
        dataElement.referenceDataModel?.removeFromReferenceDataElements(dataElement)
        dataElement.delete(flush: flush)
    }

    @Override
    ReferenceDataElement updateFacetsAfterInsertingCatalogueItem(ReferenceDataElement catalogueItem) {
        super.updateFacetsAfterInsertingCatalogueItem(catalogueItem)
        if (catalogueItem.referenceSummaryMetadata) {
            catalogueItem.referenceSummaryMetadata.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            ReferenceSummaryMetadata.saveAll(catalogueItem.referenceSummaryMetadata)
        }
        catalogueItem
    }

    @Override
    boolean hasTreeTypeModelItems(ReferenceDataElement catalogueItem, boolean forDiff) {
        false
    }

    @Override
    List<ModelItem> findAllTreeTypeModelItemsIn(ReferenceDataElement catalogueItem, boolean forDiff) {
        []
    }

    @Override
    ReferenceDataElement findByIdJoinClassifiers(UUID id) {
        ReferenceDataElement.findById(id, [fetch: [classifiers: 'join']])
    }

    @Override
    void removeAllFromClassifier(Classifier classifier) {
        ReferenceDataElement.byClassifierId(classifier.id).list().each {
            it.removeFromClassifiers(classifier)
        }
    }

    @Override
    List<ReferenceDataElement> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier) {
        ReferenceDataElement.byClassifierId(classifier.id).list().findAll { userSecurityPolicyManager.userCanReadSecuredResourceId(ReferenceDataModel, it.model.id) }
    }

    @Override
    Class<ReferenceDataElement> getModelItemClass() {
        ReferenceDataElement
    }

    @Override
    Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType) {
        domainType == ReferenceDataElement.simpleName
    }


    @Override
    List<ReferenceDataElement> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                          String searchTerm, String domainType) {
        List<UUID> readableIds = userSecurityPolicyManager.listReadableSecuredResourceIds(ReferenceDataModel)
        if (!readableIds) return []

        List<ReferenceDataElement> results = []
        if (shouldPerformSearchForTreeTypeCatalogueItems(domainType)) {
            log.debug('Performing lucene label search')
            long start = System.currentTimeMillis()
            results = ReferenceDataElement.luceneLabelSearch(ReferenceDataElement, searchTerm, readableIds.toList()).results
            log.debug("Search took: ${Utils.getTimeString(System.currentTimeMillis() - start)}. Found ${results.size()}")
        }

        results
    }

    def saveAll(Collection<ReferenceDataElement> dataElements) {

        List<Classifier> classifiers = dataElements.collectMany { it.classifiers ?: [] } as List<Classifier>
        if (classifiers) {
            log.trace('Saving {} classifiers')
            classifierService.saveAll(classifiers)
        }

        Collection<ReferenceDataElement> alreadySaved = dataElements.findAll { it.ident() && it.isDirty() }
        Collection<ReferenceDataElement> notSaved = dataElements.findAll { !it.ident() }

        if (alreadySaved) {
            log.trace('Straight saving {} already saved DataElements', alreadySaved.size())
            ReferenceDataElement.saveAll(alreadySaved)
        }

        if (notSaved) {
            log.trace('Batch saving {} new DataElements in batches of {}', notSaved.size(), ReferenceDataElement.BATCH_SIZE)
            List batch = []
            int count = 0

            notSaved.each { de ->

                batch += de
                count++
                if (count % ReferenceDataElement.BATCH_SIZE == 0) {
                    batchSave(batch)
                    batch.clear()
                }
            }
            batchSave(batch)
            batch.clear()
        }
    }

    void batchSave(List<ReferenceDataElement> dataElements) {
        long start = System.currentTimeMillis()
        log.trace('Performing batch save of {} DataElements', dataElements.size())

        ReferenceDataElement.saveAll(dataElements)
        dataElements.each { updateFacetsAfterInsertingCatalogueItem(it) }

        sessionFactory.currentSession.flush()
        sessionFactory.currentSession.clear()

        log.trace('Batch save took {}', Utils.getTimeString(System.currentTimeMillis() - start))
    }

    void matchUpDataTypes(ReferenceDataModel referenceDataModel, Collection<ReferenceDataElement> dataElements) {
        if (referenceDataModel.referenceDataTypes == null) referenceDataModel.referenceDataTypes = [] as HashSet
        if (dataElements) {
            log.debug("Matching up {} DataElements to a possible {} DataTypes", dataElements.size(), referenceDataModel.referenceDataTypes.size())
            def grouped = dataElements.groupBy { it.referenceDataType.label }.sort { a, b ->
                def res = a.value.size() <=> b.value.size()
                if (res == 0) res = a.key <=> b.key
                res
            }
            log.debug('Grouped {} DataElements by DataType label', grouped.size())
            grouped.each { label, elements ->
                log.trace('Matching {} elements to DataType label {}', elements.size(), label)
                ReferenceDataType dataType = referenceDataModel.findDataTypeByLabel(label)

                if (!dataType) {
                    log.debug('No DataType for {} in DataModel, using first DataElement DataType as base', label)
                    dataType = elements.first().referenceDataType
                    referenceDataModel.addToReferenceDataTypes(dataType)
                }
                elements.each { dataType.addToDataElements(it) }
            }
        }
    }

    ReferenceDataElement findByReferenceDataModelIdAndId(Serializable referenceDataModelId, Serializable id) {
        ReferenceDataElement.byReferenceDataModelIdAndId(referenceDataModelId, id).find()
    }

    ReferenceDataElement findByReferenceDataTypeIdAndId(Serializable referenceDataTypeId, Serializable id) {
        ReferenceDataElement.byReferenceDataTypeIdAndId(referenceDataTypeId, id).find()
    }

    List<ReferenceDataElement> findAllByReferenceDataModelId(Serializable referenceDataModelId, Map pagination = [:]) {
        findAllByReferenceDataModelId(referenceDataModelId, pagination, pagination)
    }

    List<ReferenceDataElement> findAllByReferenceDataModelId(Serializable referenceDataModelId, Map filter, Map pagination) {
        ReferenceDataElement.withFilter(ReferenceDataElement.byReferenceDataModelId(referenceDataModelId), filter).list(pagination)
    }

    List<ReferenceDataElement> findAllByReferenceDataModelIdJoinDataType(Serializable referenceDataModelId) {
        ReferenceDataElement.byReferenceDataModelId(referenceDataModelId).join('dataType').sort('label').list()
    }

    List<ReferenceDataElement> findAllByReferenceDataTypeId(Serializable referenceDataTypeId, Map pagination = [:]) {
        ReferenceDataElement.withFilter(ReferenceDataElement.byReferenceDataTypeId(referenceDataTypeId), pagination).list(pagination)
    }

    List<ReferenceDataElement> findAllByReferenceDataType(ReferenceDataType referenceDataType) {
        ReferenceDataElement.byReferenceDataType(referenceDataType).list()
    }

    List<ReferenceDataElement> findAllByReferenceDataModel(ReferenceDataModel referenceDataModel) {
        ReferenceDataElement.byReferenceDataModel(referenceDataModel).list()
    }


    List<ReferenceDataElement> findAllByReferenceDataModelIdAndLabelIlike(Serializable referenceDataModelId, String labelSearch, Map pagination = [:]) {
        ReferenceDataElement.byReferenceDataModelIdAndLabelIlike(referenceDataModelId, labelSearch).list(pagination)
    }

    Number countByReferenceDataTypeId(Serializable referenceDataTypeId) {
        ReferenceDataElement.byReferenceDataTypeId(referenceDataTypeId).count()
    }

    Number countByReferenceDataModelId(Serializable referenceDataModelId) {
        ReferenceDataElement.byReferenceDataModelId(referenceDataModelId).count()
    }


    ReferenceDataElement findOrCreateDataElementForReferenceDataModel(ReferenceDataModel referenceDataModel, String label, String description, User createdBy,
                                                                      ReferenceDataType dataType,
                                                                      Integer minMultiplicity = 0, Integer maxMultiplicity = 1) {
        String cleanLabel = label.trim()
        ReferenceDataElement dataElement = referenceDataModel.findDataElement(cleanLabel)

        if (!dataElement) {
            dataElement = new ReferenceDataElement(label: cleanLabel, description: description, createdBy: createdBy.emailAddress,
                                          minMultiplicity: minMultiplicity,
                                          maxMultiplicity: maxMultiplicity)

            if (!dataType.label) dataType.setLabel("$cleanLabel-dataType")
            dataType.addToDataElements(dataElement)
            referenceDataModel.addToReferenceDataElements(dataElement)
        }
        if (dataElement.referenceDataType.label != dataType.label) {
            return findOrCreateDataElementForReferenceDataModel(referenceDataModel, "${cleanLabel}.1", description, createdBy, dataType, minMultiplicity,
                                                       maxMultiplicity)
        }
        dataElement
    }

    ReferenceDataElement copyReferenceDataElement(ReferenceDataModel copiedReferenceDataModel, ReferenceDataElement original, User copier,
                                         UserSecurityPolicyManager userSecurityPolicyManager) {
        ReferenceDataElement copy = new ReferenceDataElement(minMultiplicity: original.minMultiplicity,
                                           maxMultiplicity: original.maxMultiplicity)

        copy = copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager)
        setCatalogueItemRefinesCatalogueItem(copy, original, copier)

        ReferenceDataType referenceDataType = copiedReferenceDataModel.findReferenceDataTypeByLabel(original.referenceDataType.label)

        // If theres no DataType then copy the original's DataType into the DataModel
        if (!referenceDataType) {
            referenceDataType = referenceDataTypeService.copyReferenceDataType(copiedReferenceDataModel, original.referenceDataType, copier,
                                                    userSecurityPolicyManager)
        }

        copy.referenceDataType = referenceDataType

        copiedReferenceDataModel.addToReferenceDataElements(copy)

        copy
    }

    @Override
    ReferenceDataElement copyCatalogueItemInformation(ReferenceDataElement original,
                                                      ReferenceDataElement copy,
                                                      User copier,
                                                      UserSecurityPolicyManager userSecurityPolicyManager,
                                                      boolean copySummaryMetadata = false) {
        copy = super.copyCatalogueItemInformation(original, copy, copier, userSecurityPolicyManager)
        if (copySummaryMetadata) {
            referenceSummaryMetadataService.findAllByCatalogueItemId(original.id).each {
                copy.addToReferenceSummaryMetadata(label: it.label, summaryMetadataType: it.summaryMetadataType, createdBy: copier.emailAddress)
            }
        }
        copy
    }

    DataElementSimilarityResult findAllSimilarReferenceDataElementsInReferenceDataModel(ReferenceDataModel referenceDataModelToSearch, ReferenceDataElement referenceDataElementToCompare, maxResults = 5) {

        EntityManager entityManager = sessionFactory.createEntityManager()
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager)


        QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
            .buildQueryBuilder()
            .forEntity(ReferenceDataElement)
            .get()

        Query moreLikeThisQuery = queryBuilder
            .bool()
            .must(queryBuilder.moreLikeThis()
                      .excludeEntityUsedForComparison()
                      .comparingField('label').boostedTo(1f)
                      .andField('description').boostedTo(1f)
                      .andField('referenceDataType.label').boostedTo(1f)
                      .toEntity(referenceDataElementToCompare)
                      .createQuery()
            )
            .createQuery()

        SimilarityResult similarityResult = new DataElementSimilarityResult(referenceDataElementToCompare)

        FullTextQuery query = fullTextEntityManager
            .createFullTextQuery(moreLikeThisQuery, ReferenceDataElement)
            .setMaxResults(maxResults)
            .setProjection(ProjectionConstants.THIS, ProjectionConstants.SCORE)

        query.enableFullTextFilter('idPathSecured').setParameter('allowedIds', [referenceDataModelToSearch.id])

        query.getResultList().each {
            similarityResult.add(get(it[0].id), it[1])
        }

        similarityResult
    }


    void addDataElementIsFromDataElements(ReferenceDataElement dataElement, Collection<ReferenceDataElement> fromDataElements, User user) {
        addDataElementsAreFromDataElements([dataElement], fromDataElements, user)
    }

    void addDataElementsAreFromDataElement(Collection<ReferenceDataElement> dataElements, ReferenceDataElement fromDataElement, User user) {
        addDataElementsAreFromDataElements(dataElements, [fromDataElement], user)
    }

    void addDataElementsAreFromDataElements(Collection<ReferenceDataElement> dataElements, Collection<ReferenceDataElement> fromDataElements, User user) {
        if (!dataElements || !fromDataElements) throw new ApiInternalException('DESXX', 'No DataElements or FromDataElements exist to create links')
        List<SemanticLink> alreadyExistingLinks = semanticLinkService.findAllBySourceCatalogueItemIdInListAndTargetCatalogueItemIdInListAndLinkType(
            dataElements*.id, fromDataElements*.id, SemanticLinkType.IS_FROM)
        dataElements.each { de ->
            fromDataElements.each { fde ->
                // If no link already exists then add a new one
                if (!alreadyExistingLinks.any { it.catalogueItemId == de.id && it.targetCatalogueItemId == fde.id }) {
                    setDataElementIsFromDataElement(de, fde, user)
                }
            }
        }
    }

    void removeDataElementIsFromDataElements(ReferenceDataElement dataElement, Collection<ReferenceDataElement> fromDataElements) {
        removeDataElementsAreFromDataElements([dataElement], fromDataElements)
    }

    void removeDataElementsAreFromDataElement(Collection<ReferenceDataElement> dataElements, ReferenceDataElement fromDataElement) {
        removeDataElementsAreFromDataElements(dataElements, [fromDataElement])
    }

    void removeDataElementsAreFromDataElements(Collection<ReferenceDataElement> dataElements, Collection<ReferenceDataElement> fromDataElements) {
        if (!dataElements || !fromDataElements) throw new ApiInternalException('DESXX', 'No DataElements or FromDataElements exist to remove links')
        List<SemanticLink> links = semanticLinkService.findAllBySourceCatalogueItemIdInListAndTargetCatalogueItemIdInListAndLinkType(
            dataElements*.id, fromDataElements*.id, SemanticLinkType.IS_FROM)
        semanticLinkService.deleteAll(links)
    }

    void setDataElementIsFromDataElement(ReferenceDataElement source, ReferenceDataElement target, User user) {
        source.addToSemanticLinks(linkType: SemanticLinkType.IS_FROM, createdBy: user.getEmailAddress(), targetCatalogueItem: target)
    }

    void checkImportedReferenceDataElementAssociations(User importingUser, ReferenceDataModel referenceDataModel, ReferenceDataElement referenceDataElement) {
        referenceDataModel.addToReferenceDataElements(referenceDataElement)
        referenceDataElement.createdBy = importingUser.emailAddress
        referenceDataElement.referenceDataType.createdBy = importingUser.emailAddress

        //Get the reference data type for this element by getting the matching reference data type for the model
        referenceDataElement.referenceDataType = referenceDataModel.referenceDataTypes.find {it.label == referenceDataElement.referenceDataType.label}

        checkFacetsAfterImportingCatalogueItem(referenceDataElement)
    }    
}