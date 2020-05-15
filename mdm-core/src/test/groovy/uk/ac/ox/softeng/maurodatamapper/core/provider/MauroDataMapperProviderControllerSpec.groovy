package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.provider.plugin.AbstractMauroDataMapperPlugin
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.web.controllers.ControllerUnitTest

class MauroDataMapperProviderControllerSpec extends BaseUnitSpec implements ControllerUnitTest<MauroDataMapperProviderController> {

    void 'test get modules'() {
        given:
        controller.mauroDataMapperProviderService = Mock(MauroDataMapperProviderService) {
            getModulesList() >> [[getName: 'test', getVersion: '1.0'] as AbstractMauroDataMapperPlugin,
                                 [getName: 'test2', getVersion: '0.2'] as AbstractMauroDataMapperPlugin]
        }

        when:
        controller.modules()

        then:
        response.json
        response.json[0].name == 'test'
        response.json[0].version == '1.0'
        response.json[1].name == 'test2'
        response.json[1].version == '0.2'
    }

}