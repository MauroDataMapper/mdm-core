package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import grails.testing.mixin.integration.Integration
import org.springframework.context.MessageSource

/**
 * Check the email returned with this plugin is the one provided by it
 */
@Integration
class MauroDataMapperServiceProvidingServiceSpec extends MdmSpecification {

    MessageSource messageSource
    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService

    void 'test email plugins'() {
        when:
        def results = mauroDataMapperServiceProviderService.emailProviderServices

        then: 'we have 2 email providers'
        results.size() == 2

        and:
        mauroDataMapperServiceProviderService.emailProvider.version == '1.0'
        mauroDataMapperServiceProviderService.emailProvider.providerType == 'EmailProvider'
        mauroDataMapperServiceProviderService.emailProvider.name == 'ProxyEmailProviderService'
        mauroDataMapperServiceProviderService.emailProvider.displayName == 'Proxy Email Provider'
        mauroDataMapperServiceProviderService.emailProvider.namespace == 'uk.ac.ox.softeng.maurodatamapper.plugin.email.proxy'
    }
}
