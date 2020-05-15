package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

class MauroDataMapperProviderController implements ResourcelessMdmController {
    static responseFormats = ['json', 'xml']

    MauroDataMapperProviderService mauroDataMapperProviderService

    def modules() {
        respond modules: mauroDataMapperProviderService.modulesList
    }
}
