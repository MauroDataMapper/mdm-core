package uk.ac.ox.softeng.maurodatamapper.ui.providers

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel

abstract class DataModelViewUIProviderService extends UIProviderService {


    @Override
    String getProviderType() {
        'DataModelViewUIProvider'
    }

    boolean providesDataModelView(DataModel dataModel) {
        false
    }

    abstract String getTabName()

    abstract byte[] getJSWebComponent(UUID dataModelId)


}
