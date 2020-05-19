package uk.ac.ox.softeng.maurodatamapper.plugin.email.proxy

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer

import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

/**
 * @since 08/10/2019
 */
@Integration
class ProxyEmailProviderServiceSpec extends MdmSpecification implements JsonComparer {

    MessageSource messageSource
    @Autowired
    ProxyEmailProviderService proxyEmailProviderService

    void 'Confirm service info'() {
        expect:
        proxyEmailProviderService.version == '1.0'
        proxyEmailProviderService.providerType == 'EmailProvider'
        proxyEmailProviderService.name == 'ProxyEmailProviderService'
        proxyEmailProviderService.displayName == 'Proxy Email Provider'
        proxyEmailProviderService.namespace == 'uk.ac.ox.softeng.maurodatamapper.plugin.email.proxy'
    }

    void "Build basic message for special email service"() {
        given:
        proxyEmailProviderService.allProps.emailServiceUsername = 'svc_user'
        proxyEmailProviderService.allProps.emailServicePassword = 'svc_password'

        when:
        String msg = proxyEmailProviderService.buildMessage('MCD', 'mcd@mcd.com', ['test user': 'test@test.com'], [:], 'Test', 'Hello')

        then:
        verifyJson('''{
  "fromName": "MCD",
  "fromAddress": "mcd@mcd.com",
  "to": [{"test user": "test@test.com"}],
  "subject": "Test",
  "body": "Hello",
  "username": "svc_user",
  "password": "svc_password"
}''',
                   msg)
    }

    void "Build multiple to and cc basic message"() {
        given:
        proxyEmailProviderService.allProps.emailServiceUsername = 'svc_user'
        proxyEmailProviderService.allProps.emailServicePassword = 'svc_password'

        when:
        String msg = proxyEmailProviderService.buildMessage('MCD', 'mcd@mcd.com',
                                                            ['test user': 'test@test.com', 'test user1': 'test1@test.com'],
                                                            ['cc user': 'cc@test.com', 'cc user1': 'cc1@test.com'], 'Test', 'Hello')

        then:
        verifyJson('''{
  "fromName": "MCD",
  "fromAddress": "mcd@mcd.com",
  "to": [{"test user": "test@test.com"},{"test user1": "test1@test.com"}],
  "cc": [{"cc user": "cc@test.com"},{"cc user1": "cc1@test.com"}],
  "subject": "Test",
  "body": "Hello",
  "username": "svc_user",
  "password": "svc_password"
}''',
                   msg)
    }

    void "Test sending email, will fail without credentials"() {
        given:
        proxyEmailProviderService.allProps.emailServiceUrl = 'http://localhost'

        when:
        def res = proxyEmailProviderService.sendEmail('MCD', 'mcd@mcd.com', ['Ollie': 'ollie.freeman@gmail.com'], [:], 'Test', 'Hello')
        then:
        res != true
    }

}
