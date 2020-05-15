package uk.ac.ox.softeng.maurodatamapper.test


import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.test.unit.security.TestUser

import grails.testing.spock.OnceBefore
import grails.validation.Validateable
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.spockframework.util.Assert
import org.springframework.context.MessageSource
import spock.lang.Specification

/**
 * @since 08/10/2019
 */
@Slf4j
@CompileStatic
abstract class MdmSpecification extends Specification {

    abstract MessageSource getMessageSource()

    static UUID adminId
    static UUID editorId
    static UUID reader1Id
    static UUID reader2Id
    static UUID pendingId

    @OnceBefore
    def setupUserIds() {
        log.debug('Setup User Ids')
        adminId = UUID.randomUUID()
        editorId = UUID.randomUUID()
        reader1Id = UUID.randomUUID()
        reader2Id = UUID.randomUUID()
        pendingId = UUID.randomUUID()
    }

    def setup() {
        log.warn('--- {} --- {} ---', getClass().simpleName, specificationContext.currentIteration.name)
    }

    void check(GormEntity domainObj) {
        if (!domainObj) Assert.fail('No domain object to save')

        boolean valid = domainObj.validate()

        if (!valid) {
            outputDomainErrors(domainObj)
            Assert.fail("Domain object is not valid. Has ${domainObj.errors.errorCount} errors")
        }
    }

    void check(Validateable domainObj) {
        if (!domainObj) Assert.fail('No domain object to save')

        boolean valid = domainObj.validate()

        if (!valid) {
            outputDomainErrors(domainObj)
            Assert.fail("Domain object is not valid. Has ${domainObj.errors.errorCount} errors")
        }
    }

    void checkAndSave(GormEntity domainObj) {
        check(domainObj)
        save(domainObj)
    }

    void checkAndSave(GormEntity... domainObjs) {
        domainObjs.each {checkAndSave(it)}
    }

    def save(GormEntity domainObj) {
        domainObj.save(failOnError: true, validate: false, flush: true) ? true : false
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    def outputDomainErrors(def domainObj) {
        log.error 'Errors validating domain: {}', domainObj.class.simpleName
        System.err.println 'Errors validating domain: ' + domainObj.class.simpleName
        domainObj.errors.allErrors.each {error ->

            String msg = messageSource ? messageSource.getMessage(error, Locale.default) :
                         "${error.defaultMessage} :: ${Arrays.asList(error.arguments)}"

            // if (error instanceof FieldError) msg += " :: [${error.field}]"

            log.error msg
            System.err.println msg
        }
    }

    User getAdmin() {
        new TestUser(emailAddress: 'admin@maurodatamapper.com',
                     firstName: 'Admin',
                     lastName: 'User',
                     organisation: 'Oxford BRC Informatics',
                     jobTitle: 'God',
                     id: adminId)
    }

    User getEditor() {
        new TestUser(emailAddress: 'editor@test.com',
                     firstName: 'editor', lastName: 'User', id: editorId)
    }

    User getPending() {
        new TestUser(emailAddress: 'pending@test.com',
                     firstName: 'pending', lastName: 'User', id: pendingId)
    }

    User getReader1() {
        new TestUser(emailAddress: 'reader1@test.com',
                     firstName: 'reader1', lastName: 'User', id: reader1Id)
    }

    User getReader2() {
        new TestUser(emailAddress: 'reader2@test.com',
                     firstName: 'reader2', lastName: 'User', id: reader2Id)
    }
}
