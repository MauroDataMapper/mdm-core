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
package uk.ac.ox.softeng.maurodatamapper.core.hibernate.search

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.LuceneIndexParameters
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.LuceneIndexAwareService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.hibernate.CacheMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity
import org.hibernate.search.mapper.orm.mapping.SearchMapping
import org.hibernate.search.mapper.orm.massindexing.MassIndexer
import org.hibernate.search.mapper.orm.massindexing.impl.LoggingMassIndexingMonitor
import org.hibernate.search.mapper.orm.session.SearchSession
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan
import org.springframework.beans.factory.annotation.Autowired

import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
class HibernateSearchIndexingService {

    GrailsApplication grailsApplication

    @Autowired(required = false)
    Set<LuceneIndexAwareService> luceneIndexAwareServices

    SessionFactory sessionFactory

    Map getHibernateSearchConfig() {
        Utils.getMapFromConfig(grailsApplication.config, 'hibernate.search')
    }

    Path getLuceneIndexPath() {
        String luceneDir = hibernateSearchConfig['backend.directory.root']
        Paths.get(luceneDir).toAbsolutePath().normalize()
    }

    Map getMassIndexerConfig() {
        Utils.cleanPrefixFromMap(hibernateSearchConfig, 'massindexer')
    }

    SearchSession getSearchSession() {
        Search.session(sessionFactory.currentSession)
    }

    void addEntityToIndexingPlan(Object entity) {
        SearchIndexingPlan indexingPlan = Search.session(sessionFactory.currentSession).indexingPlan()
        indexingPlan.addOrUpdate(entity)
    }

    void addEntitiesToIndexingPlan(Collection<Object> entities) {
        SearchIndexingPlan indexingPlan = Search.session(sessionFactory.currentSession).indexingPlan()
        entities.each {indexingPlan.addOrUpdate(it)}
    }

    void flushIndexes() {
        searchSession.indexingPlan().execute()
    }

    void purgeAllIndexes() {
        log.warn('Purging all existing indexes from lucene')
        Search.mapping(sessionFactory).allIndexedEntities().each {domain ->
            searchSession.workspace(domain.javaClass()).purge();
        }
    }

    void rebuildLuceneIndexes(LuceneIndexParameters indexParameters) {
        if (sessionFactory.currentSession) {
            log.info('Rebuilding indexes using current session')
            rebuildLuceneIndexes(indexParameters, sessionFactory.currentSession)
        } else {
            sessionFactory.openSession().withCloseable {session ->
                log.info('Rebuilding indexes using new session')
                rebuildLuceneIndexes(indexParameters, session)
            }
        }
    }

    void removeLuceneIndexDirectory() {
        log.warn('Removing Lucene Index Directory {}', luceneIndexPath)
        getLuceneIndexPath().toFile().deleteDir()
    }

    void rebuildLuceneIndexes(LuceneIndexParameters indexParameters, Session session) {

        // Update from config default parameters
        indexParameters.updateFromMap(getMassIndexerConfig())

        log.warn('Lucene Indexes are being rebuilt, searches will not work')

        SearchSession searchSession = Search.session(session)
        SearchMapping searchMapping = Search.mapping(session.sessionFactory)

        luceneIndexAwareServices.each {it.beforeRebuild(session)}

        log.info("Using ${indexParameters}")
        try {

            Collection<SearchIndexedEntity> indexedEntities = searchMapping.allIndexedEntities()
            log.debug('Reindexing entities {}', indexedEntities.collect {it.jpaName()}.sort())

            // All indexed classes must be added here
            MassIndexer indexer = searchSession.massIndexer(indexedEntities.collect {it.javaClass()})
                .monitor(new LoggingMassIndexingMonitor(1))
                .typesToIndexInParallel(indexParameters.typesToIndexInParallel)
                .threadsToLoadObjects(indexParameters.threadsToLoadObjects)
                .batchSizeToLoadObjects(indexParameters.batchSizeToLoadObjects)
                .cacheMode(CacheMode.interpretExternalSetting(indexParameters.cacheMode))
                .mergeSegmentsOnFinish(indexParameters.optimizeOnFinish)
                .mergeSegmentsAfterPurge(indexParameters.optimizeAfterPurge)
                .purgeAllOnStart(indexParameters.purgeAllOnStart)
                .transactionTimeout(indexParameters.transactionTimeout)

            if (indexParameters.idFetchSize != -1) indexer.idFetchSize(indexParameters.idFetchSize)

            indexer.startAndWait()

            log.warn('Lucene Indexes rebuilt')
        } finally {
            luceneIndexAwareServices.each {it.afterRebuild()}
        }
    }
}
