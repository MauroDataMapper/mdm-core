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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelExtend
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImport
import uk.ac.ox.softeng.maurodatamapper.core.facet.ModelImportService
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFileService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.RuleService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.DomainService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import grails.core.GrailsClass
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.orm.hibernate.cfg.JoinTable
import org.grails.orm.hibernate.cfg.Mapping
import org.grails.orm.hibernate.cfg.PropertyConfig
import org.grails.orm.hibernate.cfg.Table
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
abstract class CatalogueItemService<K extends CatalogueItem> implements DomainService<K> {

    @Autowired
    GrailsApplication grailsApplication

    ClassifierService classifierService
    MetadataService metadataService
    RuleService ruleService
    ModelImportService modelImportService
    SemanticLinkService semanticLinkService
    SessionFactory sessionFactory
    AnnotationService annotationService
    ReferenceFileService referenceFileService

    abstract Class<K> getCatalogueItemClass()

    boolean handles(Class clazz) {
        clazz == getCatalogueItemClass()
    }

    boolean handles(String domainType) {
        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, domainType)
        if (!grailsClass) {
            throw new ApiBadRequestException('CISXX', "Unrecognised domain class resource [${domainType}]")
        }
        handles(grailsClass.clazz)
    }

    boolean handlesPathPrefix(String pathPrefix) {
        false
    }

    /**
     * What domains does the catalogue item allow to be imported? Override this in sub-classes.
     */
    List<Class> importsDomains() {
        []
    }

    /**
     * Does the catalogue item allow a catalogue item of type importedDomainType to be imported?
     *
     * @param clazz Domain (Class) that something is trying to import.
     * @return boolean Is the import of this domain type allowed or not?
     */
    boolean importsDomain(Class clazz) {
        importsDomains().contains(clazz)
    }

    /**
     * Does the catalogue item allow a catalogue item of type importedDomainType to be imported?
     *
     * @param importedDomainType Domain type (string) of the domain that something is trying to import.
     * @return boolean Is the import of this domain type allowed or not?
     */
    boolean importsDomainType(String importedDomainType) {
        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, importedDomainType)
        if (!grailsClass) {
            throw new ApiBadRequestException('CISXX', "Unrecognised domain class resource [${domainType}]")
        }
        importsDomain(grailsClass.clazz)
    }

    /**
     * Does the importedModelItem belong to a DataModel which is finalised, or does it belong to the same
     * collection as the importing DataModel?
     *
     * @param importingDataModel The DataModel which is importing the importedModelItem
     * @param importedModelItem The ModelItem which is being imported into importingDataModel
     *
     * @return boolean Is this import allowed by domain specific rules?
     */
    boolean isImportableByCatalogueItem(CatalogueItem importingCatalogueItem, CatalogueItem importedCatalogueItem) {
        false
    }

    /**
     * What domains can the catalogue item extend? Override this in sub-classes.
     */
    List<Class> extendsDomains() {
        []
    }

    /**
     * Can the catalogue item extend a catalogue item of type extendedDomainType?
     *
     * @param clazz Domain (Class) that something is trying to extend.
     * @return boolean Is the extend of this domain type allowed or not?
     */
    boolean extendsDomain(Class clazz) {
        extendsDomains().contains(clazz)
    }

    /**
     * Does the catalogue item allow a catalogue item of type extendedDomainType to be extended?
     *
     * @param extendedDomainType Domain type (string) of the domain that something is trying to extend.
     * @return boolean Is the extend of this domain type allowed or not?
     */
    boolean extendsDomainType(String extendedDomainType) {
        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, extendedDomainType)
        if (!grailsClass) {
            throw new ApiBadRequestException('CISXX', "Unrecognised domain class resource [${domainType}]")
        }
        extendsDomain(grailsClass.clazz)
    }

    /**
     * Does the extendeModelItem pass domain specific rules for extension?
     *
     * @param extendingCatalogueItem The CatalogueItem which is extending the extendedCatalogueItem
     * @param extendedCatalogueItem The CatalogueItem which is being extended by extendingCatalogueItem
     *
     * @return boolean Is this extend allowed by domain specific rules?
     */
    boolean isExtendableByCatalogueItem(CatalogueItem extendingCatalogueItem, CatalogueItem extendedCatalogueItem) {
        false
    }    

    abstract void deleteAll(Collection<K> catalogueItems)

    K save(K catalogueItem) {
        // Default behaviours for save in GormEntity
        save(flush: false, validate: true, catalogueItem)
    }

    K save(Map args, K catalogueItem) {
        Map saveArgs = new HashMap(args)
        if (args.flush) {
            saveArgs.remove('flush')
            (catalogueItem as GormEntity).save(saveArgs)
            updateFacetsAfterInsertingCatalogueItem(catalogueItem)
            sessionFactory.currentSession.flush()
        } else {
            (catalogueItem as GormEntity).save(args)
            updateFacetsAfterInsertingCatalogueItem(catalogueItem)
        }
        catalogueItem
    }

    /**
     * Use domain.getAll(ids) to retrieve objects from the database.
     *
     * Make sure you use findAll() on the output of this, its possible to get ids which dont exist in this domain and the Grails implementation
     * of getAll(ids) will return a list of null elements
     * @param ids
     * @return
     */
    abstract List<K> getAll(Collection<UUID> ids)

    abstract boolean hasTreeTypeModelItems(K catalogueItem, boolean forDiff, boolean includeImported)

    abstract List<ModelItem> findAllTreeTypeModelItemsIn(K catalogueItem, boolean forDiff = false, boolean includeImported = false)

    abstract K findByIdJoinClassifiers(UUID id)

    abstract void removeAllFromClassifier(Classifier classifier)

    abstract List<K> findAllReadableByClassifier(UserSecurityPolicyManager userSecurityPolicyManager, Classifier classifier)

    abstract List<K> findAllReadableTreeTypeCatalogueItemsBySearchTermAndDomain(UserSecurityPolicyManager userSecurityPolicyManager,
                                                                                String searchTerm, String domainType)

    abstract Boolean shouldPerformSearchForTreeTypeCatalogueItems(String domainType)

    void addClassifierToCatalogueItem(UUID catalogueItemId, Classifier classifier) {
        get(catalogueItemId).addToClassifiers(classifier)
    }

    void removeClassifierFromCatalogueItem(UUID catalogueItemId, Classifier classifier) {
        removeFacetFromDomain(catalogueItemId, classifier.id, 'classifiers')
    }

    void removeMetadataFromCatalogueItem(UUID catalogueItemId, Metadata metadata) {
        removeFacetFromDomain(catalogueItemId, metadata.id, 'metadata')
    }

    void removeAnnotationFromCatalogueItem(UUID catalogueItemId, Annotation annotation) {
        removeFacetFromDomain(catalogueItemId, annotation.id, 'annotations')
    }

    void removeSemanticLinkFromCatalogueItem(UUID catalogueItemId, SemanticLink semanticLink) {
        removeFacetFromDomain(catalogueItemId, semanticLink.id, 'semanticLinks')
    }

    void removeModelExtendFromCatalogueItem(UUID catalogueItemId, ModelExtend modelExtend) {
        get(catalogueItemId).removeFromModelExtends(modelExtend)
    }      

    void removeModelImportFromCatalogueItem(UUID catalogueItemId, ModelImport modelImport) {
        get(catalogueItemId).removeFromModelImports(modelImport)
    }    

    void removeReferenceFileFromCatalogueItem(UUID catalogueItemId, ReferenceFile referenceFile) {
        removeFacetFromDomain(catalogueItemId, referenceFile.id, 'referenceFiles')
    }

    void removeRuleFromCatalogueItem(UUID catalogueItemId, Rule rule) {
        removeFacetFromDomain(catalogueItemId, rule.id, 'rules')
    }

    K copyCatalogueItemInformation(K original, K copy, User copier, UserSecurityPolicyManager userSecurityPolicyManager) {
        copy.createdBy = copier.emailAddress
        copy.label = original.label
        copy.description = original.description

        classifierService.findAllByCatalogueItemId(userSecurityPolicyManager, original.id).each {copy.addToClassifiers(it)}
        metadataService.findAllByCatalogueItemId(original.id).each {copy.addToMetadata(it.namespace, it.key, it.value, copier)}
        ruleService.findAllByCatalogueItemId(original.id).each {rule ->
            Rule copiedRule = new Rule(name: rule.name, description: rule.description, createdBy: copier.emailAddress)
            rule.ruleRepresentations.each {ruleRepresentation ->
                copiedRule.addToRuleRepresentations(language: ruleRepresentation.language,
                                                    representation: ruleRepresentation.representation,
                                                    createdBy: copier.emailAddress)
            }
            copy.addToRules(copiedRule)
        }

        semanticLinkService.findAllBySourceCatalogueItemId(original.id).each { link ->
            copy.addToSemanticLinks(createdBy: copier.emailAddress, linkType: link.linkType,
                                    targetCatalogueItemId: link.targetCatalogueItemId,
                                    targetCatalogueItemDomainType: link.targetCatalogueItemDomainType,
                                    unconfirmed: true)
        }

        copy
    }

    void setCatalogueItemRefinesCatalogueItem(CatalogueItem source, CatalogueItem target, User catalogueUser) {
        source.addToSemanticLinks(linkType: SemanticLinkType.REFINES, createdByUser: catalogueUser, targetCatalogueItem: target)
    }

    K updateFacetsAfterInsertingCatalogueItem(K catalogueItem) {
        if (catalogueItem.metadata) {
            catalogueItem.metadata.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            Metadata.saveAll(catalogueItem.metadata)
        }
        if (catalogueItem.rules) {
            catalogueItem.rules.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            Rule.saveAll(catalogueItem.rules)
        }
        if (catalogueItem.annotations) {
            catalogueItem.annotations.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            Annotation.saveAll(catalogueItem.annotations)
        }
        if (catalogueItem.semanticLinks) {
            catalogueItem.semanticLinks.each {
                if (!it.isDirty('catalogueItemId')) it.trackChanges()
                it.catalogueItemId = catalogueItem.getId()
            }
            SemanticLink.saveAll(catalogueItem.semanticLinks)
        }
        if (catalogueItem.referenceFiles) {
            catalogueItem.referenceFiles.each {
                if (!it.isDirty()) it.trackChanges()
                it.beforeValidate()
            }
            ReferenceFile.saveAll(catalogueItem.referenceFiles)
        }      
        catalogueItem.breadcrumbTree?.trackChanges()
        catalogueItem.breadcrumbTree?.beforeValidate()
        catalogueItem.breadcrumbTree?.save(validate: false)
        catalogueItem
    }

    K checkFacetsAfterImportingCatalogueItem(K catalogueItem) {
        if (catalogueItem.metadata) {
            catalogueItem.metadata.each {
                it.catalogueItem = catalogueItem
                it.createdBy = it.createdBy ?: catalogueItem.createdBy
            }
        }
        if (catalogueItem.rules) {
            catalogueItem.rules.each {
                it.catalogueItem = catalogueItem
                it.createdBy = it.createdBy ?: catalogueItem.createdBy
            }
        }
        if (catalogueItem.annotations) {
            catalogueItem.annotations.each {
                it.catalogueItem = catalogueItem
                it.createdBy = it.createdBy ?: catalogueItem.createdBy
            }
        }
        if (catalogueItem.semanticLinks) {
            catalogueItem.semanticLinks.each {
                it.catalogueItem = catalogueItem
                it.createdBy = it.createdBy ?: catalogueItem.createdBy
            }
        }
        if (catalogueItem.referenceFiles) {
            catalogueItem.referenceFiles.each {
                it.catalogueItem = catalogueItem
                it.createdBy = it.createdBy ?: catalogueItem.createdBy
            }
        }
        catalogueItem
    }

    /*
     * Find a CatalogueItem which is labeled with label and whose parent is parentCatalogueItem.
     * @param parentCatalogueItem The CatalogueItem which is the parent of the CatalogueItem being sought
     * @param label The label of the CatalogueItem being sought
     */

    K findByParentAndLabel(CatalogueItem parentCatalogueItem, String label) {
        null
    }

    void removeFacetFromDomain(UUID domainId, UUID facetId, String facetProperty) {
        PersistentEntity persistentEntity = getPersistentEntity()
        JoinTable joinTable = getJoinTable(persistentEntity, facetProperty)
        Table domainEntityTable = getDomainEntityTable(persistentEntity)
        sessionFactory.currentSession
            .createSQLQuery("DELETE FROM ${domainEntityTable.schema}.${joinTable.name} " +
                            "WHERE ${joinTable.key.name} = :domainId " +
                            "AND ${joinTable.column.name} = :facetId")
            .setParameter('domainId', domainId)
            .setParameter('facetId', facetId)
            .executeUpdate()
        log.warn('stop')

    }

    void deleteAllFacetsByCatalogueItemId(UUID catalogueItemId, String queryToDeleteFromJoinTable) {
        sessionFactory.currentSession
            .createSQLQuery(queryToDeleteFromJoinTable)
            .setParameter('id', catalogueItemId)
            .executeUpdate()

        deleteAllFacetDataByCatalogueItemIds([catalogueItemId])
    }

    void deleteAllFacetsByCatalogueItemIds(List<UUID> catalogueItemIds, String queryToDeleteFromJoinTable) {
        sessionFactory.currentSession
            .createSQLQuery(queryToDeleteFromJoinTable)
            .setParameter('ids', catalogueItemIds)
            .executeUpdate()

        deleteAllFacetDataByCatalogueItemIds catalogueItemIds
    }

    void deleteAllFacetDataByCatalogueItemIds(List<UUID> catalogueItemIds) {
        annotationService.deleteAllByCatalogueItemIds(catalogueItemIds)
        metadataService.deleteAllByCatalogueItemIds(catalogueItemIds)
        referenceFileService.deleteAllByCatalogueItemIds(catalogueItemIds)
        ruleService.deleteAllByCatalogueItemIds(catalogueItemIds)
        semanticLinkService.deleteAllByCatalogueItemIds(catalogueItemIds)
    }

    void additionalModelImports(User currentUser, ModelImport imported) {
        //no-op
    }
}