package uk.ac.ox.softeng.maurodatamapper.profile.provider

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService

/**
 * @since 17/05/2022
 */
abstract class DynamicImportJsonProfileProviderService extends JsonProfileProviderService {

    public static final String IMPORT_NAMESPACE_PREFIX = 'import'

    String importingId

    DynamicImportJsonProfileProviderService(String importingId, MetadataService metadataService) {
        this.importingId = importingId
        this.metadataService = metadataService
    }

    abstract String getProfileNamespace()

    @Override
    String getMetadataNamespace() {
        "${IMPORT_NAMESPACE_PREFIX}.${importingId}.${profileNamespace}"
    }
}
