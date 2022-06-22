/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.profile.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileFieldDataType
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.springframework.beans.factory.annotation.Autowired

import java.util.regex.Pattern

/**
 * @since 17/05/2022
 */
abstract class DynamicImportJsonProfileProviderService extends JsonProfileProviderService {

    public static final String IMPORT_NAMESPACE_PREFIX = 'import'
    public static final Pattern UUID_PATTERN = ~/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}/
    public static final String IMPORTING_SECTION_NAME = 'Importing Owner'

    UUID importingId
    String importingDomainType
    Path importingPath
    boolean includeImportOwnerSection

    @Autowired
    PathService pathService

    DynamicImportJsonProfileProviderService() {
        includeImportOwnerSection = true
    }

    @Override
    String getDisplayName() {
        if (importingPath) return "$displayNamePrefix for $importingPath"
        if (importingId) return "$displayNamePrefix for $importingDomainType:$importingId"
        "Unassigned $displayNamePrefix"
    }

    abstract String getDisplayNamePrefix()

    abstract String getProfileNamespace()

    @Override
    String getNamespace() {
        "${IMPORT_NAMESPACE_PREFIX}.${importingId ?: 'NOT_ASSIGNED'}.${profileNamespace}"
    }

    @Override
    String getMetadataNamespace() {
        getNamespace()
    }

    @Override
    Boolean canBeEditedAfterFinalisation() {
        true
    }

    boolean matchesMetadataNamespace(String fullMedataNamespace) {
        fullMedataNamespace.matches(/${IMPORT_NAMESPACE_PREFIX}\.${UUID_PATTERN}\.${profileNamespace}/)
    }

    DynamicImportJsonProfileProviderService generateDynamicProfileForImportingOwner(String namespace, Collection<Metadata> metadata) {
        // Identify the import Id and create a new PPS using the id
        String extractedImportingId = extractImportingId(namespace)
        String importingOwnerId = metadata.find {it.key == 'import_id'}?.value
        String importingOwnerDomainType = metadata.find {it.key == 'import_domainType'}?.value
        String importingOwnerPath = metadata.find {it.key == 'import_path'}?.value

        if (importingOwnerId != extractedImportingId) {
            throw new ApiBadRequestException('DIJPPS',
                                             "Extracted import owner ID [${extractedImportingId}] from profile namespace does not match import owner id " +
                                             "[${importingOwnerId}] stored in profile")
        }

        clone().tap {
            importingId = Utils.toUuid(importingOwnerId)
            importingDomainType = importingOwnerDomainType
            importingPath = Path.from(importingOwnerPath)
        } as DynamicImportJsonProfileProviderService
    }

    DynamicImportJsonProfileProviderService generateDynamicProfileForImportingOwner(MdmDomain importingOwner, boolean includeImportOwnerSectionInProfile = true) {
        clone().tap {
            importingId = importingOwner.id
            importingDomainType = importingOwner.domainType
            importingPath = importingOwner.path
            includeImportOwnerSection = includeImportOwnerSectionInProfile
        } as DynamicImportJsonProfileProviderService
    }

    String extractImportingId(String fullMedataNamespace) {
        if (!matchesMetadataNamespace(fullMedataNamespace)) return null
        fullMedataNamespace.find(/${IMPORT_NAMESPACE_PREFIX}\.(${UUID_PATTERN})\.${profileNamespace}/) {it[1]}
    }

    @Override
    JsonProfile getNewProfile() {
        JsonProfile emptyProfile = EmptyJsonProfileFactory.instance.getEmptyProfile(this)
        if (includeImportOwnerSection) emptyProfile.addToSections {
            name = IMPORTING_SECTION_NAME
            description = 'Information about the domain which is importing'
            order = -1
            addToFields {
                fieldName = 'Id'
                metadataPropertyName = 'import_id'
                description = 'The id of the domain which is importing this domain. For a new Profile [Id & Domain Type] OR [Path] must be supplied.'
                minMultiplicity = 0
                maxMultiplicity = 1
                dataType = ProfileFieldDataType.STRING
                if (importingId) {
                    uneditable = true
                    currentValue = importingId
                }
            }
            addToFields {
                fieldName = 'Domain Type'
                metadataPropertyName = 'import_domainType'
                description = 'The domain type of the domain which is importing this domain. For a new Profile [Id & Domain Type] OR [Path] must be supplied.'
                minMultiplicity = 0
                maxMultiplicity = 1
                dataType = ProfileFieldDataType.STRING
                if (importingId) {
                    uneditable = true
                    currentValue = importingDomainType
                }
            }
            addToFields {
                fieldName = 'Path'
                metadataPropertyName = 'import_path'
                description = 'The path of the domain which is importing this domain. For a new Profile [Id & Domain Type] OR [Path] must be supplied.'
                minMultiplicity = 0
                maxMultiplicity = 1
                dataType = ProfileFieldDataType.STRING
                if (importingId) {
                    uneditable = true
                    currentValue = importingPath.toString()
                }
            }
            customFieldsValidation {fields, errors ->
                String idValue = fields.find {it.metadataPropertyName == 'import_id'}.currentValue
                String domainTypeValue = fields.find {it.metadataPropertyName == 'import_domainType'}.currentValue
                String pathValue = fields.find {it.metadataPropertyName == 'import_path'}.currentValue
                if (pathValue) return
                if (idValue && domainTypeValue) return
                if (!idValue) errors.rejectValue("fields[0].currentValue", 'import.group.must.be.supplied',
                                                 new Object[]{'currentValue', ProfileField.simpleName, null, 'Id', 'import_id'},
                                                 'For a new import profile [Id & Domain Type] OR [Path] must be supplied')
                if (!domainTypeValue) errors.rejectValue("fields[1].currentValue", 'import.group.must.be.supplied',
                                                         new Object[]{'currentValue', 'ProfileField.simpleName', null, 'Domain Type', 'import_domainType'},
                                                         'For a new import profile [Id & Domain Type] OR [Path] must be supplied')
                if (!pathValue) errors.rejectValue("fields[2].currentValue", 'import.group.must.be.supplied',
                                                   new Object[]{'currentValue', 'ProfileField.simpleName', null, 'Path', 'import_path'},
                                                   'For a new import profile [Id & Domain Type] OR [Path] must be supplied')
            }
        }
        emptyProfile
    }

    @Override
    JsonProfile storeProfileInEntity(MultiFacetAware entity, JsonProfile jsonProfile, String userEmailAddress, boolean isEntityFinalised) {
        // If this service is a "realised" PPS it will have the importing id set and so we can just use the standard way to store the profile
        if (importingId) {
            storeFieldInEntity(entity, importingId.toString(), 'import_id', userEmailAddress)
            storeFieldInEntity(entity, importingDomainType, 'import_domainType', userEmailAddress)
            storeFieldInEntity(entity, importingPath.toString(), 'import_path', userEmailAddress)
            return super.storeProfileInEntity(entity, jsonProfile, userEmailAddress, isEntityFinalised)
        }

        // Otherwise this is a dynamic "super" instance which needs to be "realised" from the profile using generateDynamicProfileForId
        ProfileSection profileSection = jsonProfile.find {it.name == IMPORTING_SECTION_NAME}
        ProfileField idField = profileSection.find {it.metadataPropertyName == 'import_id'}
        ProfileField domainTypeField = profileSection.find {it.metadataPropertyName == 'import_domainType'}
        ProfileField pathField = profileSection.find {it.metadataPropertyName == 'import_path'}
        MdmDomain importingOwner = null

        // Find the importing owner from the supplied profile information
        if (domainTypeField.currentValue && idField.currentValue) {
            importingOwner = metadataService.findMultiFacetAwareItemByDomainTypeAndId(domainTypeField.currentValue, Utils.toUuid(idField.currentValue)) as MdmDomain
        } else if (pathField.currentValue) {
            importingOwner = pathService.findResourceByPath(Path.from(pathField.currentValue))
        }

        if (!importingOwner) {
            throw new ApiBadRequestException('DIJPPS', 'Profile cannot be stored as no importing owner can be found')
        }

        // Generate a new dynamic PPS and use this to store the profile. This PPS will fall into the top of this method
        DynamicImportJsonProfileProviderService dynamicImportJsonProfileProviderService = generateDynamicProfileForImportingOwner(importingOwner)
        dynamicImportJsonProfileProviderService.storeProfileInEntity(entity, jsonProfile, userEmailAddress, isEntityFinalised)
    }

    @Override
    DynamicImportJsonProfileProviderService clone() throws CloneNotSupportedException {
        (super.clone() as DynamicImportJsonProfileProviderService).tap {
            pathService = owner.pathService
            importingId = owner.importingId
            importingDomainType = owner.importingDomainType
            importingPath = owner.importingPath
        }
    }
}
