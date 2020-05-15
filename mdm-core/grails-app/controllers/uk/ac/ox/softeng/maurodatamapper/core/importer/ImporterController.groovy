package uk.ac.ox.softeng.maurodatamapper.core.importer

import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.ResourcelessMdmController

import static org.springframework.http.HttpStatus.BAD_REQUEST

class ImporterController implements ResourcelessMdmController {

    static responseFormats = ['json', 'xml']

    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService
    ImporterService importerService

    def parameters() {
        if (!params.ns || !params.name || !params.version) {
            return errorResponse(BAD_REQUEST, 'Namespace, name and version must be provided to identify individual importers')
        }

        ImporterProviderService importer = mauroDataMapperServiceProviderService.findImporterProvider(params.ns, params.name, params.version)

        if (!importer) return notFound("${params.ns}:${params.name}:${params.version}", ImporterProviderService)

        respond importerProviderService: importer, importParameterGroups: importerService.describeImporterParams(importer)
    }
}
