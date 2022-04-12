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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataController
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.MultiFacetAware
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j
import spock.lang.Unroll

import java.time.OffsetDateTime

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

import static io.micronaut.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
class MetadataControllerSpec extends ResourceControllerSpec<Metadata> implements
    DomainUnitTest<Metadata>,
    ControllerUnitTest<MetadataController> {

    BasicModel basicModel

    def setup() {
        mockDomains(Folder, BasicModel, Authority)
        log.debug('Setting up metadata controller unit')
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        Authority testAuthority = new Authority(label: 'Test Authority', url: 'https://localhost', createdBy: UNIT_TEST)
        checkAndSave(testAuthority)
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'),
                                    authority: testAuthority)
        checkAndSave(basicModel)
        domain.properties = [namespace  : 'http://test.com', key: 'existing', value: 'v1', createdBy: admin.emailAddress,
                             dateCreated: OffsetDateTime.now()]
        basicModel.addToMetadata(domain)
        checkAndSave(basicModel)

        controller.metadataService = Stub(MetadataService) {
            findAllByMultiFacetAwareItemId(basicModel.id, _) >> basicModel.metadata.toList()
            findMultiFacetAwareItemByDomainTypeAndId(BasicModel.simpleName, _) >>
            {String domain, UUID bid -> basicModel.id == bid ? basicModel : null}
            findByMultiFacetAwareItemIdAndId(_, _) >> {UUID iid, Serializable mid ->
                if (iid != basicModel.id) return null
                mid == domain.id ? domain : null
            }
            //findByMultiFacetAwareItemIdAndId(basicModelId, metadataId) >> basicModel.metadata[0]
            validate(_) >> {Metadata res ->
                boolean valid = res.validate()
                if (!valid) return res

                MultiFacetAware multiFacetAwareItem = res.multiFacetAwareItem ?: basicModel

                if (multiFacetAwareItem.metadata.any {md -> md != res && md.namespace == res.namespace && md.key == res.key}) {
                    res.errors.rejectValue('key', 'default.not.unique.message', ['key', Metadata.toString(), res.key].toArray(),
                                           'Property [{0}] of class [{1}] with value [{2}] must be unique')
                }
                res
            }
            addFacetToDomain(_, _, _) >> {Metadata md, String domain, UUID bid ->
                if (basicModel.id == bid) {
                    basicModel.addToMetadata(md)
                    md.multiFacetAwareItem = basicModel
                }
            }
        }
    }

    @Unroll
    void 'Test the save action with using the same key'() {
        given:
        givenParameters()

        when:
        requestJson = new Metadata(namespace: domain.namespace, key: domain.key, value: 'v3', createdBy: reader1.emailAddress)
        request.method = 'POST'
        controller.save()

        then:
        verifyJsonResponse UNPROCESSABLE_ENTITY, '''{
  "total": 1,
  "errors": [
    {"message": "Property [key] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata] with value [existing] must be unique"}
  ]
}'''
    }

    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 1,
  "items": [
    {
      "id": "${json-unit.matches:id}",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "namespace": "http://test.com",
      "value": "v1",
      "key": "existing"
    }
  ]
}'''
    }

    @Override
    String getExpectedNullSavedJson() {
        '''{
  "total": 2,
  "errors": [
    {"message": "Property [namespace] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata] cannot be null"},
    {"message": "Property [key] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata] cannot be null"}
  ]
}'''
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '''{
  "total": 1,
  "errors": [
    {"message": "Property [key] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata] cannot be null"}
  ]
}'''
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
        "lastUpdated": "${json-unit.matches:offsetDateTime}",
        "namespace": "http://test.com",
        "id": "${json-unit.matches:id}",
        "value": "v2",
        "key": "valid"
        }'''
    }

    @Override
    String getExpectedShowJson() {
        '''{
      "id": "${json-unit.matches:id}",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "namespace": "http://test.com",
      "value": "v1",
      "key": "existing"
    }'''
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
      "id": "${json-unit.matches:id}",
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "namespace": "http://test.com",
      "value": "v3",
      "key": "existing"
    }'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '''{
  "total": 1,
  "errors": [
    {"message": "Property [key] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata] cannot be null"}
  ]
}'''
    }

    @Override
    Metadata invalidUpdate(Metadata instance) {
        instance.key = ''
        instance
    }

    @Override
    Metadata validUpdate(Metadata instance) {
        instance.value = 'v3'
        instance
    }

    @Override
    Metadata getInvalidUnsavedInstance() {
        new Metadata(namespace: 'http://test.com')
    }

    @Override
    Metadata getValidUnsavedInstance() {
        new Metadata(namespace: 'http://test.com', key: 'valid', value: 'v2')
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.multiFacetAwareItemDomainType = BasicModel.simpleName
        params.multiFacetAwareItemId = basicModel.id
    }
}