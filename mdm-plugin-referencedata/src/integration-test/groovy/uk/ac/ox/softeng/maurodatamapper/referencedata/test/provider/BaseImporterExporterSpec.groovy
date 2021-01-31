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
/*package uk.ac.ox.softeng.maurodatamapper.referencedata.test.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.core.provider.exporter.ExporterProviderService
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.test.BaseReferenceDataModelIntegrationSpec

import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import grails.gorm.transactions.Rollback
import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import spock.lang.Shared

import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
*/
/**
 * @since 17/09/2020
 */
/*@Slf4j
abstract class BaseImporterExporterSpec extends BaseReferenceDataModelIntegrationSpec {

    @Shared
    Path resourcesPath

    @Shared
    UUID exampleReferenceDataModelId

    @Shared
    UUID secondExampleReferenceDataModelId

    abstract ImporterProviderService getImporterService()
    abstract ExporterProviderService getExporterService()
    abstract void validateExportedModel(String testName, String exportedModel)

    abstract String getImportType()


    void 'RDM01: test that trying to export when specifying a null referenceDataModelId fails with an exception'() {
        given:
        setupData()

        when:
        exportModel(null)

        then:
        ApiInternalException exception = thrown(ApiInternalException)
        exception.errorCode == 'RDMEP01'
    }

    void 'RDM02: test exporting and reimporting the bootstrapped example reference data model'() {
        given:
        setupData()

        expect:
        ReferenceDataModel.count() == 2

        when:
        String exported = exportModel(exampleReferenceDataModelId)

        then:
        validateExportedModel('bootstrapExample', exported.replace(/Test Authority/, 'Mauro Data Mapper'))

        //note: importing does not actually save
        when:
        ReferenceDataModel imported = importerService.importReferenceDataModel(admin, exported.bytes)

        then:
        assert imported

        and:
        imported.classifiers
        imported.classifiers.size() == 1
        imported.classifiers[0].label == 'test classifier simple'

        when:
        imported.folder = testFolder
        ObjectDiff diff = referenceDataModelService.diff(referenceDataModelService.get(exampleReferenceDataModelId), imported)

        then:
        diff.objectsAreIdentical()
    }


    void 'RDM03: test empty data import'() {
        given:
        setupData()

        when:
        String data = ''
        importerService.importReferenceDataModel(admin, data.bytes)

        then:
        thrown(ApiBadRequestException)
    }

    void 'RDM04: test simple data import and export'() {
        given:
        setupData()

        expect:
        ReferenceDataModel.count() == 2

        when:
        String data = new String(loadTestFile('importSimple'))
        log.debug("importing ${data}")
        and:
        ReferenceDataModel rdm = importAndConfirm(data.bytes)

        then:
        rdm.label == 'importSimple Reference Data Model'
        rdm.author == 'Test Author'
        rdm.organisation == 'Test Organisation'
        rdm.documentationVersion.toString() == '1.0.0'
        rdm.finalised == false
        rdm.authority.label == 'Mauro Data Mapper'
        rdm.authority.url == 'http://localhost'
        !rdm.aliases
        !rdm.annotations
        !rdm.metadata
        !rdm.classifiers
        !rdm.referenceDataTypes
        !rdm.referenceDataElements
        !rdm.referenceDataValues


        when:
        String exported = exportModel(rdm.id)

        then:
        validateExportedModel('importSimple', exported)
    }

    void 'RDM05: test simple data with aliases import and export'() {
        given:
        setupData()

        expect:
        ReferenceDataModel.count() == 2

        when:
        String data = new String(loadTestFile('importSimpleWithAliases'))
        log.debug("importing ${data}")
        and:
        ReferenceDataModel rdm = importAndConfirm(data.bytes)

        then:
        rdm.label == 'importSimple Reference Data Model'
        rdm.author == 'Test Author'
        rdm.organisation == 'Test Organisation'
        rdm.documentationVersion.toString() == '1.0.0'
        rdm.finalised == false
        rdm.authority.label == 'Mauro Data Mapper'
        rdm.authority.url == 'http://localhost'
        rdm.aliases.size() == 2
        'Alias 1' in rdm.aliases
        'Alias 2' in rdm.aliases
        !rdm.annotations
        !rdm.metadata
        !rdm.classifiers
        !rdm.referenceDataTypes
        !rdm.referenceDataElements
        !rdm.referenceDataValues


        when:
        String exported = exportModel(rdm.id)

        then:
        validateExportedModel('importSimpleWithAliases', exported)
    }

    void 'RDM06: test simple data with annotations import and export'() {
        given:
        setupData()

        expect:
        ReferenceDataModel.count() == 2

        when:
        String data = new String(loadTestFile('importSimpleWithAnnotations'))
        log.debug("importing ${data}")
        and:
        ReferenceDataModel rdm = importAndConfirm(data.bytes)

        then:
        rdm.label == 'importSimple Reference Data Model'
        rdm.author == 'Test Author'
        rdm.organisation == 'Test Organisation'
        rdm.documentationVersion.toString() == '1.0.0'
        rdm.finalised == false
        rdm.authority.label == 'Mauro Data Mapper'
        rdm.authority.url == 'http://localhost'
        !rdm.aliases
        rdm.annotations.size() == 1
        !rdm.metadata
        !rdm.classifiers
        !rdm.referenceDataTypes
        !rdm.referenceDataElements
        !rdm.referenceDataValues

        when:
        Annotation ann = rdm.annotations[0]

        then:
        ann.description == 'test annotation 1 description'
        ann.label == 'test annotation 1 label'          

        when:
        String exported = exportModel(rdm.id)

        then:
        validateExportedModel('importSimpleWithAnnotations', exported)
    }

    void 'RDM07: test simple data with metadata import and export'() {
        given:
        setupData()

        expect:
        ReferenceDataModel.count() == 2

        when:
        String data = new String(loadTestFile('importSimpleWithMetadata'))
        log.debug("importing ${data}")
        and:
        ReferenceDataModel rdm = importAndConfirm(data.bytes)

        then:
        rdm.label == 'importSimple Reference Data Model'
        rdm.author == 'Test Author'
        rdm.organisation == 'Test Organisation'
        rdm.documentationVersion.toString() == '1.0.0'
        rdm.finalised == false
        rdm.authority.label == 'Mauro Data Mapper'
        rdm.authority.url == 'http://localhost'
        !rdm.aliases
        !rdm.annotations
        rdm.metadata.size() == 3
        !rdm.classifiers
        !rdm.referenceDataTypes
        !rdm.referenceDataElements
        !rdm.referenceDataValues
         

        when:
        String exported = exportModel(rdm.id)

        then:
        validateExportedModel('importSimpleWithMetadata', exported)
    }

    void 'RDM08: test simple data with classifiers import and export'() {
        given:
        setupData()

        expect:
        ReferenceDataModel.count() == 2

        when:
        String data = new String(loadTestFile('importSimpleWithClassifiers'))
        log.debug("importing ${data}")
        and:
        ReferenceDataModel rdm = importAndConfirm(data.bytes)

        then:
        rdm.label == 'importSimple Reference Data Model'
        rdm.author == 'Test Author'
        rdm.organisation == 'Test Organisation'
        rdm.documentationVersion.toString() == '1.0.0'
        rdm.finalised == false
        rdm.authority.label == 'Mauro Data Mapper'
        rdm.authority.url == 'http://localhost'
        !rdm.aliases
        !rdm.annotations
        !rdm.metadata
        rdm.classifiers.size() == 2
        !rdm.referenceDataTypes
        !rdm.referenceDataElements
        !rdm.referenceDataValues
         

        when:
        String exported = exportModel(rdm.id)

        then:
        validateExportedModel('importSimpleWithClassifiers', exported)
    }

    void 'RDM09: test reference data import and export'() {
        given:
        setupData()

        expect:
        ReferenceDataModel.count() == 2

        when:
        String data = new String(loadTestFile('importReferenceDataModel'))
        log.debug("importing ${data}")
        and:
        ReferenceDataModel rdm = importAndConfirm(data.bytes)

        then:
        rdm.label == 'Imported Reference Data Model'
        rdm.author == 'Test Author'
        rdm.organisation == 'Test Organisation'
        rdm.documentationVersion.toString() == '1.0.0'
        rdm.finalised == false
        rdm.authority.label == 'Mauro Data Mapper'
        rdm.authority.url == 'http://localhost'
        !rdm.aliases
        !rdm.annotations
        
        //Metadata
        rdm.metadata.size() == 3

        //Classifiers
        rdm.classifiers.size() == 1
        rdm.classifiers[0].label == "An imported classifier"
        
        //Reference Data Types
        rdm.referenceDataTypes.size() == 2
        
        //Reference Data Elements
        rdm.referenceDataElements.size() == 2

        //Reference Data Values (100 rows of 2 columns)
        rdm.referenceDataValues.size() == 200
         

        when:
        String exported = exportModel(rdm.id)

        then:
        validateExportedModel('importReferenceDataModel', exported)
    }
}*/
