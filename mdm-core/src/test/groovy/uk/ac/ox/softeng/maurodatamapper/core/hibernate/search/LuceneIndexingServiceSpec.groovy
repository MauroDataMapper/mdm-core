package uk.ac.ox.softeng.maurodatamapper.core.hibernate.search

import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.LuceneIndexingService
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest

class LuceneIndexingServiceSpec extends BaseUnitSpec implements ServiceUnitTest<LuceneIndexingService> {

    void 'test core lucene index directory'() {
        expect:
        service.luceneIndexPath.toString() == '/tmp/lucene/core'
    }

    void 'test lucene default config mass indexer properties'() {
        expect:
        service.massIndexerConfig == [:]
    }
}
