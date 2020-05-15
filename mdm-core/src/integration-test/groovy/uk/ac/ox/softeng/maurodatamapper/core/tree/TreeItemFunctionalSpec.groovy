package uk.ac.ox.softeng.maurodatamapper.core.tree

import uk.ac.ox.softeng.maurodatamapper.test.functional.BaseFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.tree.TreeItemController* Controller: treeItem
 *  | GET | /api/tree/search/${search} | Action: search |
 *  | GET | /api/tree/${id}?           | Action: index  |
 */
@Integration
@Slf4j
class TreeItemFunctionalSpec extends BaseFunctionalSpec {

    def setup() {

    }

    @Override
    String getResourcePath() {
        ''
    }

    String getOldTreeResourcePath() {
        'tree'
    }

    String getNewTreeResourcePath() {
        'tree/folders'
    }

    Map getValidJson() {
        [:]
    }

    Map getInvalidJson() {
        [:]
    }

    void '4 : test call to full tree no containers'() {

        when: 'no containers new url'
        def response = GET(newTreeResourcePath, Argument.of(List))

        then: 'response is correct'
        response.status == HttpStatus.OK
        response.body() == []
    }

    void '5 : test single folder in existence'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": false,
    "deleted": false
  }
]'''

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'getting folder tree'
        def folderId = response.body().id
        GET(newTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, exp

        cleanup:
        DELETE("folders/$folderId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '6 : test nested folders'() {
        given:
        String exp = '''[
  {
    "id": "${json-unit.matches:id}",
    "domainType": "Folder",
    "label": "Functional Test Folder",
    "hasChildren": true,
    "deleted": false,
    "children": [
        {
        "id": "${json-unit.matches:id}",
        "domainType": "Folder",
        "label": "Functional Test Folder Child",
        "hasChildren": false,
        "deleted": false,
        "parentFolder": "${json-unit.matches:id}"
        }
    ]
  }
]'''

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'creating new folder'
        def parentId = response.body().id
        POST("folders/$parentId/folders", [label: 'Functional Test Folder Child'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'getting folder tree'
        def childId = response.body().id
        GET(newTreeResourcePath, STRING_ARG)

        then:
        verifyJsonResponse HttpStatus.OK, exp

        cleanup:
        DELETE("folders/$parentId/folders/$childId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
        DELETE("folders/$parentId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    void '7 : Test the show action for a folder id returns not found'() {
        when: 'When the show action is called to retrieve a resource which doesnt exist'
        def id = '1'
        String path = "$newTreeResourcePath/${id}"
        client.toBlocking().exchange(HttpRequest.GET(path), Map)

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND

        when: 'creating new folder'
        POST('folders', [label: 'Functional Test Folder'])

        then:
        verifyResponse HttpStatus.CREATED, response

        when: 'When the show action is called to retrieve a folder'
        def folderId = response.body().id
        id = folderId
        GET("$newTreeResourcePath/${id}")

        then: 'The response is correct'
        verifyResponse(HttpStatus.NOT_FOUND, response)

        cleanup:
        DELETE("folders/$folderId?permanent=true")
        assert response.status() == HttpStatus.NO_CONTENT
    }

    /*
                               '''[
      {
        "deleted": false,
        "domainType": "Folder",
        "children": [
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "documentationVersion": "1.0.0",
            "hasChildren": true,
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test DataModel",
            "superseded": false,
            "type": "Data Standard"
          },
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "Terminology",
            "documentationVersion": "1.0.0",
            "hasChildren": true,
            "finalised": true,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "superseded": false
          },
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "CodeSet",
            "documentationVersion": "1.0.0",
            "hasChildren": false,
            "finalised": true,
            "id": "${json-unit.matches:id}",
            "label": "Simple Test CodeSet",
            "superseded": false
          }
        ],
        "hasChildren": true,
        "id": "${json-unit.matches:id}",
        "label": "Test Folder"
      }
    ]'''

            when: 'logged in as normal user'
            loginEditor()
            response = restGet('')

            then:
            verifyResponse OK, response, '''[
      {
        "deleted": false,
        "domainType": "Folder",
        "children": [
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "documentationVersion": "1.0.0",
            "hasChildren": true,
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test DataModel",
            "superseded": false,
            "type": "Data Standard"
          },
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "documentationVersion": "1.0.0",
            "hasChildren": true,
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Simple Test DataModel",
            "superseded": false,
            "type": "Data Standard"
          },
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "Terminology",
            "documentationVersion": "1.0.0",
            "hasChildren": true,
            "finalised": true,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "superseded": false
          },
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "Terminology",
            "documentationVersion": "1.0.0",
            "hasChildren": false,
            "finalised": true,
            "id": "${json-unit.matches:id}",
            "label": "Simple Test Terminology",
            "superseded": false
          },
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "CodeSet",
            "documentationVersion": "1.0.0",
            "hasChildren": false,
            "finalised": true,
            "id": "${json-unit.matches:id}",
            "label": "Simple Test CodeSet",
            "superseded": false
          }
        ],
        "hasChildren": true,
        "id": "${json-unit.matches:id}",
        "label": "Test Folder"
      }
    ]'''

            when: 'logged in as admin user'
            loginAdmin()
            response = restGet('')

            then:
            verifyResponse OK, response, '''[
      {
        "deleted": false,
        "domainType": "Folder",
        "children": [
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "documentationVersion": "1.0.0",
            "hasChildren": true,
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test DataModel",
            "superseded": false,
            "type": "Data Standard"
          },
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "documentationVersion": "1.0.0",
            "hasChildren": true,
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Simple Test DataModel",
            "superseded": false,
            "type": "Data Standard"
          },
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "Terminology",
            "documentationVersion": "1.0.0",
            "hasChildren": true,
            "finalised": true,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test Terminology",
            "superseded": false
          },
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "Terminology",
            "documentationVersion": "1.0.0",
            "hasChildren": false,
            "finalised": true,
            "id": "${json-unit.matches:id}",
            "label": "Simple Test Terminology",
            "superseded": false
          },
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "CodeSet",
            "documentationVersion": "1.0.0",
            "hasChildren": false,
            "finalised": true,
            "id": "${json-unit.matches:id}",
            "label": "Simple Test CodeSet",
            "superseded": false
          }
        ],
        "hasChildren": true,
        "id": "${json-unit.matches:id}",
        "label": "Test Folder"
      },
      {
        "deleted": false,
        "domainType": "Folder",
        "hasChildren": false,
        "id": "${json-unit.matches:id}",
        "label": "Miscellaneous"
      }
    ]'''
        }

        void 'test call to tree using DataModel id'() {
            given:
            def id = testDataModel.id
            String expectedJson = '''[
      {
        "domainType": "DataClass",
        "hasChildren": true,
        "dataModel": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "parent"
      },
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "dataModel": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "content"
      },
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "dataModel": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "emptyclass"
      }
    ]'''

            when: 'not logged in'
            RestResponse response = restGet("$id")

            then:
            verifyUnauthorised response

            when: 'logged in as reader 3 user'
            loginUser(reader3)
            response = restGet("$id")

            then:
            verifyResponse OK, response, expectedJson

            when: 'logged in as normal user'
            loginEditor()
            response = restGet("$id")

            then:
            verifyResponse OK, response, expectedJson

            when: 'logged in as admin user'
            loginAdmin()
            response = restGet("$id")

            then:
            verifyResponse OK, response, expectedJson
        }

        void 'test call to tree using DataClass id'() {
            given:
            def id = testDataModel.childDataClasses.find {it.label == 'parent'}.id

            when: 'not logged in'
            RestResponse response = restGet("$id")

            then:
            verifyUnauthorised response

            when: 'logged in as reader 3'
            loginUser(reader3)
            response = restGet("$id")

            then:
            verifyResponse OK, response, '''[
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "dataModel": "${json-unit.matches:id}",
        "parentDataClass": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "child"
      }
    ]'''

            when: 'logged in as normal user'
            loginEditor()
            response = restGet("$id")

            then:
            verifyResponse OK, response, '''[
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "dataModel": "${json-unit.matches:id}",
        "parentDataClass": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "child"
      }
    ]'''

            when: 'logged in as admin user'
            loginAdmin()
            response = restGet("$id")

            then:
            verifyResponse OK, response, '''[
      {
        "domainType": "DataClass",
        "hasChildren": false,
        "dataModel": "${json-unit.matches:id}",
        "parentDataClass": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "child"
      }
    ]'''
        }

        void 'test searching for "model"'() {
            given:
            String expectedJson = '''[
      {
        "deleted": false,
        "domainType": "Folder",
        "children": [
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "documentationVersion": "1.0.0",
            "hasChildren": false,
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test DataModel",
            "superseded": false,
            "type": "Data Standard"
          },
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "documentationVersion": "1.0.0",
            "hasChildren": false,
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Simple Test DataModel",
            "superseded": false,
            "type": "Data Standard"
          }
        ],
        "hasChildren": true,
        "id": "${json-unit.matches:id}",
        "label": "Test Folder"
      }
    ]'''
            String search = 'search/model'

            when: 'not logged in'
            RestResponse response = restGet(search)

            then:
            verifyResponse OK, response, '[]'

            when: 'logged in as reader 3 user'
            loginUser(reader3)
            response = restGet(search)

            then:
            verifyResponse OK, response, '''[
      {
        "deleted": false,
        "domainType": "Folder",
        "children": [
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "documentationVersion": "1.0.0",
            "hasChildren": false,
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test DataModel",
            "superseded": false,
            "type": "Data Standard"
          }
        ],
        "hasChildren": true,
        "id": "${json-unit.matches:id}",
        "label": "Test Folder"
      }
    ]'''

            when: 'logged in as normal user'
            loginEditor()
            response = restGet(search)

            then:
            verifyResponse OK, response, expectedJson

            when: 'logged in as admin user'
            loginAdmin()
            response = restGet(search)

            then:
            verifyResponse OK, response, expectedJson
        }

        void 'test searching for "emptyclass"'() {
            given:
            String expectedJson = '''[
      {
        "deleted": false,
        "domainType": "Folder",
        "children": [
          {
            "deleted": false,
            "folder": "${json-unit.matches:id}",
            "domainType": "DataModel",
            "children": [
              {
                "domainType": "DataClass",
                "hasChildren": false,
                "dataModel": "${json-unit.matches:id}",
                "id": "${json-unit.matches:id}",
                "label": "emptyclass"
              }
            ],
            "documentationVersion": "1.0.0",
            "hasChildren": true,
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "Complex Test DataModel",
            "superseded": false,
            "type": "Data Standard"
          }
        ],
        "hasChildren": true,
        "id": "${json-unit.matches:id}",
        "label": "Test Folder"
      }
    ]'''

            when: 'not logged in'
            RestResponse response = restGet("search/emptyclass")

            then:
            verifyResponse OK, response, '[]'

            when: 'logged in as reader 3 user'
            loginUser(reader3)
            response = restGet("search/emptyclass")

            then:
            verifyResponse OK, response, expectedJson

            when: 'logged in as normal user'
            loginEditor()
            response = restGet("search/emptyclass")

            then:
            verifyResponse OK, response, expectedJson

            when: 'logged in as admin user'
            loginAdmin()
            response = restGet("search/emptyclass")

            then:
            verifyResponse OK, response, expectedJson
        }

        void 'test call to full tree with foldersOnly'() {

            when: 'not logged in'
            RestResponse response = restGet('?foldersOnly=true')

            then:
            verifyResponse OK, response, '[]'

            when: 'logged in as reader 3 user'
            loginUser(reader3)
            response = restGet('?foldersOnly=true')

            then:
            verifyResponse OK, response, '''[]'''

            when: 'logged in as normal user'
            loginEditor()
            response = restGet('?foldersOnly=true')

            then:
            verifyResponse OK, response, '''[
      {
        "deleted": false,
        "domainType": "Folder",
        "hasChildren": false,
        "id": "${json-unit.matches:id}",
        "label": "Test Folder"
      }
    ]'''

            when: 'logged in as admin user'
            loginAdmin()
            response = restGet('?foldersOnly=true')

            then:
            verifyResponse OK, response, '''[
       {
        "deleted": false,
        "domainType": "Folder",
        "hasChildren": false,
        "id": "${json-unit.matches:id}",
        "label": "Test Folder"
      },
      {
        "deleted": false,
        "domainType": "Folder",
        "hasChildren": false,
        "id": "${json-unit.matches:id}",
        "label": "Miscellaneous"
      }
    ]'''
        }

        void 'test call to tree using Terminology id'() {
            given:
            def id = complexTestTerminology.id

            when: 'not logged in'
            RestResponse response = restGet("$id")

            then:
            verifyResponse NOT_FOUND, response

            when: 'logged in as reader 3 user'
            loginUser(reader3)
            response = restGet("$id")

            then:
            verifyResponse NOT_FOUND, response

            when: 'logged in as normal user'
            loginEditor()
            response = restGet("$id")

            then:
            verifyResponse NOT_FOUND, response

            when: 'logged in as admin user'
            loginAdmin()
            response = restGet("$id")

            then:
            verifyResponse NOT_FOUND, response
        }
    */

    void '1 : Test the save action correctly persists an instance'() {
        when: 'The save action is executed with valid data'
        client.toBlocking().exchange(HttpRequest.POST(newTreeResourcePath, validJson), Map)

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }

    void '2 : Test the update action correctly updates an instance'() {
        when: 'The update action is executed with valid data'
        String path = "$newTreeResourcePath/1"
        client.toBlocking().exchange(HttpRequest.PUT(path, validJson), Map)

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }

    void '3 : Test the delete action correctly deletes an instance'() {
        when: 'When the delete action is executed on an unknown instance'
        def path = "$newTreeResourcePath/99999"
        client.toBlocking().exchange(HttpRequest.DELETE(path))

        then: 'The response is correct'
        def e = thrown(HttpClientResponseException)
        e.response.status == HttpStatus.NOT_FOUND
    }
}
