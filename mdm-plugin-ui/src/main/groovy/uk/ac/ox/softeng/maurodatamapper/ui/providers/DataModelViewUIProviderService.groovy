package uk.ac.ox.softeng.maurodatamapper.ui.providers

abstract class TabViewUIProviderService extends UIProviderService {


    @Override
    String getProviderType() {
        'TabViewUIProvider'
    }

    abstract List<String> providedTabViews(String domainType, UUID itemId)

    abstract byte[] getJSWebComponent(String tabName, UUID itemId, String domainType)


}
