package uk.ac.ox.softeng.maurodatamapper.ui

import uk.ac.ox.softeng.maurodatamapper.TabViewService
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel

class TabViewController {

    TabViewService tabViewService

	static responseFormats = ['json', 'xml']
	
    def tabViewProviders() {
        UUID itemId = UUID.fromString(params.itemId)
        String domainType = params.domainType?:''
        respond tabViewService.getTabViewProviderServices(domainType, itemId)
    }

    // get "/views/$pluginNamespace/$pluginName/$pluginVersion/$tabView" (controller: 'dataModelView', action: 'dataModelView')
    def tabView() {
        byte[] response = tabViewService.tabView(params.itemId, params.domainType, params.pluginNamespace, params.pluginName, params.pluginVersion, params.tabName)
        render(file: response, fileName: 'output.js', contentType: "application/javascript")
    }

}
