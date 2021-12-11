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
package uk.ac.ox.softeng.maurodatamapper.core.traits.service

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
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware

import grails.core.GrailsApplication
import groovy.transform.SelfType
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.orm.hibernate.cfg.JoinTable
import org.grails.orm.hibernate.cfg.Mapping
import org.grails.orm.hibernate.cfg.PropertyConfig
import org.grails.orm.hibernate.cfg.Table
import org.hibernate.SessionFactory

/**
 * @since 17/03/2021
 */
@SelfType(DomainService)
@Slf4j
trait MultiFacetAwareService<K extends MultiFacetAware> {

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

    abstract boolean isMultiFacetAwareFinalised (K multiFacetAwareItem)

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

    K updateFacetsAfterInsertingMultiFacetAware(K multiFacetAware) {
        if (multiFacetAware.metadata) {
            multiFacetAware.metadata.each {
                if (!it.isDirty('multiFacetAwareItemId')) it.trackChanges()
                it.multiFacetAwareItemId = multiFacetAware.id
                it.multiFacetAwareItemDomainType = it.domainType
            }
            Metadata.saveAll(multiFacetAware.metadata)
        }
        if (multiFacetAware.rules) {
            multiFacetAware.rules.each {
                if (!it.isDirty('multiFacetAwareItemId')) it.trackChanges()
                it.multiFacetAwareItemId = multiFacetAware.id
                it.multiFacetAwareItemDomainType = it.domainType
            }
            Rule.saveAll(multiFacetAware.rules)
        }
        if (multiFacetAware.annotations) {
            multiFacetAware.annotations.each {
                if (!it.isDirty('multiFacetAwareItemId')) it.trackChanges()
                it.multiFacetAwareItemId = multiFacetAware.id
                it.multiFacetAwareItemDomainType = it.domainType
            }
            Annotation.saveAll(multiFacetAware.annotations)
        }
        if (multiFacetAware.semanticLinks) {
            multiFacetAware.semanticLinks.each {
                if (!it.isDirty('multiFacetAwareItemId')) it.trackChanges()
                it.multiFacetAwareItemId = multiFacetAware.id
            }
            SemanticLink.saveAll(multiFacetAware.semanticLinks)
        }
        if (multiFacetAware.referenceFiles) {
            multiFacetAware.referenceFiles.each {
                if (!it.isDirty()) it.trackChanges()
                it.beforeValidate()
                it.multiFacetAwareItemId = multiFacetAware.id
            }
            ReferenceFile.saveAll(multiFacetAware.referenceFiles)
        }
        multiFacetAware
    }

    K checkFacetsAfterImportingMultiFacetAware(K multiFacetAware) {
        if (multiFacetAware.metadata) {
            multiFacetAware.metadata.each {
                it.multiFacetAwareItemId = multiFacetAware.id
                it.multiFacetAwareItemDomainType = it.domainType
                it.createdBy = it.createdBy ?: multiFacetAware.createdBy
            }
        }
        if (multiFacetAware.rules) {
            multiFacetAware.rules.each {
                it.multiFacetAwareItemId = multiFacetAware.id
                it.multiFacetAwareItemDomainType = it.domainType
                it.createdBy = it.createdBy ?: multiFacetAware.createdBy
            }
        }
        if (multiFacetAware.annotations) {
            multiFacetAware.annotations.each {
                it.multiFacetAwareItemId = multiFacetAware.id
                it.multiFacetAwareItemDomainType = it.domainType
                it.createdBy = it.createdBy ?: multiFacetAware.createdBy
            }
        }
        if (multiFacetAware.semanticLinks) {
            multiFacetAware.semanticLinks.each {
                it.multiFacetAwareItemId = multiFacetAware.id
                it.createdBy = it.createdBy ?: multiFacetAware.createdBy
            }
        }
        if (multiFacetAware.referenceFiles) {
            multiFacetAware.referenceFiles.each {
                it.multiFacetAwareItemId = multiFacetAware.id
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
        sessionFactory.currentSession
            .createSQLQuery(queryToDeleteFromJoinTable)
            .setParameter('ids', multiFacetAwareIds)
            .executeUpdate()

        deleteAllFacetDataByMultiFacetAwareIds multiFacetAwareIds
    }

    void deleteAllFacetDataByMultiFacetAwareIds(List<UUID> multiFacetAwareIds) {
        annotationService.deleteAllByMultiFacetAwareItemIds(multiFacetAwareIds)
        metadataService.deleteAllByMultiFacetAwareItemIds(multiFacetAwareIds)
        referenceFileService.deleteAllByMultiFacetAwareItemIds(multiFacetAwareIds)
        ruleService.deleteAllByMultiFacetAwareItemIds(multiFacetAwareIds)
        semanticLinkService.deleteAllByMultiFacetAwareItemIds(multiFacetAwareIds)
    }
}
