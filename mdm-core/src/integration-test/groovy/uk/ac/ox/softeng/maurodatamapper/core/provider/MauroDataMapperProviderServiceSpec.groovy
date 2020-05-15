package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.test.MdmSpecification

import grails.core.GrailsApplication
import grails.testing.mixin.integration.Integration
import org.springframework.context.MessageSource

/**
 * @since 13/10/2017
 */
@Integration
class MauroDataMapperProviderServiceSpec extends MdmSpecification {

    MessageSource messageSource
    GrailsApplication grailsApplication
    MauroDataMapperProviderService mauroDataMapperProviderService

    void 'test modules'() {
        expect:
        mauroDataMapperProviderService.modulesList.size() == 82
        mauroDataMapperProviderService.javaModules.size() == 60
        mauroDataMapperProviderService.allGrailsPluginModules.size() == 21
        mauroDataMapperProviderService.grailsPluginModules.size() == 20
        mauroDataMapperProviderService.mdmGrailsPluginModules.size() == 1
        mauroDataMapperProviderService.otherModules.size() == 1

        and:
        mauroDataMapperProviderService.findModule('Core', grailsApplication.metadata.getApplicationVersion())
        mauroDataMapperProviderService.findModule('Common', '4.0.0-SNAPSHOT')
    }
}
