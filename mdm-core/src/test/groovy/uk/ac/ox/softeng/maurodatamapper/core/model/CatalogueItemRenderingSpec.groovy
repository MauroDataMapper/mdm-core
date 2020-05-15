package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.plugin.json.view.test.JsonViewTest
import groovy.util.logging.Slf4j

/**
 * @since 06/02/2020
 */
@Slf4j
class CatalogueItemRenderingSpec extends BaseUnitSpec implements JsonViewTest, JsonComparer {

    BasicModel basicModel
    BasicModelItem basicModelItem
    Folder misc

    def setup() {
        mockDomains(Folder, BasicModel, BasicModelItem)
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        basicModel = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc,
                                    description: 'a very basic model')
        basicModelItem = new BasicModelItem(createdBy: editor.emailAddress, label: 'content', description: 'some sort of content in a model')
        basicModel.addToModelItems(basicModelItem)
        checkAndSave(misc)
        checkAndSave(basicModel)

    }

    void 'test rendering of model using base catalogue item'() {
        when:
        def json = render(template: "/catalogueItem/baseCatalogueItem", model: [catalogueItem: basicModel])

        then:
        verifyJson('''{
  "id": "${json-unit.matches:id}",
  "domainType": "BasicModel",
  "label": "test"
}''', json.jsonText)
    }

    void 'test rendering of model item base catalogue item with breadcrumbs'() {
        when:
        def json = render(template: "/catalogueItem/baseCatalogueItem", model: [catalogueItem: basicModelItem])

        then:
        verifyJson('''{
  "id": "${json-unit.matches:id}",
  "domainType": "BasicModelItem",
  "label": "content",
  "model": "${json-unit.matches:id}",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test",
      "domainType": "BasicModel",
      "finalised": false
    }
  ]
}''', json.jsonText)
    }

    void 'test rendering model catalogue item'() {
        when:
        def json = render(template: "/catalogueItem/catalogueItem", model: [catalogueItem: basicModel])

        then:
        verifyJson('''{
  "id": "${json-unit.matches:id}",
  "domainType": "BasicModel",
  "label": "test",
  "description": "a very basic model"
}''', json.jsonText)
    }

    void 'test rendering of model item catalogue item'() {
        when:
        def json = render(template: "/catalogueItem/catalogueItem", model: [catalogueItem: basicModelItem])

        then:
        verifyJson('''{
  "id": "${json-unit.matches:id}",
  "domainType": "BasicModelItem",
  "label": "content",
  "model": "${json-unit.matches:id}",
  "description": "some sort of content in a model",
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test",
      "domainType": "BasicModel",
      "finalised": false
    }
  ]
}''', json.jsonText)
    }

    void 'test rendering model full catalogue item'() {
        when:
        def json = render(template: "/catalogueItem/fullCatalogueItem", model: [catalogueItem            : basicModel,
                                                                                userSecurityPolicyManager:
                                                                                    PublicAccessSecurityPolicyManager.instance])

        then:
        verifyJson('''{
  "id": "${json-unit.matches:id}",
  "domainType": "BasicModel",
  "label": "test",
  "description": "a very basic model",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "availableActions": ["delete", "show", "update"]
}''', json.jsonText)
    }

    void 'test rendering of model item full catalogue item'() {
        when:
        def json = render(template: "/catalogueItem/fullCatalogueItem", model: [catalogueItem            : basicModelItem,
                                                                                userSecurityPolicyManager:
                                                                                    PublicAccessSecurityPolicyManager.instance])

        then:
        verifyJson('''{
  "id": "${json-unit.matches:id}",
  "domainType": "BasicModelItem",
  "label": "content",
  "model": "${json-unit.matches:id}",
  "description": "some sort of content in a model",
  "lastUpdated": "${json-unit.matches:offsetDateTime}",
  "availableActions": ["delete", "show", "update"],
  "breadcrumbs": [
    {
      "id": "${json-unit.matches:id}",
      "label": "test",
      "domainType": "BasicModel",
      "finalised": false
    }
  ]
}''', json.jsonText)
    }
}
