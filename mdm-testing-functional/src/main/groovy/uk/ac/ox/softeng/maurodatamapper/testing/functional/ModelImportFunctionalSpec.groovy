/*
 * Copyright 2020 University of Oxford
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
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import spock.lang.Stepwise

import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NOT_FOUND
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

    abstract String getResourcePath()

    abstract String getEditorIndexJson()

    /**
     * The endpoint (without ID) used for ModelImport
     *
     */
    abstract String getModelImportPath()

    /**
     * Endpoints for additional model imports which should be cleaned up
     *
     */
    abstract List getAdditionalModelImportPaths()    

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

    /**
     * Expected JSON for a ModelImport
     *
     */
    abstract String getExpectedModelImportJson()

    /**
     * Expected JSON when listing all resources including imported catalogue items.
     * Should be the same as getEditorIndexJson() but with the addition of any imported resources.
     *
     */
    abstract String getEditorIndexJsonWithImported()

    Map getModelImportJson() {
        [
            importedCatalogueItemDomainType       : getImportedCatalogueItemDomainType(),
            importedCatalogueItemId               : getImportedCatalogueItemId()
        ]
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
        GET(getResourcePath(), STRING_ARG, true)

        then: "The correct resources are listed"
        verifyJsonResponse HttpStatus.OK, getEditorIndexJson()

        when: "The save action is executed with valid data"
        POST(getModelImportPath(), getModelImportJson(), MAP_ARG, true)

        then: "The response is correct"
        verifyResponse HttpStatus.CREATED, response
        String id = responseBody().id
        assert responseBody().catalogueItem
        assert responseBody().importedCatalogueItem

        when: "The ModelImport is requested"
        GET("${getModelImportPath()}/${id}" , STRING_ARG, true)        

        then: "The response is correct"
        verifyJsonResponse HttpStatus.OK, getExpectedModelImportJson()

        when: "List the resources on the endpoint without showing imported resources"
        GET("${getResourcePath()}?imported=false", STRING_ARG, true)

        then: "The correct resources are listed"
        verifyJsonResponse HttpStatus.OK, getEditorIndexJson()        

        when: "List the resources on the endpoint showing imported resources"
        GET(getResourcePath(), STRING_ARG, true)

        then: "The correct resources are listed"
        verifyJsonResponse HttpStatus.OK, getEditorIndexJsonWithImported()           

        cleanup:
        GET("${getModelImportPath()}", MAP_ARG, true)
        verifyResponse HttpStatus.OK, response
        responseBody().items.each { item ->
            log.debug("Deleting model import {}", item.id)
            DELETE("${getModelImportPath()}/${item.id}", MAP_ARG, true) 
            verifyResponse HttpStatus.NO_CONTENT, response
        }

        getAdditionalModelImportPaths().each { path ->
            GET("${path}", MAP_ARG, true)
            responseBody().items.each { item ->
                log.debug("Deleting additional model import {}", item.id)
                DELETE("${path}/${item.id}", MAP_ARG, true) 
                verifyResponse HttpStatus.NO_CONTENT, response
            }
        }
    }     
}
