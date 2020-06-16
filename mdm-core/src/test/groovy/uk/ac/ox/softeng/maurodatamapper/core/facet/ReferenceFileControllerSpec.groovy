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

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFileController
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFileService
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Slf4j
class ReferenceFileControllerSpec extends ResourceControllerSpec<ReferenceFile> implements
    DomainUnitTest<ReferenceFile>,
    ControllerUnitTest<ReferenceFileController> {

    BasicModel basicModel

    def setup() {
        mockDomains(Folder, BasicModel)
        log.debug('Setting up referenceFile controller unit')
        checkAndSave(new Folder(label: 'catalogue', createdBy: admin.emailAddress))
        basicModel = new BasicModel(label: 'dm1', createdBy: admin.emailAddress, folder: Folder.findByLabel('catalogue'))
        checkAndSave(basicModel)
        Path lf = Paths.get('grails-app/conf/logback.groovy')

        domain.fileName = lf.fileName
        domain.fileContents = Files.readAllBytes(lf)
        domain.fileType = Files.probeContentType(lf) ?: 'Unknown'
        domain.createdBy = admin.emailAddress

        basicModel.addToReferenceFiles(domain)
        basicModel.addToReferenceFiles(fileName: 'test', fileType: 'text', fileContents: 'blah'.bytes, createdBy: editor.emailAddress)
        basicModel.addToReferenceFiles(fileName: 'test2', fileType: 'text/plain', fileContents: 'jhsdkjfhsdgfsdnmbhfjhsdjghsdjgjfhsd'.bytes,
                                       createdBy: reader1.emailAddress)
        checkAndSave(basicModel)

        controller.referenceFileService = Stub(ReferenceFileService) {
            findAllByCatalogueItemId(basicModel.id, _) >> basicModel.referenceFiles.toList()
            findCatalogueItemByDomainTypeAndId(BasicModel.simpleName, _) >> {String domain, UUID bid -> basicModel.id == bid ? basicModel : null}
            findByCatalogueItemIdAndId(_, _) >> {UUID iid, Serializable mid ->
                if (iid != basicModel.id) return null
                mid == domain.id ? domain : null
            }
        }
    }

    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 3,
  "items": [
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "fileName": "logback.groovy",
      "domainType": "ReferenceFile",
      "fileSize": 4973,
      "id": "${json-unit.matches:id}",
      "fileContents": "${json-unit.matches:fileContents}",
      "fileType": "Unknown"
    },
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "fileName": "test",
      "domainType": "ReferenceFile",
      "fileSize": 4,
      "id": "${json-unit.matches:id}",
      "fileContents": "${json-unit.matches:fileContents}",
      "fileType": "text"
    },
    {
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "fileName": "test2",
      "domainType": "ReferenceFile",
      "fileSize": 35,
      "id": "${json-unit.matches:id}",
      "fileContents": "${json-unit.matches:fileContents}",
      "fileType": "text/plain"
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
      "message": "Property [fileSize] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile] cannot be null"
    },
    {
      "message": "Property [fileContents] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile] cannot be null"
    },
    {
      "message": "Property [fileName] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile] cannot be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '''{
  "total": 3,
  "errors": [
    {
      "message": "Property [fileSize] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile] cannot be null"
    },
    {
      "message": "Property [fileContents] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile] cannot be null"
    },
    {
      "message": "Property [fileName] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile] cannot be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "fileName": "test3",
      "domainType": "ReferenceFile",
      "fileSize": 5,
      "id": "${json-unit.matches:id}",
      "fileContents": "${json-unit.matches:fileContents}",
      "fileType": "text"
    }'''
    }

    @Override
    String getExpectedShowJson() {
        '''{}'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '''{
  "total": 1,
  "errors": [
    {
      "message": "Property [fileType] of class [class uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile] cannot be null"
    }
  ]
}'''
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
      "lastUpdated": "${json-unit.matches:offsetDateTime}",
      "fileName": "logback.groovy",
      "domainType": "ReferenceFile",
      "fileSize": 7,
      "id": "${json-unit.matches:id}",
      "fileContents": "${json-unit.matches:fileContents}",
      "fileType": "text/plain"
    }'''
    }

    @Override
    ReferenceFile invalidUpdate(ReferenceFile instance) {
        instance.fileType = ''
        instance
    }

    @Override
    ReferenceFile validUpdate(ReferenceFile instance) {
        instance.fileContents = 'updated'.bytes
        instance.fileType = 'text/plain'
        instance
    }

    @Override
    ReferenceFile getInvalidUnsavedInstance() {
        new ReferenceFile()
    }

    @Override
    ReferenceFile getValidUnsavedInstance() {
        new ReferenceFile(fileName: 'test3', fileType: 'text', fileContents: 'hello'.bytes, createdBy: editor.emailAddress)
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.catalogueItemDomainType = BasicModel.simpleName
        params.catalogueItemId = basicModel.id
    }
}