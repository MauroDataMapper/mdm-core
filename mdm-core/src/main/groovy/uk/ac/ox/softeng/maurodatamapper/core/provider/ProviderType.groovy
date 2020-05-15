package uk.ac.ox.softeng.maurodatamapper.core.provider


import uk.ac.ox.softeng.maurodatamapper.core.provider.dataloader.DataLoaderProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.email.EmailProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService

import com.google.common.base.CaseFormat

/**
 * @since 22/08/2017
 */
enum ProviderType {

    DATALOADER(DataLoaderProviderService),
    EMAIL(EmailProviderService),
    IMPORTER(ImporterProviderService),
    EXPORTER(ExporterProviderService)
    //PROFILE(ProfilePlugin)

    Class<? extends MauroDataMapperService> pluginTypeClass

    ProviderType(Class<? extends MauroDataMapperService> clazz) {
        this.pluginTypeClass = clazz
    }

    Class<? extends MauroDataMapperService> getPluginTypeClass() {
        pluginTypeClass
    }

    String getTypeName() {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name)
    }

    String getName() {
        pluginTypeClass.getSimpleName() - 'Service'
    }

    static List<String> getProviderTypeNames() {
        values().toList().collect {it.getTypeName()}
    }
}
