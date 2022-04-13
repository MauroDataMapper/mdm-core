package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.RunOnce
import groovy.util.logging.Slf4j
import org.junit.jupiter.api.Tag
import spock.lang.Shared

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.FUNCTIONAL_TEST

import static io.micronaut.http.HttpStatus.OK

/**
 * @since 12/04/2022
 */
@Tag('non-parallel')
@Integration
@Slf4j
class SearchFunctionalSpec extends BaseFunctionalSpec {

    @Shared
    Folder folder

    @Shared
    UUID complexDataModelId

    @Shared
    UUID simpleDataModelId

    ProfileSpecificationFieldProfileService profileSpecificationFieldProfileService

    @Transactional
    Authority getTestAuthority() {
        Authority.findByDefaultAuthority(true)
    }

    @RunOnce
    @Transactional
    def setup() {
        log.debug('Check and setup test data')
        folder = new Folder(label: 'Functional Test Folder', createdBy: FUNCTIONAL_TEST)
        checkAndSave(folder)
        folder.addToMetadata(new Metadata(namespace: 'test.namespace', key: 'propertyKey', value: 'propertyValue', createdBy: FUNCTIONAL_TEST))
        checkAndSave(folder)

        DataModel dataModel = BootstrapModels.buildAndSaveComplexDataModel(messageSource, folder, testAuthority)
        complexDataModelId = dataModel.id

        dataModel.allDataElements.eachWithIndex {de, i ->
            profileSpecificationFieldProfileService.storeFieldInEntity(de, "value $i", 'metadataPropertyName', FUNCTIONAL_TEST)
            if (de.label == 'ele1') profileSpecificationFieldProfileService.storeFieldInEntity(de, "value type $i", 'metadataPropertyName', FUNCTIONAL_TEST)
            de.save()
        }
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

    void 'S01 : test searching using profile filter for single result'() {
        when:
        POST("dataModels/${complexDataModelId}/search", [
            searchTerm   : "child",
            domainTypes  : ["DataElement"],
            labelOnly    : true,
            profileFields: [
                [
                    metadataNamespace   : profileSpecificationFieldProfileService.metadataNamespace,
                    metadataPropertyName: 'metadataPropertyName',
                    filterTerm          : 'value'
                ],
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.first().label == 'child'

        when:
        POST("dataModels/${complexDataModelId}/search", [
            searchTerm   : "child",
            domainTypes  : ["DataElement"],
            labelOnly    : true,
            profileFields: [
                [
                    metadataNamespace   : profileSpecificationFieldProfileService.metadataNamespace,
                    metadataPropertyName: 'metadataPropertyName',
                    filterTerm          : 'blob'
                ],
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 0
    }

    void 'S02 : test searching using profile filter for multiple results'() {
        when:
        POST("dataModels/${complexDataModelId}/search", [
            searchTerm   : "ele*",
            domainTypes  : ["DataElement"],
            labelOnly    : true,
            profileFields: [
                [
                    metadataNamespace   : profileSpecificationFieldProfileService.metadataNamespace,
                    metadataPropertyName: 'metadataPropertyName',
                    filterTerm          : 'value'
                ],
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 2
        responseBody().items.any {it.label == 'ele1'}
        responseBody().items.any {it.label == 'element2'}

        when:
        POST("dataModels/${complexDataModelId}/search", [
            searchTerm   : "ele*",
            domainTypes  : ["DataElement"],
            labelOnly    : true,
            profileFields: [
                [
                    metadataNamespace   : profileSpecificationFieldProfileService.metadataNamespace,
                    metadataPropertyName: 'metadataPropertyName',
                    filterTerm          : 'value type'
                ],
            ]
        ])

        then:
        verifyResponse(OK, response)
        responseBody().count == 1
        responseBody().items.any {it.label == 'ele1'}
    }
}
