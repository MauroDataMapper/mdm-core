package uk.ac.ox.softeng.maurodatamapper.test.unit

import uk.ac.ox.softeng.maurodatamapper.traits.domain.CreatorAware

import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError
import org.springframework.validation.FieldError

/**
 * @since 26/09/2017
 */
@Slf4j
abstract class CreatorAwareSpec<K extends CreatorAware> extends BaseUnitSpec {

    K item

    abstract void setValidDomainOtherValues()

    K findById() {
        domain.getClass().findById(domain.id)
    }

    abstract void verifyDomainOtherConstraints(K domain)

    abstract K getDomain()

    String getDefaultCreator() {
        admin.emailAddress
    }

    void setValidDomainValues() {
        setValidDomainOtherValues()
        domain.createdBy = defaultCreator
    }

    def 'CA1 : test valid instance'() {
        when:
        setValidDomainValues()

        then:
        checkAndSave(domain)
        domain.count() == 1

        when:
        item = findById()

        then:
        verifyDomainConstraints item
    }

    def 'CA2 : test createdby is only valid as email'() {

        when:
        setValidDomainValues()
        domain.createdBy = null
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.errorCount == 1

        when:
        FieldError error = domain.errors.getFieldError('createdBy')

        then:
        error.code == 'nullable'

        when:
        domain.createdBy = 'testing user'
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.errorCount == 1

        when:
        error = domain.errors.getFieldError('createdBy')

        then:
        error.code == 'email.invalid'

        when:
        domain.createdBy = admin.emailAddress

        then:
        check(domain)
        !domain.hasErrors()

    }

    void verifyDomainConstraints(K domain) {
        assert domain
        assert domain.lastUpdated
        assert domain.dateCreated
        assert domain.createdBy == defaultCreator
        verifyDomainOtherConstraints(domain)
    }
}