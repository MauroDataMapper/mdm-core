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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.path

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.artefact.DomainClass
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import static io.micronaut.http.HttpStatus.OK

/**
 * <pre>
 * Controller: path
 *  |   GET   | /api/terminologies/$terminologyId/path/$path       | Action: show
 *  |   GET   | /api/terminologies/path/$path                      | Action: show
 *  |   GET   | /api/codeSets/$codeSetId/path/$path                | Action: show
 *  |   GET   | /api/codeSets/path/$path                           | Action: show
 *  |   GET   | /api/dataModels/$dataModelId/path/$path            | Action: show
 *  |   GET   | /api/dataModels/path/$path                         | Action: show
 *  |   GET   | /api/path/$path                                    | Action: show
 * </pre>
 *
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.path.PathController
 */
@Integration
@Slf4j
class PathFunctionalSpec extends FunctionalSpec {

    String SIMPLE_TERMINOLOGY_NAME = BootstrapModels.SIMPLE_TERMINOLOGY_NAME
    String COMPLEX_TERMINOLOGY_NAME = BootstrapModels.COMPLEX_TERMINOLOGY_NAME
    String SIMPLE_CODESET_NAME = BootstrapModels.SIMPLE_CODESET_NAME
    String COMPLEX_DATAMODEL_NAME = uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels.COMPLEX_DATAMODEL_NAME
    String PARENT_DATACLASS_NAME = 'parent'
    String CHILD_DATACLASS_NAME = 'child'
    String CONTENT_DATACLASS_NAME = 'content'
    String DATA_ELEMENT_NAME = 'ele1'
    String PRIMITIVE_DATA_TYPE_NAME = 'integer'
    String ENUMERATION_DATA_TYPE_NAME = 'yesnounknown'
    String REFERENCE_DATA_TYPE_NAME = 'child'
    String node

    @Override
    String getResourcePath() {
        ''
    }

    @Transactional
    String getSimpleTerminologyId() {
        Terminology.findByLabel(SIMPLE_TERMINOLOGY_NAME).id.toString()
    }

    @Transactional
    String getComplexTerminologyId() {
        Terminology.findByLabel(COMPLEX_TERMINOLOGY_NAME).id.toString()
    }

    @Transactional
    String getSimpleCodeSetId() {
        CodeSet.findByLabel(SIMPLE_CODESET_NAME).id.toString()
    }

    @Transactional
    String getComplexDataModelId() {
        DataModel.findByLabel(COMPLEX_DATAMODEL_NAME).id.toString()
    }

    String makePathNode(String prefix, String label) {
        prefix + ':' + label
    }

    String makePathNodes(String... pathNodes) {
        pathNodes.join('|')
    }

    String makePath(String node) {
        Utils.safeUrlEncode(node)
    }

    def cleanup() {
        node = null
    }

    void verifyNotFound(HttpResponse<Map> response, Object id, Class resourceClass = DomainClass) {
        super.verifyNotFound(response, id)
        assert response.body().resource == resourceClass.simpleName
    }

    void 'T01: Get Terminology by path and ID when not logged in'() {
        //No ID
        when:
        node = makePathNode('te', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID
        when:
        node = makePathNode('te', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getSimpleTerminologyId(), Terminology)

        //No ID
        when:
        node = makePathNode('te', COMPLEX_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID
        when:
        node = makePathNode('te', COMPLEX_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getComplexTerminologyId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getComplexTerminologyId(), Terminology)
    }

    void 'T02 : Get Terminology by path and ID when logged in'() {
        given:
        loginReader()

        //No ID
        when:
        node = makePathNode('te', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedSimpleTerminologyJson()

        //With ID
        when:
        node = makePathNode('te', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedSimpleTerminologyJson()

        //No ID
        when:
        node = makePathNode('te', COMPLEX_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedComplexTerminologyJson()

        //With ID
        when:
        node = makePathNode('te', COMPLEX_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getComplexTerminologyId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedComplexTerminologyJson()
    }

    void 'T03 : get a Terminology but use the wrong prefix when not logged in'() {
        //No ID
        when:
        node = makePathNode('tm', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID (this shouldnt work as no read access to simple terminology id)
        when:
        node = makePathNode('tm', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getSimpleTerminologyId(), Terminology)
    }

    void 'T04 : get a Terminology but use the wrong prefix when logged in'() {
        given:
        loginReader()

        //No ID
        when:
        node = makePathNode('tm', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID (shouldnt work as the prefix is wrong)
        when:
        node = makePathNode('tm', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath(node)}")

        then: 'The response is OK because the ID is used'
        verifyNotFound(response, node)
    }

    void 'C01 : Get CodeSet by path and ID when not logged in'() {
        //No ID
        when:
        node = makePathNode('cs', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID
        when:
        node = makePathNode('cs', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getSimpleCodeSetId(), CodeSet)
    }

    void 'C02 : Get CodeSet by path and ID when logged in'() {
        given:
        loginReader()

        //No ID
        when:
        node = makePathNode('cs', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedSimpleCodeSetJson()

        //With ID
        when:
        node = makePathNode('cs', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedSimpleCodeSetJson()
    }

    void 'C03 : get a CodeSet but use the wrong prefix when not logged in'() {
        //No ID
        when:
        node = makePathNode('tm', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID (no access to the ID'd codeset)
        when:
        node = makePathNode('tm', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getSimpleCodeSetId(), CodeSet)
    }

    void 'C04 : get a CodeSet but use the wrong prefix when logged in'() {
        given:
        loginReader()

        //No ID
        when:
        node = makePathNode('tm', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID (shouldnt work as the prefix is wrong)
        when:
        node = makePathNode('tm', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath(node)}")

        then: 'The response is OK because the ID is used'
        verifyNotFound(response, node)
    }

    void 'TM01 : get a Term for a Terminology when not logged in'() {
        //No Terminology ID
        when:
        node = makePathNodes(makePathNode('te', SIMPLE_TERMINOLOGY_NAME),
                             makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/terminologies/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With Terminology ID and label
        when:
        node = makePathNodes(makePathNode('te', SIMPLE_TERMINOLOGY_NAME),
                             makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getSimpleTerminologyId(), Terminology)

        //With Terminology ID and no label
        when:
        node = makePathNodes(makePathNode('te', ''),
                             makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getSimpleTerminologyId(), Terminology)
    }

    void 'TM02 : get a Term for a Terminology when logged in'() {
        given:
        loginReader()

        //No Terminology ID
        when:
        node = makePathNodes(makePathNode('te', SIMPLE_TERMINOLOGY_NAME),
                             makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/terminologies/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedSimpleTermJson()

        //With Terminology ID and label
        when:
        node = makePathNodes(makePathNode('te', SIMPLE_TERMINOLOGY_NAME),
                             makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedSimpleTermJson()

        //With Terminology ID and no label
        when:
        node = makePathNodes(makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedSimpleTermJson()
    }

    void 'TM03 : get a Term for a CodeSet when not logged in'() {
        //No CodeSet ID
        when:
        node = makePathNodes(makePathNode('cs', SIMPLE_CODESET_NAME),
                             makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/codeSets/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With CodeSet ID and label
        when:
        node = makePathNodes(makePathNode('cs', SIMPLE_CODESET_NAME),
                             makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getSimpleCodeSetId(), CodeSet)

        //With CodeSet ID and no label
        when:
        node = makePathNodes(makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getSimpleCodeSetId(), CodeSet)
    }

    void 'TM04 : get a Term for a CodeSet when logged in'() {
        given:
        loginReader()

        //No CodeSet ID
        when:
        node = makePathNodes(makePathNode('cs', SIMPLE_CODESET_NAME),
                             makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/codeSets/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedSimpleTermJson()

        //With CodeSet ID and label
        when:
        node = makePathNodes(makePathNode('cs', SIMPLE_CODESET_NAME),
                             makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedSimpleTermJson()

        //With CodeSet ID and no label
        when:
        node = makePathNodes(makePathNode('tm', 'STT01: Simple Test Term 01'))
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedSimpleTermJson()
    }

    void 'TM05 : get a Term for a Terminology when logged in and term not in terminology'() {
        given:
        loginReader()

        //No Terminology ID
        when:
        node = makePathNodes(makePathNode('te', SIMPLE_TERMINOLOGY_NAME),
                             makePathNode('tm', 'CTT01'))
        GET("/api/terminologies/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With Terminology ID and label
        when:
        node = makePathNodes(makePathNode('te', SIMPLE_TERMINOLOGY_NAME),
                             makePathNode('tm', 'CTT01'))
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)
    }

    void 'DM01 : Get DataModel by path and ID when not logged in'() {
        //No ID
        when:
        node = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        GET("/api/dataModels/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID
        when:
        node = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getComplexDataModelId(), DataModel)
    }

    void 'DM02 : Get DataModel by path and ID when logged in'() {
        given:
        loginReader()

        //No ID
        when:
        node = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        GET("/api/dataModels/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedComplexDataModelJson()

        //With ID
        when:
        node = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedComplexDataModelJson()
    }

    void 'DC01 : Get DataClass with DataModel by path and ID when not logged in'() {
        //No ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dc', PARENT_DATACLASS_NAME))
        GET("/api/dataModels/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dc', PARENT_DATACLASS_NAME))
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getComplexDataModelId(), DataModel)
    }

    void 'DC02 : Get DataClass with DataModel by path and ID when logged in'() {
        given:
        loginReader()

        //No ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dc', PARENT_DATACLASS_NAME))
        GET("/api/dataModels/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedParentDataClassJson()

        //With ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dc', PARENT_DATACLASS_NAME))
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedParentDataClassJson()
    }

    void 'DC03 : Get non-existent DataClass with DataModel by path and ID when logged in'() {
        given:
        loginReader()

        //No ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dc', 'simple'))
        GET("/api/dataModels/path/${makePath(node)}")

        then: 'The response is OK'
        verifyNotFound(response, node)

        //With ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dc', 'simple'))
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}")

        then: 'The response is OK'
        verifyNotFound(response, node)
    }

    void 'DE01 : Get DataElement by path and ID when not logged in'() {
        //No ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dc', CONTENT_DATACLASS_NAME),
                             makePathNode('de', DATA_ELEMENT_NAME))
        GET("/api/dataModels/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dc', CONTENT_DATACLASS_NAME),
                             makePathNode('de', DATA_ELEMENT_NAME))
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getComplexDataModelId(), DataModel)
    }

    void 'DE02 : Get DataElement by DataClass, path and ID when logged in'() {
        given:
        loginReader()

        //No ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dc', CONTENT_DATACLASS_NAME),
                             makePathNode('de', DATA_ELEMENT_NAME))
        GET("/api/dataModels/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedDataElementJson()

        //With ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dc', CONTENT_DATACLASS_NAME),
                             makePathNode('de', DATA_ELEMENT_NAME))
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedDataElementJson()
    }

    void 'DT01 : Get PrimitiveDataType by DataModel, path and ID when not logged in'() {
        //No ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', PRIMITIVE_DATA_TYPE_NAME))
        GET("/api/dataModels/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', PRIMITIVE_DATA_TYPE_NAME))
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getComplexDataModelId(), DataModel)
    }

    void 'DT02 : Get PrimitiveDataType by DataModel, path and ID when logged in'() {
        given:
        loginReader()

        //No ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', PRIMITIVE_DATA_TYPE_NAME))
        GET("/api/dataModels/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedPrimitiveTypeJson()

        //With ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', PRIMITIVE_DATA_TYPE_NAME))
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedPrimitiveTypeJson()
    }

    void 'DT03 : Get EnumerationType by DataModel, path and ID when not logged in'() {
        //No ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', ENUMERATION_DATA_TYPE_NAME))
        GET("/api/dataModels/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', ENUMERATION_DATA_TYPE_NAME))
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getComplexDataModelId(), DataModel)
    }

    void 'DT04 : Get EnumerationType by DataModel, path and ID when logged in'() {
        given:
        loginReader()

        //No ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', ENUMERATION_DATA_TYPE_NAME))
        GET("/api/dataModels/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedEnumerationTypeJson()

        //With ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', ENUMERATION_DATA_TYPE_NAME))
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedEnumerationTypeJson()
    }

    void 'DT05 : Get ReferenceType by DataModel, path and ID when not logged in'() {
        //No ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', REFERENCE_DATA_TYPE_NAME))
        GET("/api/dataModels/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, node)

        //With ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', REFERENCE_DATA_TYPE_NAME))
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}")

        then: 'The response is Not Found'
        verifyNotFound(response, getComplexDataModelId(), DataModel)
    }

    void 'DT06 : Get ReferenceType by DataModel, path and ID when logged in'() {
        given:
        loginReader()

        //No ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', REFERENCE_DATA_TYPE_NAME))
        GET("/api/dataModels/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedReferenceTypeJson()

        //With ID
        when:
        node = makePathNodes(makePathNode('dm', COMPLEX_DATAMODEL_NAME),
                             makePathNode('dt', REFERENCE_DATA_TYPE_NAME))
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath(node)}", STRING_ARG)

        then: 'The response is OK'
        verifyJsonResponse OK, getExpectedReferenceTypeJson()
    }

    void 'PP : Confirm all path prefixes are unique'() {
        when:
        GET('path/prefixMappings', STRING_ARG)
        log.debug('{}', jsonResponseBody())

        and:
        GET('path/prefixMappings')

        then:
        verifyResponse(OK, response)

        when:
        Map<String, Map<String, String>> grouped = (responseBody() as Map<String, String>).groupBy {it.key}

        then:
        grouped.each {
            log.debug('Checking {}', it.key)
            assert it.value.size() == 1
        }
    }

    String getExpectedSimpleTerminologyJson() {
        return '''{
          "id": "${json-unit.matches:id}",
          "domainType": "Terminology",
          "label": "Simple Test Terminology",
          "availableActions": [
            "show"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "classifiers": [
            {
              "id": "${json-unit.matches:id}",
              "label": "test classifier simple",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            }
          ],
          "type": "Terminology",
          "branchName": "main",
          "documentationVersion": "1.0.0",
          "finalised": false,
          "readableByEveryone": false,
          "readableByAuthenticatedUsers": false,
          "author": "Test Bootstrap",
          "organisation": "Oxford BRC",
          "authority": {
            "id": "${json-unit.matches:id}",
            "url": "http://localhost",
            "label": "Mauro Data Mapper",
            "defaultAuthority": true
          }
        }'''
    }

    String getExpectedComplexTerminologyJson() {
        return '''{
          "id": "${json-unit.matches:id}",
          "domainType": "Terminology",
          "label": "Complex Test Terminology",
          "availableActions": [
            "show"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "classifiers": [
            {
              "id": "${json-unit.matches:id}",
              "label": "test classifier",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            },
            {
              "id": "${json-unit.matches:id}",
              "label": "test classifier2",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            }
          ],
          "type": "Terminology",
          "branchName": "main",
          "documentationVersion": "1.0.0",
          "finalised": false,
          "readableByEveryone": false,
          "readableByAuthenticatedUsers": false,
          "author": "Test Bootstrap",
          "organisation": "Oxford BRC",
            "authority": {
            "id": "${json-unit.matches:id}",
            "url": "http://localhost",
            "label": "Mauro Data Mapper",
            "defaultAuthority": true
          }
        }'''
    }

    String getExpectedSimpleCodeSetJson() {
        return '''{
          "id": "${json-unit.matches:id}",
          "domainType": "CodeSet",
          "label": "Simple Test CodeSet",
          "availableActions": [
            "show",
            "createNewVersions",
            "newForkModel",
            "finalisedReadActions"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "classifiers": [
            {
              "id": "${json-unit.matches:id}",
              "label": "test classifier",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            }
          ],
          "type": "CodeSet",
          "branchName": "main",
          "documentationVersion": "1.0.0",
          "finalised": true,
          "readableByEveryone": false,
          "readableByAuthenticatedUsers": false,
          "dateFinalised": "${json-unit.matches:offsetDateTime}",
          "author": "Test Bootstrap",
          "organisation": "Oxford BRC",
          "modelVersion": "1.0.0",
          "authority": {
            "id": "${json-unit.matches:id}",
            "url": "http://localhost",
            "label": "Mauro Data Mapper",
            "defaultAuthority": true
          }
        }'''
    }

    String getExpectedSimpleTermJson() {
        return '''{
          "id": "${json-unit.matches:id}",
          "domainType": "Term",
          "label": "STT01: Simple Test Term 01",
          "model": "${json-unit.matches:id}",
          "breadcrumbs": [
              {
               "id": "${json-unit.matches:id}",
               "label": "Simple Test Terminology",
               "domainType":"Terminology",
               "finalised":false
               }
          ],
          "availableActions": [
            "show"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "code": "STT01",
          "definition": "Simple Test Term 01"
        }'''
    }

    String getExpectedComplexDataModelJson() {
        return '''{
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Complex Test DataModel",
            "availableActions": [
              "show"
            ],
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "classifiers": [
              {
                "id": "${json-unit.matches:id}",
                "label": "test classifier",
                "lastUpdated": "${json-unit.matches:offsetDateTime}"
              },
              {
                "id": "${json-unit.matches:id}",
                "label": "test classifier2",
                "lastUpdated": "${json-unit.matches:offsetDateTime}"
              }
            ],
            "type": "Data Standard",
            "branchName": "main",
            "documentationVersion": "1.0.0",
            "finalised": false,
            "readableByEveryone": false,
            "readableByAuthenticatedUsers": false,
            "author": "admin person",
            "organisation": "brc",
            "authority": {
              "id": "${json-unit.matches:id}",
              "url": "http://localhost",
              "label": "Mauro Data Mapper",
              "defaultAuthority": true
            }
        }'''
    }

    String getExpectedParentDataClassJson() {
        return '''{
            "id": "${json-unit.matches:id}",
            "domainType": "DataClass",
            "label": "parent",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
                {
                "id": "${json-unit.matches:id}",
                "label": "Complex Test DataModel",
                "domainType": "DataModel",
                "finalised": false
                }
            ],
            "availableActions": [
                "show"
            ],
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "maxMultiplicity": -1,
            "minMultiplicity": 1
        }'''
    }

    String getExpectedChildDataClassJson() {
        return '''{
            "id": "${json-unit.matches:id}",
            "domainType": "DataClass",
            "label": "child",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
                {
                    "id": "${json-unit.matches:id}",
                    "label": "Complex Test DataModel",
                    "domainType": "DataModel",
                    "finalised": false
                },
                {
                    "id": "${json-unit.matches:id}",
                    "label": "parent",
                    "domainType": "DataClass"
                }
            ],
            "availableActions": [
                "show"
            ],
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "parentDataClass": "${json-unit.matches:id}"
        }'''
    }

    String getExpectedDataElementJson() {
        return '''{
            "id": "${json-unit.matches:id}",
            "domainType": "DataElement",
            "label": "ele1",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
                {
                    "id": "${json-unit.matches:id}",
                    "label": "Complex Test DataModel",
                    "domainType": "DataModel",
                    "finalised": false
                },
                {
                    "id": "${json-unit.matches:id}",
                    "label": "content",
                    "domainType": "DataClass"
                }
            ],
            "availableActions": [
                "show"
            ],
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "dataClass": "${json-unit.matches:id}",
            "dataType": {
              "id": "${json-unit.matches:id}",
              "domainType": "PrimitiveType",
              "label": "string",
              "model": "${json-unit.matches:id}",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                }
            ]
            },
            "maxMultiplicity": 20,
            "minMultiplicity": 0
        }'''
    }

    String getExpectedPrimitiveTypeJson() {
        return '''{
            "id": "${json-unit.matches:id}",
            "domainType": "PrimitiveType",
            "label": "integer",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
                {
                    "id": "${json-unit.matches:id}",
                    "label": "Complex Test DataModel",
                    "domainType": "DataModel",
                    "finalised": false
                }
            ],
            "availableActions": [
                "show"
            ],
            "lastUpdated": "${json-unit.matches:offsetDateTime}"
        }'''
    }

    String getExpectedEnumerationTypeJson() {
        return '''{
            "id": "${json-unit.matches:id}",
            "domainType": "EnumerationType",
            "label": "yesnounknown",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
                {
                    "id": "${json-unit.matches:id}",
                    "label": "Complex Test DataModel",
                    "domainType": "DataModel",
                    "finalised": false
                }
            ],
            "availableActions": [
                "show"
            ],
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "enumerationValues": [
              {
                "index": 1,
                "id": "${json-unit.matches:id}",
                "key": "N",
                "value": "No",
                "category": null
              },
              {
                "index": 2,
                "id": "${json-unit.matches:id}",
                "key": "U",
                "value": "Unknown",
                "category": null
              },
              {
                "index": 0,
                "id": "${json-unit.matches:id}",
                "key": "Y",
                "value": "Yes",
                "category": null
              }
            ]
        }'''
    }

    String getExpectedReferenceTypeJson() {
        return '''{
            "id": "${json-unit.matches:id}",
            "domainType": "ReferenceType",
            "label": "child",
            "model": "${json-unit.matches:id}",
            "breadcrumbs": [
                {
                    "id": "${json-unit.matches:id}",
                    "label": "Complex Test DataModel",
                    "domainType": "DataModel",
                    "finalised": false
                }
            ],
            "availableActions": [
                "show"
            ],
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "referenceClass": {
              "id": "${json-unit.matches:id}",
              "domainType": "DataClass",
              "label": "child",
              "model": "${json-unit.matches:id}",
              "breadcrumbs": [
                {
                  "id": "${json-unit.matches:id}",
                  "label": "Complex Test DataModel",
                  "domainType": "DataModel",
                  "finalised": false
                },
                {
                  "id": "${json-unit.matches:id}",
                  "label": "parent",
                  "domainType": "DataClass"
                }
              ],
              "parentDataClass": "${json-unit.matches:id}"
            }
        }'''
    }

}
