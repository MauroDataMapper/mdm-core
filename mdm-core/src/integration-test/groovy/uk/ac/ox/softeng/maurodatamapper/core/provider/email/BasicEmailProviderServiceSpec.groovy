package uk.ac.ox.softeng.maurodatamapper.core.provider.email

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import grails.testing.mixin.integration.Integration
import org.springframework.context.MessageSource

/**
 * @since 08/10/2019
 */
@Integration
class BasicEmailProviderServiceSpec extends MdmSpecification implements JsonComparer {

    MessageSource messageSource

    BasicEmailProviderService basicEmailProviderService

    void 'Confirm service info'() {
        expect:
        basicEmailProviderService.version == '1.0'
        basicEmailProviderService.providerType == 'EmailProvider'
        basicEmailProviderService.name == 'BasicEmailProviderService'
        basicEmailProviderService.displayName == 'Basic Email Provider'
        basicEmailProviderService.namespace == 'uk.ac.ox.softeng.maurodatamapper.core.provider.email'
    }

    void "Test sending email, will fail without credentials"() {

        when:
        def res = basicEmailProviderService.sendEmail('MDM', 'mdm@mdm.com', ['Ollie': 'ollie.freeman@gmail.com'], [:], 'Test', 'Hello')

        then:
        res instanceof String
        res.contains('error')
    }

}
