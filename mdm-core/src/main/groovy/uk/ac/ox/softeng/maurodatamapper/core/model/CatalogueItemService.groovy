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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFileService
import uk.ac.ox.softeng.maurodatamapper.core.facet.Rule
import uk.ac.ox.softeng.maurodatamapper.core.facet.RuleService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkService
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import grails.core.GrailsClass
import org.grails.datastore.gorm.GormEntity
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

abstract class CatalogueItemService<K extends CatalogueItem> {

    @Autowired
    GrailsApplication grailsApplication

    ClassifierService classifierService
    MetadataService metadataService
    RuleService ruleService
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

    abstract void deleteAll(Collection<K> catalogueItems)

    abstract void delete(K catalogueItem)

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

    abstract K get(Serializable id)

    abstract List<K> list(Map args)

    /**
     * Use domain.getAll(ids) to retrieve objects from the database.
     *
     * Make sure you use findAll() on the output of this, its possible to get ids which dont exist in this domain and the Grails implementation
     * of getAll(ids) will return a list of null elements
     * @param ids
     * @return
     */
    abstract List<K> getAll(Collection<UUID> ids)

    abstract boolean hasTreeTypeModelItems(K catalogueItem, boolean forDiff)

    abstract List<ModelItem> findAllTreeTypeModelItemsIn(K catalogueItem, boolean forDiff = false)

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
        get(catalogueItemId).removeFromClassifiers(classifier)
    }

    void removeMetadataFromCatalogueItem(UUID catalogueItemId, Metadata metadata) {
        get(catalogueItemId).removeFromMetadata(metadata)
    }

    void removeAnnotationFromCatalogueItem(UUID catalogueItemId, Annotation annotation) {
        get(catalogueItemId).removeFromAnnotations(annotation)
    }

    void removeSemanticLinkFromCatalogueItem(UUID catalogueItemId, SemanticLink semanticLink) {
        get(catalogueItemId).removeFromSemanticLinks(semanticLink)
    }

    void removeReferenceFileFromCatalogueItem(UUID catalogueItemId, ReferenceFile referenceFile) {
        get(catalogueItemId).removeFromReferenceFiles(referenceFile)
    }

    K copyCatalogueItemInformation(K original, K copy, User copier, UserSecurityPolicyManager userSecurityPolicyManager) {
        copy.createdBy = copier.emailAddress
        copy.label = original.label
        copy.description = original.description

        classifierService.findAllByCatalogueItemId(userSecurityPolicyManager, original.id).each { copy.addToClassifiers(it) }
        metadataService.findAllByCatalogueItemId(original.id).each { copy.addToMetadata(it.namespace, it.key, it.value, copier) }
        ruleService.findAllByCatalogueItemId(original.id).each { rule ->
            Rule copiedRule = new Rule(name: rule.name, description: rule.description, createdBy: copier.emailAddress)
            rule.ruleRepresentations.each { ruleRepresentation ->
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
}