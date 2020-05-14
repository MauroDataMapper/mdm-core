package uk.ac.ox.softeng.maurodatamapper.provider

import uk.ac.ox.softeng.maurodatamapper.provider.plugin.MauroDataMapperPlugin

import org.slf4j.Logger
import org.slf4j.LoggerFactory

trait MauroDataMapperService implements MauroDataMapperProvider {

    MauroDataMapperPlugin plugin

    abstract String getDisplayName()

    abstract String getProviderType()

    @Override
    String getName() {
        getClass().getSimpleName()
    }

    String getNamespace() {
        getClass().getPackage().getName()
    }

    Boolean allowsExtraMetadataKeys() {
        true
    }

    Set<String> getKnownMetadataKeys() {
        [] as HashSet
    }

    Logger getLog() {
        LoggerFactory.getLogger(getClass())
    }

    @Override
    String toString() {
        "${namespace} : ${name} : ${version}"
    }
}
