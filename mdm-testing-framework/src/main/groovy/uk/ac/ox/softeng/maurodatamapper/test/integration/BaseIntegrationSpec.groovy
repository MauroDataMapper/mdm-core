package uk.ac.ox.softeng.maurodatamapper.test.integration

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.LuceneIndexingService
import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.hibernate.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

@Slf4j
abstract class BaseIntegrationSpec extends MdmSpecification {

    @Autowired
    GrailsApplication grailsApplication

    @Autowired
    MessageSource messageSource

    @Autowired
    SessionFactory sessionFactory

    @Autowired
    LuceneIndexingService luceneIndexingService

    UUID id

    Folder folder

    abstract void setupDomainData()

    void setupData() {

        // Remove any indexes which currently exist from previous tests
        luceneIndexingService.purgeAllIndexes()

        preDomainDataSetup()

        setupDomainData()

        postDomainDataSetup()

        // Flush all the new data indexes to the files
        luceneIndexingService.flushIndexes()

        // This log marker allows us to ignore all the inserts and DB queries in the logs prior,
        // thus allowing analysis of the SQL actioned for each test
        log.debug("==> Test data setup and inserted into database <==")
    }

    void preDomainDataSetup() {
        // No-op allows any extending classes to perform actions
    }

    void postDomainDataSetup() {
        // No-op allows any extending classes to perform actions
    }
}
