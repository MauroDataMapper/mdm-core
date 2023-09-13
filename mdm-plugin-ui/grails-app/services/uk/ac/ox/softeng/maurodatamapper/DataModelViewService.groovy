package uk.ac.ox.softeng.maurodatamapper

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.ui.providers.DataModelViewUIProviderService

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired

@Transactional
class DataModelViewService {

    @Autowired(required = false)
    Set<DataModelViewUIProviderService> dataModelViewProviderServices


    List<Map> getDataModelViewProviderServicesForDataModel(DataModel dataModel) {
        return dataModelViewProviderServices.findAll {
            it.providesDataModelView (dataModel)
        }.collect {
            [   "tabName" : it.tabName,
                "pluginNamespace": it.namespace,
                "pluginName": it.name,
                "pluginVersion": it.version
            ]
        }

    }

    byte[] dataModelView(String dataModelId, String pluginNamespace, String pluginName, String pluginVersion) {
        dataModelViewProviderServices.find {service ->
            service.namespace == pluginNamespace &&
                service.name == pluginName &&
                service.version == pluginVersion
        }.getJSWebComponent(UUID.fromString(dataModelId))
    }

}
