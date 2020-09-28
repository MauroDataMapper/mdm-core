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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.core.path


import uk.ac.ox.softeng.maurodatamapper.testing.functional.FunctionalSpec
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.CodeSet
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

import java.net.URLEncoder

import grails.gorm.transactions.Transactional

import static io.micronaut.http.HttpStatus.OK
import static io.micronaut.http.HttpStatus.NOT_FOUND

/**
 * <pre>
 * Controller: path
 *  |   GET   | /api/terminologies/$terminologyId/path/$path       | Action: show
 *  |   GET   | /api/terminologies/path/$path                      | Action: show
 *  |   GET   | /api/codeSets/$codeSetId/path/$path                | Action: show
 *  |   GET   | /api/codeSets/path/$path                           | Action: show
 *  |   GET   | /api/dataModels/$dataModelId/path/$path            | Action: show
 *  |   GET   | /api/dataModels/path/$path                         | Action: show
 *  |   GET   | /api/dataClasses/$dataClassId/path/$path           | Action: show
 *  |   GET   | /api/dataClasses/path/$path                        | Action: show
 * </pre>
 *
 *
 * @see uk.ac.ox.softeng.maurodatamapper.core.path.PathController
 */
@Integration
@Slf4j
class PathFunctionalSpec extends FunctionalSpec {

    String SIMPLE_TERMINOLOGY_NAME = uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels.SIMPLE_TERMINOLOGY_NAME
    String COMPLEX_TERMINOLOGY_NAME = uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels.COMPLEX_TERMINOLOGY_NAME
    String SIMPLE_CODESET_NAME = uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels.SIMPLE_CODESET_NAME
    String COMPLEX_DATAMODEL_NAME = uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels.COMPLEX_DATAMODEL_NAME
    String PARENT_DATACLASS_NAME = 'parent'
    String CHILD_DATACLASS_NAME = 'child'
    String CONTENT_DATACLASS_NAME = 'content'
    String DATA_ELEMENT_NAME = 'ele1'
    String DATA_TYPE_NAME = 'integer'

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

    @Transactional
    String getParentDataClassId() {
        DataClass.findByLabel(PARENT_DATACLASS_NAME).id.toString()
    }

    @Transactional
    String getChildDataClassId() {
        DataClass.findByLabel(CHILD_DATACLASS_NAME).id.toString()
    }

    @Transactional
    String getContentDataClassId() {
        DataClass.findByLabel(CONTENT_DATACLASS_NAME).id.toString()
    }

    @Transactional
    String getDataElementId() {
        DataClass.findByLabel(DATA_ELEMENT_NAME).id.toString()
    }

    String makePathNode(String prefix, String label) {
        prefix + ":" + label
    }

    String makePath(List<String> nodes) {
        //java.net.URLEncoder.encode turns spaces into +, and these are decoded by grails as +. So do a replace.
        java.net.URLEncoder.encode(String.join("|", nodes)).replace("+", "%20")
    }

    String getNotFoundPathJson() {
        '''
        {
            path: "${json-unit.any-string}",
            resource: "CatalogueItem",
            id: "${json-unit.any-string}"
        }'''
    }

    String getExpectedSimpleTerminologyJson() {
        return '''{
          "id": "${json-unit.matches:id}",
          "domainType": "Terminology",
          "label": "Simple Test Terminology",
          "availableActions": [
            "show",
            "comment"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "classifiers": [
            {
              "id": "${json-unit.matches:id}",
              "label": "test classifier simple",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            }
          ]
        }'''
    }

    String getExpectedComplexTerminologyJson() {
        return '''{
          "id": "${json-unit.matches:id}",
          "domainType": "Terminology",
          "label": "Complex Test Terminology",
          "availableActions": [
            "show",
            "comment"
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
          ]
        }'''
    }    

    String getExpectedSimpleCodeSetJson() {
        return '''{
          "id": "${json-unit.matches:id}",
          "domainType": "CodeSet",
          "label": "Simple Test CodeSet",
          "availableActions": [
            "show",
            "comment"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "classifiers": [
            {
              "id": "${json-unit.matches:id}",
              "label": "test classifier",
              "lastUpdated": "${json-unit.matches:offsetDateTime}"
            }
          ]
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
            "show",
            "comment"
          ],
          "lastUpdated": "${json-unit.matches:offsetDateTime}"       
        }'''
    }

    String getExpectedComplexDataModelJson() {
        return '''{
            "id": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "label": "Complex Test DataModel",
            "availableActions": [
              "show",
              "comment"
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
            ]  
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
                "show",
                "comment"
            ],
            "lastUpdated": "${json-unit.matches:offsetDateTime}"         
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
                "show",
                "comment"
            ],
            "lastUpdated": "${json-unit.matches:offsetDateTime}"         
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
                "show",
                "comment"
            ],
            "lastUpdated": "${json-unit.matches:offsetDateTime}" 
        }'''
    }

    String getExpectedDataTypeJson() {
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
                "show",
                "comment"
            ],
            "lastUpdated": "${json-unit.matches:offsetDateTime}"        
        }'''
    }

    void 'Get Terminology by path and ID when not logged in'() {
        String node

        //No ID
        when:
        node = makePathNode('te', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID
        when:
        node = makePathNode('te', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //No ID
        when:
        node = makePathNode('te', COMPLEX_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID
        when:
        node = makePathNode('te', COMPLEX_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getComplexTerminologyId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()
    }

    void 'Get Terminology by path and ID when logged in'() {
        String node

        given:
        loginReader()

        //No ID
        when:
        node = makePathNode('te', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedSimpleTerminologyJson()

        //With ID
        when:
        node = makePathNode('te', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedSimpleTerminologyJson()

        //No ID
        when:
        node = makePathNode('te', COMPLEX_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedComplexTerminologyJson()

        //With ID
        when:
        node = makePathNode('te', COMPLEX_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getComplexTerminologyId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedComplexTerminologyJson()
    }

    void 'get a Terminology but use the wrong prefix when not logged in'() {
        String node

        //No ID
        when:
        node = makePathNode('tm', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID (this should work because the ID is used)
        when:
        node = makePathNode('tm', COMPLEX_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()
    }

    void 'get a Terminology but use the wrong prefix when logged in'() {
        String node

        given:
        loginReader()

        //No ID
        when:
        node = makePathNode('tm', SIMPLE_TERMINOLOGY_NAME)
        GET("/api/terminologies/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID (this should work because the ID is used)
        when:
        node = makePathNode('tm', COMPLEX_TERMINOLOGY_NAME)
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK because the ID is used"
        verifyJsonResponse OK, getExpectedSimpleTerminologyJson()
    }

    void 'Get CodeSet by path and ID when not logged in'() {
        String node

        //No ID
        when:
        node = makePathNode('cs', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID
        when:
        node = makePathNode('cs', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()
    }

    void 'Get CodeSet by path and ID when logged in'() {
        String node

        given:
        loginReader()

        //No ID
        when:
        node = makePathNode('cs', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedSimpleCodeSetJson()

        //With ID
        when:
        node = makePathNode('cs', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedSimpleCodeSetJson()
    }

    void 'get a CodeSet but use the wrong prefix when not logged in'() {
        String node

        //No ID
        when:
        node = makePathNode('tm', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID (this should work because the ID is used)
        when:
        node = makePathNode('tm', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()
    }

    void 'get a CodeSet but use the wrong prefix when logged in'() {
        String node

        given:
        loginReader()

        //No ID
        when:
        node = makePathNode('tm', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID (this should work because the ID is used)
        when:
        node = makePathNode('tm', SIMPLE_CODESET_NAME)
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK because the ID is used"
        verifyJsonResponse OK, getExpectedSimpleCodeSetJson()
    }

    void 'get a Term for a Terminology when not logged in'() {
        String node1
        String node2

        //No Terminology ID
        when:
        node1 = makePathNode('te', SIMPLE_TERMINOLOGY_NAME)
        node2 = makePathNode('tm', 'STT01: Simple Test Term 01')
        GET("/api/terminologies/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With Terminology ID and no label
        when:
        node1 = makePathNode('te', '')
        node2 = makePathNode('tm', 'STT01: Simple Test Term 01')
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()
    }

    void 'get a Term for a Terminology when logged in'() {
        String node1
        String node2

        given:
        loginReader()

        //No Terminology ID
        when:
        node1 = makePathNode('te', SIMPLE_TERMINOLOGY_NAME)
        node2 = makePathNode('tm', 'STT01: Simple Test Term 01')
        GET("/api/terminologies/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedSimpleTermJson()

        //With Terminology ID and no label
        when:
        node1 = makePathNode('te', '')
        node2 = makePathNode('tm', 'STT01: Simple Test Term 01')
        GET("/api/terminologies/${getSimpleTerminologyId()}/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedSimpleTermJson()
    }

    void 'get a Term for a CodeSet when not logged in'() {
        String node1
        String node2

        //No CodeSet ID
        when:
        node1 = makePathNode('cs', SIMPLE_CODESET_NAME)
        node2 = makePathNode('tm', 'STT01: Simple Test Term 01')
        GET("/api/codeSets/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With CodeSet ID and no label
        when:
        node1 = makePathNode('cs', '')
        node2 = makePathNode('tm', 'STT01: Simple Test Term 01')
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()
    }

    void 'get a Term for a CodeSet when logged in'() {
        String node1
        String node2

        given:
        loginReader()

        //No CodeSet ID
        when:
        node1 = makePathNode('cs', SIMPLE_CODESET_NAME)
        node2 = makePathNode('tm', 'STT01: Simple Test Term 01')
        GET("/api/codeSets/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedSimpleTermJson()

        //With CodeSet ID and no label
        when:
        node1 = makePathNode('cs', '')
        node2 = makePathNode('tm', 'STT01: Simple Test Term 01')
        GET("/api/codeSets/${getSimpleCodeSetId()}/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedSimpleTermJson()
    }

    void 'Get DataModel by path and ID when not logged in'() {
        String node

        //No ID
        when:
        node = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        GET("/api/dataModels/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID
        when:
        node = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()
    }

    void 'Get DataModel by path and ID when logged in'() {
        String node

        given:
        loginReader()

        //No ID
        when:
        node = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        GET("/api/dataModels/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedComplexDataModelJson()

        //With ID
        when:
        node = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedComplexDataModelJson()
    }

    void 'Get DataClass with DataModel by path and ID when not logged in'() {
        String node1
        String node2

        //No ID
        when:
        node1 = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        node2 = makePathNode('dc', PARENT_DATACLASS_NAME)
        GET("/api/dataModels/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID
        when:
        node1 = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        node2 = makePathNode('dc', PARENT_DATACLASS_NAME)
        GET("/api/dataModels/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()
    }

    void 'Get DataClass with DataModel by path and ID when logged in'() {
        String node1
        String node2

        given:
        loginReader()

        //No ID
        when:
        node1 = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        node2 = makePathNode('dc', PARENT_DATACLASS_NAME)
        GET("/api/dataModels/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedParentDataClassJson()

        //With ID
        when:
        node1 = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        node2 = makePathNode('dc', PARENT_DATACLASS_NAME)
        GET("/api/dataModels/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedParentDataClassJson()
    }

    void 'Get DataClass by path and ID when not logged in'() {
        String node

        //No ID
        when:
        node = makePathNode('dc', PARENT_DATACLASS_NAME)
        GET("/api/dataClasses/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID
        when:
        node = makePathNode('dc', PARENT_DATACLASS_NAME)
        GET("/api/dataClasses/${getParentDataClassId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //No ID
        when:
        node = makePathNode('dc', CHILD_DATACLASS_NAME)
        GET("/api/dataClasses/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID
        when:
        node = makePathNode('dc', CHILD_DATACLASS_NAME)
        GET("/api/dataClasses/${getParentDataClassId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()
    }

    void 'Get DataClass by path and ID when logged in'() {
        String node

        given:
        loginReader()

        //No ID
        when:
        node = makePathNode('dc', PARENT_DATACLASS_NAME)
        GET("/api/dataClasses/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedParentDataClassJson()

        //With ID
        when:
        node = makePathNode('dc', PARENT_DATACLASS_NAME)
        GET("/api/dataClasses/${getParentDataClassId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedParentDataClassJson()

        //No ID
        when:
        node = makePathNode('dc', CHILD_DATACLASS_NAME)
        GET("/api/dataClasses/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedChildDataClassJson()

        //With ID
        when:
        node = makePathNode('dc', CHILD_DATACLASS_NAME)
        GET("/api/dataClasses/${getChildDataClassId()}/path/${makePath([node])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedChildDataClassJson()
    }

    void 'Get DataElement by path and ID when not logged in'() {
        String node1
        String node2

        //No ID
        when:
        node1 = makePathNode('dc', CONTENT_DATACLASS_NAME)
        node2 = makePathNode('de', DATA_ELEMENT_NAME)
        GET("/api/dataClasses/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID
        when:
        node1 = makePathNode('dc', CONTENT_DATACLASS_NAME)
        node2 = makePathNode('de', DATA_ELEMENT_NAME)
        GET("/api/dataClasses/${getContentDataClassId()}/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()
    }

    void 'Get DataElement by DataClass, path and ID when logged in'() {
        String node1
        String node2

        given:
        loginReader()

        //No ID
        when:
        node1 = makePathNode('dc', CONTENT_DATACLASS_NAME)
        node2 = makePathNode('de', DATA_ELEMENT_NAME)
        GET("/api/dataClasses/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedDataElementJson()

        //With ID
        when:
        node1 = makePathNode('dc', CONTENT_DATACLASS_NAME)
        node2 = makePathNode('de', DATA_ELEMENT_NAME)
        GET("/api/dataClasses/${getContentDataClassId()}/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedDataElementJson()
    }

    void 'Get DataType by DataModel, path and ID when not logged in'() {
        String node1
        String node2

        //No ID
        when:
        node1 = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        node2 = makePathNode('dt', DATA_TYPE_NAME)
        GET("/api/dataModels/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()

        //With ID
        when:
        node1 = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        node2 = makePathNode('dt', DATA_TYPE_NAME)
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is Not Found"
        verifyJsonResponse NOT_FOUND, getNotFoundPathJson()
    }

    void 'Get DataType by DataModel, path and ID when logged in'() {
        String node1
        String node2

        given:
        loginReader()

        //No ID
        when:
        node1 = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        node2 = makePathNode('dt', DATA_TYPE_NAME)
        GET("/api/dataModels/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedDataTypeJson()

        //With ID
        when:
        node1 = makePathNode('dm', COMPLEX_DATAMODEL_NAME)
        node2 = makePathNode('dt', DATA_TYPE_NAME)
        GET("/api/dataModels/${getComplexDataModelId()}/path/${makePath([node1, node2])}", STRING_ARG, true)

        then: "The response is OK"
        verifyJsonResponse OK, getExpectedDataTypeJson()
    }
}
