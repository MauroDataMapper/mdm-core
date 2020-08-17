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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkController
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkService
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST
import static uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType.NEW_MODEL_VERSION_OF
import static uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType.SUPERSEDED_BY_MODEL

@Slf4j
class VersionLinkControllerSpec extends ResourceControllerSpec<VersionLink> implements
    DomainUnitTest<VersionLink>,
    ControllerUnitTest<VersionLinkController> {

    BasicModel basicModel
    BasicModel basicModel3

    def setup() {
        mockDomains(Folder, BasicModel, Authority)
        log.debug('Setting up semantic link controller unit')
        mockDomains(Folder, BasicModel, Edit, VersionLink)
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                    authority: testAuthority)
        BasicModel basicModel2 = new BasicModel(label: 'dm2', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                                authority: testAuthority)
        basicModel3 = new BasicModel(label: 'dm3', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                     authority: testAuthority)
        BasicModel basicModel4 = new BasicModel(label: 'dm4', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                                authority: testAuthority)
        checkAndSave basicModel
        checkAndSave(basicModel2)
        checkAndSave(basicModel3)
        checkAndSave(basicModel4)

        VersionLink sl1 = new VersionLink(createdBy: admin.emailAddress, linkType: SUPERSEDED_BY_MODEL)
        basicModel.addToVersionLinks(sl1)
        sl1.setTargetModel(basicModel2)
        VersionLink sl2 = new VersionLink(createdBy: admin.emailAddress, linkType: NEW_MODEL_VERSION_OF)
        basicModel.addToVersionLinks(sl2)
        sl2.setTargetModel(basicModel3)

        checkAndSave(basicModel)

        domain.createdBy = admin.emailAddress
        domain.linkType = SUPERSEDED_BY_MODEL
        domain.setTargetModel(basicModel4)
        basicModel3.addToVersionLinks(domain)
        checkAndSave(domain)

        ModelService basicModelService = Stub() {
            getAll(_) >> {List<UUID> ids -> BasicModel.getAll(ids)}
            get(_) >> {UUID id -> BasicModel.get(id)}
            getModelClass() >> BasicModel
            handles('BasicModel') >> true
            removeVersionLinkFromModel(_, _) >> {UUID id, VersionLink versionLink ->
                BasicModel bm = BasicModel.get(id)
                bm.removeFromVersionLinks(versionLink)
            }
        }
        VersionLinkService versionLinkService = new VersionLinkService()
        versionLinkService.catalogueItemServices = [basicModelService]
        versionLinkService.modelServices = [basicModelService]
        controller.versionLinkService = versionLinkService
    }

    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 2,
  "items": [
    {
      "targetModel": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm2"
      },
      "domainType": "VersionLink",
      "sourceModel": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm1"
      },
      "linkType": "Superseded By Model",
      "id": "${json-unit.matches:id}"
    },
    {
      "targetModel": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm3"
      },
      "domainType": "VersionLink",
      "sourceModel": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm1"
      },
      "linkType": "New Model Version Of",
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
      "message": "Property [targetModelId] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink] cannot be null"
    },
    {
      "message": "Property [targetModelDomainType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink] cannot be null"
    },
    {
      "message": "Property [linkType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink] cannot be null"
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
      "message": "Property [targetModelId] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink] cannot be null"
    },
    {
      "message": "Property [targetModelDomainType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink] cannot be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
    "domainType": "VersionLink",
    "linkType": "Superseded By Model",
    "id": "${json-unit.matches:id}",
    "sourceModel": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm1"
    },
    "targetModel": {
        "domainType": "BasicModel",
        "id": "${json-unit.matches:id}",
        "label": "dm3"
    }
}'''
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "targetModel": {
    "domainType": "BasicModel",
    "id": "${json-unit.matches:id}",
    "label": "dm4"
  },
  "domainType": "VersionLink",
  "sourceModel": {
    "domainType": "BasicModel",
    "id": "${json-unit.matches:id}",
    "label": "dm3"
  },
  "linkType": "Superseded By Model",
  "id": "${json-unit.matches:id}"
}'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '{"total":1, "errors": [{"message": "Property [linkType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.' +
        'VersionLink] cannot be null"}]}'
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
  "targetModel": {
    "domainType": "BasicModel",
    "id": "${json-unit.matches:id}",
    "label": "dm4"
  },
  "domainType": "VersionLink",
  "sourceModel": {
    "domainType": "BasicModel",
    "id": "${json-unit.matches:id}",
    "label": "dm3"
  },
  "linkType": "New Model Version Of",
  "id": "${json-unit.matches:id}"
}'''
    }

    @Override
    VersionLink invalidUpdate(VersionLink instance) {
        instance.linkType = null
        instance
    }

    @Override
    VersionLink validUpdate(VersionLink instance) {
        instance.linkType = NEW_MODEL_VERSION_OF
        instance
    }

    @Override
    VersionLink getInvalidUnsavedInstance() {
        new VersionLink(linkType: NEW_MODEL_VERSION_OF)
    }

    @Override
    VersionLink getValidUnsavedInstance() {
        new VersionLink(linkType: SUPERSEDED_BY_MODEL, targetModel: basicModel3)
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.modelDomainType = BasicModel.simpleName
        params.modelId = basicModel.id
    }

    @Override
    String getTemplate() {
        '''
    import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink

model {
    VersionLink versionLink
}

json {
    id versionLink.id
    if(versionLink.linkType) linkType versionLink.linkType.label
    domainType versionLink.domainType

    targetModelId versionLink.targetModelId
    targetModelDomainType versionLink.targetModelDomainType
}
    '''
    }
}