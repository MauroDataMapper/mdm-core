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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationController
import uk.ac.ox.softeng.maurodatamapper.core.facet.AnnotationService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

@Slf4j
class AnnotationControllerSpec extends ResourceControllerSpec<Annotation> implements
    DomainUnitTest<Annotation>,
    ControllerUnitTest<AnnotationController> {

    BasicModel basicModel

    def setup() {
        mockDomains(Folder, BasicModel, Annotation, Authority)
        log.debug('Setting up annotation controller unit')
        Authority testAuthority = new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                    authority: testAuthority)
        checkAndSave basicModel

        basicModel.addToAnnotations(createdBy: admin.emailAddress, label: 'annotation 1')
        checkAndSave basicModel

        params.facetOwnerId = basicModel.id

        domain.createdBy = editor.emailAddress
        domain.label = 'annotation 2'
        domain.description = 'something to talk about'

        domain.addToChildAnnotations(createdBy: reader1.emailAddress, description: 'annotation 2.1')
        domain.addToChildAnnotations(new Annotation(createdBy: reader1.emailAddress, description: 'annotation 2.2'))
        domain.addToChildAnnotations(new Annotation(createdBy: editor.emailAddress, description: 'annotation 2.3'))
        basicModel.addToAnnotations(domain)

        checkAndSave basicModel

        basicModel.addToAnnotations(createdBy: admin.emailAddress, label: 'annotation 3')

        checkAndSave basicModel

        basicModel.addToAnnotations(createdBy: admin.emailAddress, label: 'annotation 4')

        checkAndSave basicModel

        controller.annotationService = Mock(AnnotationService) {
            findAllByCatalogueItemId(basicModel.id, _) >> basicModel.annotations.toList()
            findCatalogueItemByDomainTypeAndId(BasicModel.simpleName, _) >> {String domain, UUID bid -> basicModel.id == bid ? basicModel : null}
            findByCatalogueItemIdAndId(_, _) >> {UUID iid, Serializable mid ->
                if (iid != basicModel.id) return null
                mid == domain.id ? domain : null
            }
            findAllWhereRootAnnotationOfCatalogueItemId(_, _) >> {
                Annotation.whereRootAnnotationOfCatalogueItemId(basicModel.id).list()
            }
        }
    }

    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 4,
  "items": [
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "admin@maurodatamapper.com",
      "id": "${json-unit.matches:id}",
      "label": "annotation 4"
    },
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "admin@maurodatamapper.com",
      "id": "${json-unit.matches:id}",
      "label": "annotation 3"
    },
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "editor@test.com",
      "description": "something to talk about",
      "id": "${json-unit.matches:id}",
      "label": "annotation 2",
      "childAnnotations": [
        {
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "createdBy": "reader1@test.com",
          "description": "annotation 2.1",
          "id": "${json-unit.matches:id}"
        },
        {
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "createdBy": "reader1@test.com",
          "description": "annotation 2.2",
          "id": "${json-unit.matches:id}"
        },
        {
          "lastUpdated": "${json-unit.matches:offsetDateTime}",
          "createdBy": "editor@test.com",
          "description": "annotation 2.3",
          "id": "${json-unit.matches:id}"
        }
      ]
    },
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "createdBy": "admin@maurodatamapper.com",
      "id": "${json-unit.matches:id}",
      "label": "annotation 1"
    }
  ]
}'''
    }

    @Override
    String getExpectedNullSavedJson() {
        '''{
  "total": 1,
  "errors": [
    {"message": "Property [label] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation] cannot be null"}
  ]
}'''
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '''{
  "total": 1,
  "errors": [
    {"message": "Property [label] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation] cannot be null"}
  ]
}'''
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "createdBy": "unlogged_user@mdm-core.com",
    "description": "a description",
    "id": "${json-unit.matches:id}",
    "label": "valid"
}
'''
    }

    @Override
    String getExpectedShowJson() {
        '''{
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "createdBy": "editor@test.com",
    "description": "something to talk about",
    "id": "${json-unit.matches:id}",
    "label": "annotation 2",
    "childAnnotations": [
        {
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "createdBy": "reader1@test.com",
            "description": "annotation 2.1",
            "id": "${json-unit.matches:id}"
        },
        {
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "createdBy": "reader1@test.com",
            "description": "annotation 2.2",
            "id": "${json-unit.matches:id}"
        },
        {
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "createdBy": "editor@test.com",
            "description": "annotation 2.3",
            "id": "${json-unit.matches:id}"
        }
    ]
}'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '''{
  "total": 1,
  "errors": [
    {"message": "Property [label] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation] cannot be null"}
  ]
}'''
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
    "lastUpdated": "${json-unit.matches:offsetDateTime}",
    "createdBy": "editor@test.com",
    "description": "something to talk about added an updated",
    "id": "${json-unit.matches:id}",
    "label": "annotation 2",
    "childAnnotations": [
        {
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "createdBy": "reader1@test.com",
            "description": "annotation 2.1",
            "id": "${json-unit.matches:id}"
        },
        {
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "createdBy": "reader1@test.com",
            "description": "annotation 2.2",
            "id": "${json-unit.matches:id}"
        },
        {
            "lastUpdated": "${json-unit.matches:offsetDateTime}",
            "createdBy": "editor@test.com",
            "description": "annotation 2.3",
            "id": "${json-unit.matches:id}"
        }
    ]
}'''
    }

    @Override
    Annotation invalidUpdate(Annotation instance) {
        instance.label = null
        instance
    }

    @Override
    Annotation validUpdate(Annotation instance) {
        instance.description += ' added an updated'
        instance
    }

    @Override
    Annotation getInvalidUnsavedInstance() {
        new Annotation()
    }

    @Override
    Annotation getValidUnsavedInstance() {
        new Annotation(label: 'valid', description: 'a description')
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.catalogueItemDomainType = BasicModel.simpleName
        params.catalogueItemId = basicModel.id
    }
}