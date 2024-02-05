/*
 * Copyright 2020-2024 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkController
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItemService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST
import static uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType.ABSTRACTS
import static uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType.DOES_NOT_ABSTRACT
import static uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType.DOES_NOT_REFINE
import static uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType.REFINES

@Slf4j
class SemanticLinkControllerSpec extends ResourceControllerSpec<SemanticLink> implements
    DomainUnitTest<SemanticLink>,
    ControllerUnitTest<SemanticLinkController> {

    BasicModel basicModel
    BasicModel basicModel2
    BasicModel basicModel3

    def setup() {
        mockDomains(Folder, BasicModel, Authority)
        log.debug('Setting up semantic link controller unit')
        mockDomains(Folder, BasicModel, Edit, SemanticLink)
        Authority testAuthority = new Authority(label: 'Test Authority', url: 'https://localhost', createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'), authority: testAuthority)
        basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'), authority: testAuthority)
        basicModel3 = new BasicModel(label: 'dm3', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'), authority: testAuthority)
        checkAndSave basicModel
        checkAndSave(basicModel2)
        checkAndSave(basicModel3)

        BasicModelItem bmi = new BasicModelItem(label: 'bmi1', createdBy: admin.emailAddress)
        basicModel.addToModelItems(bmi)

        SemanticLink sl1 = new SemanticLink(createdBy: admin.emailAddress, linkType: REFINES)
        basicModel.addToSemanticLinks(sl1)
        sl1.setTargetMultiFacetAwareItem(basicModel2)
        SemanticLink sl2 = new SemanticLink(createdBy: admin.emailAddress, linkType: DOES_NOT_REFINE)
        basicModel.addToSemanticLinks(sl2)
        sl2.setTargetMultiFacetAwareItem(basicModel3)

        checkAndSave(basicModel)

        domain.createdBy = admin.emailAddress
        domain.linkType = ABSTRACTS
        domain.setTargetMultiFacetAwareItem(bmi)
        basicModel3.addToSemanticLinks(domain)
        checkAndSave(domain)

        ModelService basicModelService = Stub() {
            get(_) >> {UUID id -> BasicModel.get(id)}
            getAll(_) >> {List<UUID> ids -> BasicModel.getAll(ids)}
            getDomainClass() >> BasicModel
            handles('BasicModel') >> true
            removeSemanticLinkFromMultiFacetAware(_, _) >> {UUID id, SemanticLink semanticLink ->
                BasicModel bm = BasicModel.get(id)
                bm.semanticLinks.remove(semanticLink)
            }
            save(_) >> {BasicModel model -> model.save()}
        }
        ModelItemService basicModelItemService = Stub() {
            get(_) >> {UUID id -> BasicModelItem.get(id)}
            getAll(_) >> {List<UUID> ids -> BasicModelItem.getAll(ids)}
            getMultiFacetAwareClass() >> BasicModelItem
            handles('BasicModelItem') >> true
            removeSemanticLinkFromMultiFacetAware(_, _) >> {UUID id, SemanticLink semanticLink ->
                BasicModelItem bm = BasicModelItem.get(id)
                bm.semanticLinks.remove(semanticLink)
            }
        }
        SemanticLinkService semanticLinkService = new SemanticLinkService()
        semanticLinkService.catalogueItemServices = [basicModelService, basicModelItemService]
        controller.semanticLinkService = semanticLinkService
    }

    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
      "unconfirmed": false,
      "targetMultiFacetAwareItem": {
        "domainType": "BasicModelItem",
        "model": "${json-unit.matches:id}",
        "id": "${json-unit.matches:id}",
        "label": "bmi1",
        "breadcrumbs": [
          {
            "domainType": "BasicModel",
            "finalised": false,
            "id": "${json-unit.matches:id}",
            "label": "dm1"
          }
        ]
      },
      "domainType": "SemanticLink",
      "sourceMultiFacetAwareItem": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm3"
      },
      "linkType": "Abstracts",
      "id": "${json-unit.matches:id}"
    },
    {
      "unconfirmed": false,
      "targetMultiFacetAwareItem": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm3"
      },
      "domainType": "SemanticLink",
      "sourceMultiFacetAwareItem": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm1"
      },
      "linkType": "Does Not Refine",
      "id": "${json-unit.matches:id}"
    }
  ]
}'''
    }

    @Override
    String getExpectedNullSavedJson() {
        '''{
  "total": 3,
  "errors": [
    {
      "message": "Property [targetMultiFacetAwareItemId] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink] cannot be null"
    },
    {
      "message": "Property [targetMultiFacetAwareItemDomainType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink] cannot''' +
        ''' be null"
    },
    {
      "message": "Property [linkType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink] cannot be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '''{
  "total": 2,
  "errors": [
    {
      "message": "Property [targetMultiFacetAwareItemId] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink] cannot be null"
    },
    {
      "message": "Property [targetMultiFacetAwareItemDomainType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink] cannot''' +
        ''' be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
  "unconfirmed": false,
  "targetMultiFacetAwareItem": {
    "domainType": "BasicModel",
    "id": "${json-unit.matches:id}",
    "label": "dm2"
  },
  "domainType": "SemanticLink",
  "sourceMultiFacetAwareItem": {
    "domainType": "BasicModel",
    "id": "${json-unit.matches:id}",
    "label": "dm3"
  },
  "linkType": "Abstracts",
  "id": "${json-unit.matches:id}"
}'''
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "targetMultiFacetAwareItem": {
    "domainType": "BasicModelItem",
    "model": "${json-unit.matches:id}",
    "id": "${json-unit.matches:id}",
    "label": "bmi1",
    "breadcrumbs": [
      {
        "domainType": "BasicModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "dm1"
      }
    ]
  },
  "domainType": "SemanticLink",
  "unconfirmed": false,
  "sourceMultiFacetAwareItem": {
    "domainType": "BasicModel",
    "id": "${json-unit.matches:id}",
    "label": "dm3"
  },
  "linkType": "Abstracts",
  "id": "${json-unit.matches:id}"
}'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '{"total":1, "errors": [{"message": "Property [linkType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.' +
        'SemanticLink] cannot be null"}]}'
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
  "targetMultiFacetAwareItem": {
    "domainType": "BasicModelItem",
    "model": "${json-unit.matches:id}",
    "id": "${json-unit.matches:id}",
    "label": "bmi1",
    "breadcrumbs": [
      {
        "domainType": "BasicModel",
        "finalised": false,
        "id": "${json-unit.matches:id}",
        "label": "dm1"
      }
    ]
  },
  "domainType": "SemanticLink",
  "unconfirmed": false,
  "sourceMultiFacetAwareItem": {
    "domainType": "BasicModel",
    "id": "${json-unit.matches:id}",
    "label": "dm3"
  },
  "linkType": "Does Not Abstract",
  "id": "${json-unit.matches:id}"
}'''
    }

    @Override
    SemanticLink invalidUpdate(SemanticLink instance) {
        instance.linkType = null
        instance
    }

    @Override
    SemanticLink validUpdate(SemanticLink instance) {
        instance.linkType = DOES_NOT_ABSTRACT
        instance
    }

    @Override
    SemanticLink getInvalidUnsavedInstance() {
        new SemanticLink(linkType: DOES_NOT_REFINE)
    }

    @Override
    SemanticLink getValidUnsavedInstance() {
        new SemanticLink(linkType: ABSTRACTS, targetMultiFacetAwareItem: basicModel2)
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.multiFacetAwareItemDomainType = BasicModel.simpleName
        params.multiFacetAwareItemId = basicModel3.id
    }

    @Override
    String getTemplate() {
        '''
    import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink

model {
    SemanticLink semanticLink
}

json {
    id semanticLink.id
    if(semanticLink.linkType) linkType semanticLink.linkType.label
    domainType semanticLink.domainType

    targetMultiFacetAwareItemId semanticLink.targetMultiFacetAwareItemId
    targetMultiFacetAwareItemDomainType semanticLink.targetMultiFacetAwareItemDomainType
}
    '''
    }
}