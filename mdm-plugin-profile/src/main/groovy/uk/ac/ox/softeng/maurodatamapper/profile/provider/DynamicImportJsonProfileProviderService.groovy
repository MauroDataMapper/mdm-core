package uk.ac.ox.softeng.maurodatamapper.profile.provider

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileFieldDataType
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import java.util.regex.Pattern

/**
 * @since 17/05/2022
 */
abstract class DynamicImportJsonProfileProviderService extends JsonProfileProviderService {

    public static final String IMPORT_NAMESPACE_PREFIX = 'import'
    public static final Pattern UUID_PATTERN = ~/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/

    UUID importingId

    DynamicImportJsonProfileProviderService() {
    }

    DynamicImportJsonProfileProviderService(UUID importingId, MetadataService metadataService) {
        this.importingId = importingId
        this.metadataService = metadataService
    }

    abstract String getProfileNamespace()

    @Override
    String getNamespace() {
        "${IMPORT_NAMESPACE_PREFIX}.${importingId ?: 'NOT_ASSIGNED'}.${profileNamespace}"
    }

    @Override
    String getMetadataNamespace() {
        getNamespace()
    }

    boolean matchesMetadataNamespace(String fullMedataNamespace) {
        fullMedataNamespace.matches(/${IMPORT_NAMESPACE_PREFIX}\.${UUID_PATTERN}\.${profileNamespace}/)
    }

    DynamicImportJsonProfileProviderService generateDynamicProfileForId(UUID importingId) {
        getClass().newInstance(importingId, metadataService) as DynamicImportJsonProfileProviderService
    }

    UUID extractImportingId(String fullMedataNamespace) {
        if (!matchesMetadataNamespace(fullMedataNamespace)) return null
        String id = fullMedataNamespace.find(/${IMPORT_NAMESPACE_PREFIX}\.(${UUID_PATTERN})\.${profileNamespace}/) {it[1]}
        Utils.toUuid(id)
    }

    @Override
    JsonProfile getNewProfile() {
        EmptyJsonProfileFactory.instance.getEmptyProfile(this).addToSections {
            name = 'Importing Owner'
            description = 'Information about the domain which is importing'
            order = -1
            addToFields {
                fieldName = 'Id'
                metadataPropertyName = 'id'
                description = 'The id of the domain which is importing this domain'
                minMultiplicity = 1
                maxMultiplicity = 1
                dataType = ProfileFieldDataType.STRING
                if (importingId) {
                    uneditable = true
                    currentValue = importingId
                }
            }
        } as JsonProfile
    }
}
