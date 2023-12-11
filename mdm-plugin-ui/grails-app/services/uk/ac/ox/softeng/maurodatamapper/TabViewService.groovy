package uk.ac.ox.softeng.maurodatamapper

import grails.gorm.transactions.Transactional
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.ox.softeng.maurodatamapper.ui.providers.TabViewUIProviderService

@Transactional
class TabViewService {

    @Autowired(required = false)

    Set<TabViewUIProviderService> tabViewProviderServices


    List getTabViewProviderServices(String domainType, UUID itemId) {
        System.err.println("Here")
        tabViewProviderServices.collect {service ->
            System.err.println(service)
            service.providedTabViews(domainType, itemId).collect {tabName ->
                [   "tabName" : tabName,
                    "pluginNamespace": service.namespace,
                    "pluginName": service.name,
                    "pluginVersion": service.version
                ]
            }
        }.flatten()
    }

    byte[] tabView(String itemId, String domainType, String pluginNamespace, String pluginName, String pluginVersion, String tabName) {
        tabViewProviderServices.find {service ->
            service.namespace == pluginNamespace &&
                service.name == pluginName &&
                service.version == pluginVersion
        }.getJSWebComponent(tabName, UUID.fromString(itemId), domainType)
    }

}
