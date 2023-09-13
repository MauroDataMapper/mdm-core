package uk.ac.ox.softeng.maurodatamapper.ui

import uk.ac.ox.softeng.maurodatamapper.DataModelViewService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel

import grails.rest.*
import grails.converters.*

class DataModelViewController {

    DataModelViewService dataModelViewService

	static responseFormats = ['json', 'xml']
	
    def viewProviders() {
        UUID dataModelId = UUID.fromString(params.dataModelId)
        DataModel dataModel = DataModel.get(dataModelId)
        respond dataModelViewService.getDataModelViewProviderServicesForDataModel(dataModel)
    }

    // get "/views/$pluginNamespace/$pluginName/$pluginVersion" (controller: 'dataModelView', action: 'dataModelView')
    def dataModelView() {
        byte[] response = dataModelViewService.dataModelView(params.dataModelId, params.pluginNamespace, params.pluginName, params.pluginVersion)
        render(file: response, fileName: 'output.js', contentType: "application/javascript")
    }

}
