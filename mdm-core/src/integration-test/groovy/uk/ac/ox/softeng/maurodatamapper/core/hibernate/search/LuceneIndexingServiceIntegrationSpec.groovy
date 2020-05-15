package uk.ac.ox.softeng.maurodatamapper.core.hibernate.search

import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.LuceneIndexingService
import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import org.springframework.context.MessageSource

@Slf4j
@Integration
class LuceneIndexingServiceIntegrationSpec extends MdmSpecification {

    MessageSource messageSource
    LuceneIndexingService luceneIndexingService

    void 'test core lucene index directory'() {
        expect:
        luceneIndexingService.luceneIndexPath.toString() == '/tmp/lucene/core'
    }

    void 'test lucene default config mass indexer properties'() {
        expect:
        luceneIndexingService.massIndexerConfig == [typesToIndexInParallel: 1,
                                                    cacheMode             : 'IGNORE',
                                                    optimizeOnFinish      : true,
                                                    optimizeAfterPurge    : true,
                                                    purgeAllOnStart       : true,
                                                    transactionTimeout    : 1800,
                                                    threadsToLoadObjects  : 20,
                                                    batchSizeToLoadObjects: 1000,
                                                    idFetchSize           : -1]
    }
}
