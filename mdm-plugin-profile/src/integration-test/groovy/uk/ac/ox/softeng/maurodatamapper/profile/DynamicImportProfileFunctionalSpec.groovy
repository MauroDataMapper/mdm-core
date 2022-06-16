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
package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.profile.provider.DefaultDynamicImportJsonProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.provider.DefaultJsonProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.provider.DynamicImportJsonProfileProviderService
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.NO_CONTENT
import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

/**
 * @since 27/05/2022
 */
@Integration
@Slf4j
class DynamicImportProfileFunctionalSpec extends BaseFunctionalSpec {
    @Shared
    Folder folder

    @Shared
    UUID complexDataModelId

    @Shared
    UUID simpleFinalisedDataModelId

    @Shared
    UUID emptyDataClassId

    @Shared
    UUID importDataClassId

    @Shared
    UUID importDataElementId

    @Shared
    UUID importDataTypeId

    @Transactional
    Authority getTestAuthority() {
        Authority.findByDefaultAuthority(true)
    }

    DefaultJsonProfileProviderService profileSpecificationFieldProfileService
    DefaultDynamicImportJsonProfileProviderService importedDataElementDynamicProfileProviderService
    DefaultDynamicImportJsonProfileProviderService importedDataTypeDynamicProfileProviderService
    DefaultDynamicImportJsonProfileProviderService importedDataClassDynamicProfileProviderService

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')

        assert profileSpecificationFieldProfileService.displayName == 'Profile Specification Profile (Data Element)'
        assert importedDataElementDynamicProfileProviderService.displayName == 'Import Profile for DataElements'


        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        folder.addToMetadata(new Metadata(namespace: 'test.namespace', key: 'propertyKey', value: 'propertyValue', createdBy: FUNCTIONAL_TEST))
        checkAndSave(folder)

        DataModel complex = BootstrapModels.buildAndSaveComplexDataModel(messageSource, folder, testAuthority)
        DataModel finalised = BootstrapModels.buildAndSaveFinalisedSimpleDataModel(messageSource, folder, testAuthority)

        DataClass empty = complex.dataClasses.find {it.label == 'emptyclass'}
        DataType importDataType = finalised.dataTypes.find {it.label == 'Finalised Data Type'}
        DataClass importDataClass = finalised.dataClasses.find {it.label == 'Finalised Data Class'}
        DataElement importDataElement = importDataClass.dataElements.find {it.label == 'Finalised Data Element'}

        emptyDataClassId = empty.id
        importDataTypeId = importDataType.id
        importDataClassId = importDataClass.id
        importDataElementId = importDataElement.id

        empty.addToImportedDataClasses(importDataClass)
        empty.addToImportedDataElements(importDataElement)

        checkAndSave(empty)

        complex.addToImportedDataClasses(importDataClass)
        complex.addToImportedDataTypes(importDataType)

        checkAndSave(complex)

        profileSpecificationFieldProfileService.storeFlatMapProfileInEntity(importDataElement, FUNCTIONAL_TEST, [
            metadataPropertyName: 'functionalTesting',
            defaultValue        : 'Replace Me'
        ])

        complexDataModelId = complex.id
        simpleFinalisedDataModelId = finalised.id

        sessionFactory.currentSession.flush()
    }

    @Transactional
    def cleanupSpec() {
        log.debug('CleanupSpec DataFlowFunctionalSpec')
        cleanUpResources(DataModel, Folder)
    }

    @Override
    String getResourcePath() {
        ''
    }

    Map buildImportingOwnerSection(Map profileMap, UUID id, String domainType) {
        profileMap.sections.add([name  : DynamicImportJsonProfileProviderService.IMPORTING_SECTION_NAME,
                                 fields: [
                                     [metadataPropertyName: 'import_id', currentValue: id.toString()],
                                     [metadataPropertyName: 'import_domainType', currentValue: domainType]
                                 ]
        ])
        profileMap
    }

    Map buildUsageProfileMap(String mandation) {
        [sections: [
            [
                name  : 'usage',
                fields: [
                    [metadataPropertyName: 'mandation', currentValue: mandation],
                    [metadataPropertyName: 'multiplicity', currentValue: 1]
                ]]
        ]]
    }

    void 'ST01 : test adding profile to DataElement using standard endpoints'() {
        given:
        DynamicImportJsonProfileProviderService pps = importedDataElementDynamicProfileProviderService
        String importNamespace = "import.${emptyDataClassId}.${pps.profileNamespace}"
        String endpointPrefix = "dataElements/${importDataElementId}"
        Map profileMap = buildUsageProfileMap('Optional')

        when: 'validate without import owner section'
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}/validate", profileMap)

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().total == 3
        responseBody().errors.every {it.message == 'For a new import profile [Id & Domain Type] OR [Path] must be supplied'}
        responseBody().errors.any {it.metadataPropertyName == 'import_id'}
        responseBody().errors.any {it.metadataPropertyName == 'import_domainType'}
        responseBody().errors.any {it.metadataPropertyName == 'import_path'}

        when: 'validate with import owner section'
        buildImportingOwnerSection(profileMap, emptyDataClassId, 'DataClass')

        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}/validate", profileMap)

        then:
        verifyResponse(OK, response)

        when: 'save validated profile'
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)

        when: 'get the default profile which will be empty'
        GET("${endpointPrefix}/profile/${pps.namespace}/${pps.name}")

        then:
        verifyResponse(OK, response)
        def section1 = responseBody().sections.find {it.name == DynamicImportJsonProfileProviderService.IMPORTING_SECTION_NAME}
        section1.fields.any {it.metadataPropertyName == 'import_id' && !it.currentValue}
        section1.fields.any {it.metadataPropertyName == 'import_domainType' && !it.currentValue}
        def section2 = responseBody().sections.find {it.name == 'usage'}
        section2.fields.any {it.metadataPropertyName == 'mandation' && !it.currentValue}
        section2.fields.any {it.metadataPropertyName == 'multiplicity' && !it.currentValue}

        when: 'get the used profiles'
        HttpResponse<List<Map>> localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 2
        localResponse.body().any {it.name == 'ProfileSpecificationFieldProfileService'}
        localResponse.body().any {it.name == pps.name && it.namespace == importNamespace}

        when: 'get the unused profiles'
        localResponse = GET("${endpointPrefix}/profiles/unused", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().any {it.name == pps.name && it.namespace == pps.namespace}

        when: 'get the import profile'
        GET("${endpointPrefix}/profile/${importNamespace}/${pps.name}")

        then:
        verifyResponse(OK, response)
        def section3 = responseBody().sections.find {it.name == DynamicImportJsonProfileProviderService.IMPORTING_SECTION_NAME}
        section3.fields.any {it.metadataPropertyName == 'import_id' && it.currentValue == emptyDataClassId.toString()}
        section3.fields.any {it.metadataPropertyName == 'import_domainType' && it.currentValue == 'DataClass'}
        def section4 = responseBody().sections.find {it.name == 'usage'}
        section4.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Optional'}
        section4.fields.any {it.metadataPropertyName == 'multiplicity' && it.currentValue == '1'}

        when: 'update the import profile via the correct namespace'
        profileMap = buildImportingOwnerSection(buildUsageProfileMap('Required'), emptyDataClassId, 'DataClass')
        POST("${endpointPrefix}/profile/${importNamespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)
        def section5 = responseBody().sections.find {it.name == 'usage'}
        section5.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Required'}

        when: 'get the used profiles is still only 2'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 2

        when: 'update the import profile via the default namespace'
        profileMap = buildImportingOwnerSection(buildUsageProfileMap('Pilot'), emptyDataClassId, 'DataClass')
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)
        def section6 = responseBody().sections.find {it.name == 'usage'}
        section6.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Pilot'}

        when: 'get the used profiles is still only 2'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 2

        when: 'delete the profile'
        DELETE("${endpointPrefix}/profile/${importNamespace}/${pps.name}", profileMap)

        then:
        verifyResponse(NO_CONTENT, response)


        when: 'get the used profiles'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
    }

    void 'ST02 : test adding profile to DataType using standard endpoints'() {
        given:
        DynamicImportJsonProfileProviderService pps = importedDataTypeDynamicProfileProviderService
        String importNamespace = "import.${complexDataModelId}.${pps.profileNamespace}"
        String endpointPrefix = "dataTypes/${importDataTypeId}"
        Map profileMap = buildUsageProfileMap('Optional')

        when: 'validate without import owner section'
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}/validate", profileMap)

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().total == 3
        responseBody().errors.every {it.message == 'For a new import profile [Id & Domain Type] OR [Path] must be supplied'}
        responseBody().errors.any {it.metadataPropertyName == 'import_id'}
        responseBody().errors.any {it.metadataPropertyName == 'import_domainType'}
        responseBody().errors.any {it.metadataPropertyName == 'import_path'}

        when: 'validate with import owner section'
        buildImportingOwnerSection(profileMap, complexDataModelId, 'DataModel')

        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}/validate", profileMap)

        then:
        verifyResponse(OK, response)

        when: 'save validated profile'
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)

        when: 'get the default profile which will be empty'
        GET("${endpointPrefix}/profile/${pps.namespace}/${pps.name}")

        then:
        verifyResponse(OK, response)
        def section1 = responseBody().sections.find {it.name == DynamicImportJsonProfileProviderService.IMPORTING_SECTION_NAME}
        section1.fields.any {it.metadataPropertyName == 'import_id' && !it.currentValue}
        section1.fields.any {it.metadataPropertyName == 'import_domainType' && !it.currentValue}
        def section2 = responseBody().sections.find {it.name == 'usage'}
        section2.fields.any {it.metadataPropertyName == 'mandation' && !it.currentValue}
        section2.fields.any {it.metadataPropertyName == 'multiplicity' && !it.currentValue}

        when: 'get the used profiles'
        HttpResponse<List<Map>> localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().any {it.name == pps.name && it.namespace == importNamespace}

        when: 'get the unused profiles'
        localResponse = GET("${endpointPrefix}/profiles/unused", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().any {it.name == pps.name && it.namespace == pps.namespace}

        when: 'get the import profile'
        GET("${endpointPrefix}/profile/${importNamespace}/${pps.name}")

        then:
        verifyResponse(OK, response)
        def section3 = responseBody().sections.find {it.name == DynamicImportJsonProfileProviderService.IMPORTING_SECTION_NAME}
        section3.fields.any {it.metadataPropertyName == 'import_id' && it.currentValue == complexDataModelId.toString()}
        section3.fields.any {it.metadataPropertyName == 'import_domainType' && it.currentValue == 'DataModel'}
        def section4 = responseBody().sections.find {it.name == 'usage'}
        section4.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Optional'}
        section4.fields.any {it.metadataPropertyName == 'multiplicity' && it.currentValue == '1'}

        when: 'update the import profile via the correct namespace'
        profileMap = buildImportingOwnerSection(buildUsageProfileMap('Required'), complexDataModelId, 'DataModel')
        POST("${endpointPrefix}/profile/${importNamespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)
        def section5 = responseBody().sections.find {it.name == 'usage'}
        section5.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Required'}

        when: 'get the used profiles is still only 1'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1

        when: 'update the import profile via the default namespace'
        profileMap = buildImportingOwnerSection(buildUsageProfileMap('Pilot'), complexDataModelId, 'DataModel')
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)
        def section6 = responseBody().sections.find {it.name == 'usage'}
        section6.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Pilot'}

        when: 'get the used profiles is still only 1'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1

        when: 'delete the profile'
        DELETE("${endpointPrefix}/profile/${importNamespace}/${pps.name}", profileMap)

        then:
        verifyResponse(NO_CONTENT, response)


        when: 'get the used profiles'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 0
    }

    void 'ST03 : test adding profile to DataClass for DataModel using standard endpoints'() {
        given:
        DynamicImportJsonProfileProviderService pps = importedDataClassDynamicProfileProviderService
        String importNamespace = "import.${complexDataModelId}.${pps.profileNamespace}"
        String endpointPrefix = "dataClasses/${importDataClassId}"
        Map profileMap = buildUsageProfileMap('Optional')

        when: 'validate without import owner section'
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}/validate", profileMap)

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().total == 3
        responseBody().errors.every {it.message == 'For a new import profile [Id & Domain Type] OR [Path] must be supplied'}
        responseBody().errors.any {it.metadataPropertyName == 'import_id'}
        responseBody().errors.any {it.metadataPropertyName == 'import_domainType'}
        responseBody().errors.any {it.metadataPropertyName == 'import_path'}

        when: 'validate with import owner section'
        buildImportingOwnerSection(profileMap, complexDataModelId, 'DataModel')

        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}/validate", profileMap)

        then:
        verifyResponse(OK, response)

        when: 'save validated profile'
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)

        when: 'get the default profile which will be empty'
        GET("${endpointPrefix}/profile/${pps.namespace}/${pps.name}")

        then:
        verifyResponse(OK, response)
        def section1 = responseBody().sections.find {it.name == DynamicImportJsonProfileProviderService.IMPORTING_SECTION_NAME}
        section1.fields.any {it.metadataPropertyName == 'import_id' && !it.currentValue}
        section1.fields.any {it.metadataPropertyName == 'import_domainType' && !it.currentValue}
        def section2 = responseBody().sections.find {it.name == 'usage'}
        section2.fields.any {it.metadataPropertyName == 'mandation' && !it.currentValue}
        section2.fields.any {it.metadataPropertyName == 'multiplicity' && !it.currentValue}

        when: 'get the used profiles'
        HttpResponse<List<Map>> localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().any {it.name == pps.name && it.namespace == importNamespace}

        when: 'get the unused profiles'
        localResponse = GET("${endpointPrefix}/profiles/unused", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().any {it.name == pps.name && it.namespace == pps.namespace}

        when: 'get the import profile'
        GET("${endpointPrefix}/profile/${importNamespace}/${pps.name}")

        then:
        verifyResponse(OK, response)
        def section3 = responseBody().sections.find {it.name == DynamicImportJsonProfileProviderService.IMPORTING_SECTION_NAME}
        section3.fields.any {it.metadataPropertyName == 'import_id' && it.currentValue == complexDataModelId.toString()}
        section3.fields.any {it.metadataPropertyName == 'import_domainType' && it.currentValue == 'DataModel'}
        def section4 = responseBody().sections.find {it.name == 'usage'}
        section4.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Optional'}
        section4.fields.any {it.metadataPropertyName == 'multiplicity' && it.currentValue == '1'}

        when: 'update the import profile via the correct namespace'
        profileMap = buildImportingOwnerSection(buildUsageProfileMap('Required'), complexDataModelId, 'DataModel')
        POST("${endpointPrefix}/profile/${importNamespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)
        def section5 = responseBody().sections.find {it.name == 'usage'}
        section5.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Required'}

        when: 'get the used profiles is still only 1'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1

        when: 'update the import profile via the default namespace'
        profileMap = buildImportingOwnerSection(buildUsageProfileMap('Pilot'), complexDataModelId, 'DataModel')
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)
        def section6 = responseBody().sections.find {it.name == 'usage'}
        section6.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Pilot'}

        when: 'get the used profiles is still only 1'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1

        when: 'delete the profile'
        DELETE("${endpointPrefix}/profile/${importNamespace}/${pps.name}", profileMap)

        then:
        verifyResponse(NO_CONTENT, response)


        when: 'get the used profiles'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 0
    }

    void 'ST04 : test adding profile to DataClass for DataClass using standard endpoints'() {
        given:
        DynamicImportJsonProfileProviderService pps = importedDataClassDynamicProfileProviderService
        String importNamespace = "import.${emptyDataClassId}.${pps.profileNamespace}"
        String endpointPrefix = "dataClasses/${importDataClassId}"
        Map profileMap = buildUsageProfileMap('Optional')

        when: 'validate without import owner section'
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}/validate", profileMap)

        then:
        verifyResponse(UNPROCESSABLE_ENTITY, response)
        responseBody().total == 3
        responseBody().errors.every {it.message == 'For a new import profile [Id & Domain Type] OR [Path] must be supplied'}
        responseBody().errors.any {it.metadataPropertyName == 'import_id'}
        responseBody().errors.any {it.metadataPropertyName == 'import_domainType'}
        responseBody().errors.any {it.metadataPropertyName == 'import_path'}

        when: 'validate with import owner section'
        buildImportingOwnerSection(profileMap, emptyDataClassId, 'DataClass')

        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}/validate", profileMap)

        then:
        verifyResponse(OK, response)

        when: 'save validated profile'
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)

        when: 'get the default profile which will be empty'
        GET("${endpointPrefix}/profile/${pps.namespace}/${pps.name}")

        then:
        verifyResponse(OK, response)
        def section1 = responseBody().sections.find {it.name == DynamicImportJsonProfileProviderService.IMPORTING_SECTION_NAME}
        section1.fields.any {it.metadataPropertyName == 'import_id' && !it.currentValue}
        section1.fields.any {it.metadataPropertyName == 'import_domainType' && !it.currentValue}
        def section2 = responseBody().sections.find {it.name == 'usage'}
        section2.fields.any {it.metadataPropertyName == 'mandation' && !it.currentValue}
        section2.fields.any {it.metadataPropertyName == 'multiplicity' && !it.currentValue}

        when: 'get the used profiles'
        HttpResponse<List<Map>> localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().any {it.name == pps.name && it.namespace == importNamespace}

        when: 'get the unused profiles'
        localResponse = GET("${endpointPrefix}/profiles/unused", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1
        localResponse.body().any {it.name == pps.name && it.namespace == pps.namespace}

        when: 'get the import profile'
        GET("${endpointPrefix}/profile/${importNamespace}/${pps.name}")

        then:
        verifyResponse(OK, response)
        def section3 = responseBody().sections.find {it.name == DynamicImportJsonProfileProviderService.IMPORTING_SECTION_NAME}
        section3.fields.any {it.metadataPropertyName == 'import_id' && it.currentValue == emptyDataClassId.toString()}
        section3.fields.any {it.metadataPropertyName == 'import_domainType' && it.currentValue == 'DataClass'}
        def section4 = responseBody().sections.find {it.name == 'usage'}
        section4.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Optional'}
        section4.fields.any {it.metadataPropertyName == 'multiplicity' && it.currentValue == '1'}

        when: 'update the import profile via the correct namespace'
        profileMap = buildImportingOwnerSection(buildUsageProfileMap('Required'), emptyDataClassId, 'DataClass')
        POST("${endpointPrefix}/profile/${importNamespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)
        def section5 = responseBody().sections.find {it.name == 'usage'}
        section5.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Required'}

        when: 'get the used profiles is still only 1'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1

        when: 'update the import profile via the default namespace'
        profileMap = buildImportingOwnerSection(buildUsageProfileMap('Pilot'), emptyDataClassId, 'DataClass')
        POST("${endpointPrefix}/profile/${pps.namespace}/${pps.name}", profileMap)

        then:
        verifyResponse(OK, response)
        def section6 = responseBody().sections.find {it.name == 'usage'}
        section6.fields.any {it.metadataPropertyName == 'mandation' && it.currentValue == 'Pilot'}

        when: 'get the used profiles is still only 1'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 1

        when: 'delete the profile'
        DELETE("${endpointPrefix}/profile/${importNamespace}/${pps.name}", profileMap)

        then:
        verifyResponse(NO_CONTENT, response)

        when: 'get the used profiles'
        localResponse = GET("${endpointPrefix}/profiles/used", LIST_MAP_ARG)

        then:
        verifyResponse(OK, localResponse)
        localResponse.body().size() == 0
    }
}
