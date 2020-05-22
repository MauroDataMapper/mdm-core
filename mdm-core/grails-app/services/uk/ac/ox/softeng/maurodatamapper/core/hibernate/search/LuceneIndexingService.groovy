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
package uk.ac.ox.softeng.maurodatamapper.core.hibernate.search

import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.LuceneIndexParameters
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.LuceneIndexAwareService

import grails.core.GrailsApplication
import grails.plugins.hibernate.search.config.SearchMappingEntityConfig
import groovy.util.logging.Slf4j
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.hibernate.CacheMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.search.FullTextSession
import org.hibernate.search.MassIndexer
import org.hibernate.search.Search
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.annotation.AnnotationUtils

import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
class LuceneIndexingService {

    GrailsApplication grailsApplication

    @Autowired(required = false)
    Set<LuceneIndexAwareService> luceneIndexAwareServices

    SessionFactory sessionFactory

    Map getHibernateSearchConfig() {
        grailsApplication.config.hibernate.search
    }

    Path getLuceneIndexPath() {
        String luceneDir = hibernateSearchConfig.default.indexBase
        Paths.get(luceneDir).toAbsolutePath().normalize()
    }

    Map getMassIndexerConfig() {
        hibernateSearchConfig.massindexer ?: [:]
    }

    FullTextSession getFullTextSession() {
        Search.getFullTextSession(sessionFactory.currentSession)
    }

    void flushIndexes() {
        getFullTextSession().flushToIndexes()
    }

    void purgeAllIndexes() {
        log.warn('Purging all existing indexes from lucene')
        getIndexedDomains().each {domain ->
            getFullTextSession().purgeAll(domain)
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

        FullTextSession fullTextSession = Search.getFullTextSession(session)

        luceneIndexAwareServices.each {it.beforeRebuild(session)}

        log.info("Using ${indexParameters}")
        try {
            Class[] indexedDomains = getIndexedDomains().toArray() as Class[]

            // All indexed classes must be added here
            MassIndexer indexer = fullTextSession.createIndexer(indexedDomains)
                .progressMonitor(new SimpleIndexingProgressMonitor(1)) // reduces the amount of logging
                .typesToIndexInParallel(indexParameters.typesToIndexInParallel)
                .threadsToLoadObjects(indexParameters.threadsToLoadObjects)
                .batchSizeToLoadObjects(indexParameters.batchSizeToLoadObjects)
                .cacheMode(CacheMode.interpretExternalSetting(indexParameters.cacheMode))
                .optimizeOnFinish(indexParameters.optimizeOnFinish)
                .optimizeAfterPurge(indexParameters.optimizeAfterPurge)
                .purgeAllOnStart(indexParameters.purgeAllOnStart)
                .transactionTimeout(indexParameters.transactionTimeout)

            if (indexParameters.idFetchSize != -1) indexer.idFetchSize(indexParameters.idFetchSize)

            indexer.startAndWait()

            log.warn('Lucene Indexes rebuilt')
        } finally {
            luceneIndexAwareServices.each {it.afterRebuild()}
        }
    }

    List<Class> getIndexedDomains() {
        grailsApplication.getArtefacts(DomainClassArtefactHandler.TYPE).findAll {domainClass ->
            Class clazz = domainClass.getClazz()
            ClassPropertyFetcher.forClass(clazz).getStaticPropertyValue(SearchMappingEntityConfig.INDEX_CONFIG_NAME, Closure) ||
            AnnotationUtils.isAnnotationDeclaredLocally(Indexed, clazz)
        }.collect {it.clazz}

    }
}
