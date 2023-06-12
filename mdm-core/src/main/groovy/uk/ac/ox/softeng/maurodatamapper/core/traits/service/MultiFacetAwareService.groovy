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
package uk.ac.ox.softeng.maurodatamapper.core.traits.service

import uk.ac.ox.softeng.maurodatamapper.core.diff.DiffCache
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
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
import uk.ac.ox.softeng.maurodatamapper.core.facet.rule.RuleRepresentation
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model.CopyInformation
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import groovy.transform.SelfType
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.orm.hibernate.cfg.JoinTable
import org.grails.orm.hibernate.cfg.Mapping
import org.grails.orm.hibernate.cfg.PropertyConfig
import org.grails.orm.hibernate.cfg.Table
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 17/03/2021
 */
@SelfType(MdmDomainService)
@Slf4j
trait MultiFacetAwareService<K extends MultiFacetAware> {

    @Autowired(required = false)
    Set<MultiFacetItemAwareService> multiFacetItemAwareServices

    abstract GrailsApplication getGrailsApplication()

    abstract MetadataService getMetadataService()

    abstract RuleService getRuleService()

    abstract SemanticLinkService getSemanticLinkService()

    abstract AnnotationService getAnnotationService()

    abstract ReferenceFileService getReferenceFileService()

    abstract SessionFactory getSessionFactory()

    abstract Class<K> getMultiFacetAwareClass()

    abstract List<K> findAllByMetadataNamespace(String namespace, Map pagination = [:])

    abstract List<K> findAllByMetadataNamespaceAndKey(String namespace, String key, Map pagination = [:])

    abstract boolean isMultiFacetAwareFinalised(K multiFacetAwareItem)

    void removeMetadataFromMultiFacetAware(UUID multiFacetAwareId, Metadata metadata) {
        removeFacetFromDomain(multiFacetAwareId, metadata.id, 'metadata')
    }

    void removeAnnotationFromMultiFacetAware(UUID multiFacetAwareId, Annotation annotation) {
        removeFacetFromDomain(multiFacetAwareId, annotation.id, 'annotations')
    }

    void removeSemanticLinkFromMultiFacetAware(UUID multiFacetAwareId, SemanticLink semanticLink) {
        removeFacetFromDomain(multiFacetAwareId, semanticLink.id, 'semanticLinks')
    }

    void removeReferenceFileFromMultiFacetAware(UUID multiFacetAwareId, ReferenceFile referenceFile) {
        removeFacetFromDomain(multiFacetAwareId, referenceFile.id, 'referenceFiles')
    }

    void removeRuleFromMultiFacetAware(UUID multiFacetAwareId, Rule rule) {
        removeFacetFromDomain(multiFacetAwareId, rule.id, 'rules')
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
    }

    PersistentEntity getPersistentEntity() {
        grailsApplication.mappingContext.getPersistentEntity(getMultiFacetAwareClass().name)
    }

    JoinTable getJoinTable(PersistentEntity persistentEntity, String facetProperty) {
        PropertyConfig propertyConfig = persistentEntity.getPropertyByName(facetProperty).mapping.mappedForm as PropertyConfig
        propertyConfig.joinTable
    }

    Table getDomainEntityTable(PersistentEntity persistentEntity) {
        Mapping mapping = persistentEntity.mapping.mappedForm as Mapping
        mapping.table
    }

    K checkFacetsAfterImportingMultiFacetAware(K multiFacetAware) {
        if (multiFacetAware.metadata) {
            multiFacetAware.metadata.each {
                it.multiFacetAwareItem = multiFacetAware
                it.createdBy = it.createdBy ?: multiFacetAware.createdBy
            }
        }
        if (multiFacetAware.rules) {
            multiFacetAware.rules.each {
                it.multiFacetAwareItem = multiFacetAware
                it.createdBy = it.createdBy ?: multiFacetAware.createdBy
                if (it.ruleRepresentations) {
                    it.ruleRepresentations.each {RuleRepresentation representation ->
                        representation.createdBy = representation.createdBy ?: it.createdBy ?: multiFacetAware.createdBy
                    }
                }
            }
        }
        if (multiFacetAware.annotations) {
            multiFacetAware.annotations.each {
                it.multiFacetAwareItem = multiFacetAware
                it.createdBy = it.createdBy ?: multiFacetAware.createdBy
            }
        }
        if (multiFacetAware.semanticLinks) {
            multiFacetAware.semanticLinks.each {
                it.multiFacetAwareItem = multiFacetAware
                it.createdBy = it.createdBy ?: multiFacetAware.createdBy
            }
        }
        if (multiFacetAware.referenceFiles) {
            multiFacetAware.referenceFiles.each {
                it.multiFacetAwareItem = multiFacetAware
                it.createdBy = it.createdBy ?: multiFacetAware.createdBy
            }
        }
        multiFacetAware
    }

    void deleteAllFacetsByMultiFacetAwareId(UUID multiFacetAwareId, String queryToDeleteFromJoinTable) {
        sessionFactory.currentSession
            .createSQLQuery(queryToDeleteFromJoinTable)
            .setParameter('id', multiFacetAwareId)
            .executeUpdate()

        deleteAllFacetDataByMultiFacetAwareIds([multiFacetAwareId])
    }

    void deleteAllFacetsByMultiFacetAwareIds(List<UUID> multiFacetAwareIds, String queryToDeleteFromJoinTable) {

        Utils.executeInBatches(multiFacetAwareIds, {ids ->
            sessionFactory.currentSession
                .createSQLQuery(queryToDeleteFromJoinTable)
                .setParameter('ids', ids)
                .executeUpdate()

            deleteAllFacetDataByMultiFacetAwareIds ids
        })
    }

    void deleteAllFacetDataByMultiFacetAwareIds(List<UUID> multiFacetAwareIds) {
        annotationService.deleteAllByMultiFacetAwareItemIds(multiFacetAwareIds)
        metadataService.deleteAllByMultiFacetAwareItemIds(multiFacetAwareIds)
        referenceFileService.deleteAllByMultiFacetAwareItemIds(multiFacetAwareIds)
        ruleService.deleteAllByMultiFacetAwareItemIds(multiFacetAwareIds)
        semanticLinkService.deleteAllByMultiFacetAwareItemIds(multiFacetAwareIds)
    }

    CopyInformation cacheFacetInformationForCopy(List<UUID> originalIds, CopyInformation copyInformation = null) {
        // If no ids or only 1 id then caching is not of any advantage
        if (!originalIds || originalIds.size() == 1) return copyInformation
        CopyInformation cachedInformation = copyInformation ?: new CopyInformation()
        List<Metadata> md = Metadata.byMultiFacetAwareItemIdInList(originalIds).list()
        cachedInformation.preloadedFacets.metadata = new TreeMap(md.groupBy { it.multiFacetAwareItemId })
        List<Rule> r = Rule.byMultiFacetAwareItemIdInList(originalIds).list()
        cachedInformation.preloadedFacets.rules = new TreeMap(r.groupBy { it.multiFacetAwareItemId })
        List<SemanticLink> sl = SemanticLink.byMultiFacetAwareItemIdInList(originalIds).list()
        cachedInformation.preloadedFacets.semanticLinks = new TreeMap(sl.groupBy { it.multiFacetAwareItemId })
        cachedInformation
    }

    @SuppressWarnings('GroovyAssignabilityCheck')
    Map<String, Map<UUID, List<Diffable>>> loadAllDiffableFacetsIntoMemoryByIds(List<UUID> multiFacetAwareItemIds) {
        Map<String, Map<UUID, List<Diffable>>> data = [:]
        multiFacetItemAwareServices.each {service ->
            if (Utils.parentClassIsAssignableFromChild(Diffable, service.getDomainClass())) {
                String facet = service.getDomainClass().simpleName
                log.debug('Loading facet type {} for {} ids', facet, multiFacetAwareItemIds.size())
                Map<UUID, List<Diffable>> facetData = [:]
                if (multiFacetAwareItemIds) {
                    facetData = service.findAllByMultiFacetAwareItemIdInList(multiFacetAwareItemIds).groupBy {MultiFacetItemAware mfia -> mfia.multiFacetAwareItemId}
                }
                data[facet] = facetData
            }
        }
        data
    }

    void addFacetDataToDiffCache(DiffCache diffCache, Map<String, Map<UUID, List<Diffable>>> facetData, UUID catalogueItemId) {
        diffCache.addField('metadata', facetData.Metadata[catalogueItemId])
        diffCache.addField('annotations', facetData.Annotation[catalogueItemId])
        diffCache.addField('rules', facetData.Rule[catalogueItemId])
    }
}
