/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional

import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Stepwise

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.OK

/**
 * Where it is possible to import catalogue items, test that endpoints correctly show imported catalogue items when required.
 *
 * <pre>
 * Controller: dataXxxxx
 *  |  POST    |  /api/dataModels/${dataModelId}/dataXxxxx/ | Action: index
 *
 * </pre>
 */
@Stepwise
@Slf4j
abstract class ModelImportFunctionalSpec extends FunctionalSpec {

    String getResourcePath() {
        ''
    }

    abstract String getIndexPath()

    /**
     * The endpoint (without ID) used for ModelImport
     *
     */
    abstract String getModelImportPath()


    /**
     * ID of the CatalogueItem which is going to be imported
     *
     */
    abstract String getImportedCatalogueItemId()

    /**
     * Domain type of the CatalogueItem which is going to be imported
     *
     */
    abstract String getImportedCatalogueItemDomainType()

    abstract String getCatalogueItemId()

    abstract String getCatalogueItemDomainType()

    /**
     * Endpoints for additional model imports which should be cleaned up
     *
     */
    List getAdditionalModelImportPaths() {
        []
    }

    void verifyIndex() {
        assert responseBody().count == 0
        assert responseBody().items.size() == 0
    }

    void cleanupModelImports() {
        GET("${getModelImportPath()}")
        verifyResponse OK, response
        responseBody().items.each {item ->
            log.debug("Deleting model import {}", item.id)
            DELETE("${getModelImportPath()}/${item.id}")
            verifyResponse HttpStatus.NO_CONTENT, response
        }

        getAdditionalModelImportPaths().each {path ->
            GET("${path}")
            responseBody().items.each {item ->
                log.debug("Deleting additional model import {}", item.id)
                DELETE("${path}/${item.id}")
                verifyResponse HttpStatus.NO_CONTENT, response
            }
        }
    }

    /**
     * Import a CatalogueItem.
     * Note that we are not testing here that ModelImports are done/not done for different logins -
     * that is done by separate facet tests.
     * Check that the imported item appears in relevant endpoints when the ?imported query parameter is used.
     * Check that the imported item does not appear when the ?imported query parameter is not used.
     */
    void "MI01: import CatalogueItem and check it is listed in its Endpoints"() {
        given:
        loginEditor()

        when: "List the resources on the endpoint"
        GET(getIndexPath())

        then: "The correct resources are listed"
        verifyResponse OK, response
        verifyIndex()

        when: "The save action is executed with valid data"
        POST(getModelImportPath(), [
            importedCatalogueItemDomainType: getImportedCatalogueItemDomainType(),
            importedCatalogueItemId        : getImportedCatalogueItemId()
        ])

        then: "The response is correct"
        verifyResponse CREATED, response
        String id = responseBody().id
        responseBody().catalogueItem.id == getCatalogueItemId()
        responseBody().catalogueItem.domainType == getCatalogueItemDomainType()
        responseBody().importedCatalogueItem.id == getImportedCatalogueItemId()
        responseBody().importedCatalogueItem.domainType == getImportedCatalogueItemDomainType()

        when: "The ModelImport is requested"
        GET("${getModelImportPath()}/${id}")

        then: "The response is correct"
        verifyResponse OK, response
        responseBody().id == id
        responseBody().catalogueItem.id == getCatalogueItemId()
        responseBody().catalogueItem.domainType == getCatalogueItemDomainType()
        responseBody().importedCatalogueItem.id == getImportedCatalogueItemId()
        responseBody().importedCatalogueItem.domainType == getImportedCatalogueItemDomainType()

        when: "List the resources on the endpoint without showing imported resources"
        GET("${getIndexPath()}?imported=false")

        then: "The imported CI is not there"
        verifyResponse OK, response
        responseBody().items.every {it.id != importedCatalogueItemId}

        when: "List the resources on the endpoint showing imported resources"
        GET(getIndexPath())

        then: "The correct resources are listed"
        verifyResponse OK, response
        responseBody().items.any {it.id == importedCatalogueItemId}

        cleanup:
        cleanupModelImports()
    }
}
