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
package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.json.JsonComparer
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.plugin.json.view.test.JsonViewTest
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

/**
 * @since 06/02/2020
 */
@Slf4j
class CatalogueItemRenderingSpec extends BaseUnitSpec implements JsonViewTest, JsonComparer {

    BasicModel basicModel
    BasicModelItem basicModelItem
    Folder misc

    def setup() {
        mockDomains(Folder, BasicModel, BasicModelItem, Authority)
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        basicModel = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc,
                                    description: 'a very basic model', authority: testAuthority)
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
