package uk.ac.ox.softeng.maurodatamapper.core.provider

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

class MauroDataMapperServiceProviderController implements ResourcelessMdmController {
    static responseFormats = ['json', 'xml']

    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService

    def importerProviders() {
        respond([importerProviders: mauroDataMapperServiceProviderService.importerProviderServices])
    }

    def dataLoaderProviders() {
        respond dataLoaderProviders: mauroDataMapperServiceProviderService.dataLoaderProviderServices
    }

    def emailProviders() {
        respond emailProviders: mauroDataMapperServiceProviderService.emailProviderServices
    }

    def exporterProviders() {
        respond([exporterProviders: mauroDataMapperServiceProviderService.exporterProviderServices])
    }
}
