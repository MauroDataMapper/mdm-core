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
package uk.ac.ox.softeng.maurodatamapper.terminology.test.provider

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.terminology.Terminology
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.DataBindTerminologyImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.terminology.provider.importer.parameter.TerminologyImporterProviderServiceParameters

import grails.gorm.transactions.Rollback
import groovy.util.logging.Slf4j
import spock.lang.Ignore
import spock.lang.Shared

/**
 * @since 15/11/2017
 */
@Rollback
@Slf4j
@SuppressWarnings("DuplicatedCode")
//@Stepwise
abstract class DataBindTerminologyImporterProviderServiceSpec<K extends DataBindTerminologyImporterProviderService> extends BaseImportExportTerminologySpec {

    abstract K getImporterService()

    @Shared
    TerminologyImporterProviderServiceParameters basicParameters

    def setupSpec() {
        basicParameters = new TerminologyImporterProviderServiceParameters().tap {
            importAsNewBranchModelVersion = false
            importAsNewDocumentationVersion = false
            finalised = false
        }
    }

    Terminology importAndSave(byte[] bytes) {
        log.trace('Importing:\n {}', new String(bytes))

        Terminology imported = importerService.importTerminology(admin, bytes)
        assert imported
        imported.folder = testFolder
        log.debug('Check and save imported model')
        importerService.checkImport(admin, imported, basicParameters)
        check(imported)
        terminologyService.saveModelWithContent(imported)
        sessionFactory.currentSession.flush()
        assert terminologyService.count() == 3
        log.debug('Terminology saved')
        terminologyService.get(imported.id)
    }

    Terminology importAndConfirm(byte[] bytes) {
        Terminology tm = importAndSave(bytes)
        confirmTerminology(tm)
        tm
    }

    void 'I01 : test empty data import'() {
        given:
        setupData()

        when:
        String data = ''
        importerService.importTerminology(admin, data.bytes)

        then:
        thrown(ApiBadRequestException)

    }

    @Ignore
    void 'I02 : test simple data import'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        Terminology terminology = importAndConfirm(loadTestFile('simpleImport'))

        then:
        !terminology.annotations
        !terminology.metadata
        !terminology.classifiers
        !terminology.termRelationshipTypes
        terminology.terms.size() == 2

        when:
        Term a = terminology.terms.find {it.code == 'STT01'}
        Term b = terminology.terms.find {it.code == 'STT02'}

        then:
        a
        a.definition == 'Simple Test Term 01'
        a.depth == 1

        and:
        b
        b.definition == 'Simple Test Term 02'
        b.depth == 1

    }

    void 'I03 : test inc classifiers import'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        Terminology terminology = importAndConfirm(loadTestFile('terminologyWithClassifiers'))

        then:
        !terminology.annotations
        !terminology.metadata
        terminology.classifiers.size() == 2
        !terminology.termRelationshipTypes

    }

    void 'I04 : test importing aliases'() {

        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        Terminology terminology = importAndConfirm(loadTestFile('terminologyWithAliases'))

        then:
        terminology.aliases.size() == 2
        'Alias 1' in terminology.aliases
        'Alias 2' in terminology.aliases
    }

    void 'I05 : test inc metadata data import'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        Terminology terminology = importAndConfirm(loadTestFile('terminologyWithMetadata'))

        then:
        !terminology.annotations
        !terminology.classifiers
        !terminology.termRelationshipTypes

        and:
        terminology.metadata.size() == 3

        and:
        terminology.metadata.every {it.multiFacetAwareItemId == terminology.id}
        terminology.metadata.any {
            it.namespace == 'terminology.test.com/simple' &&
            it.key == 'mdk1' &&
            it.value == 'mdv1'
        }
        terminology.metadata.any {
            it.namespace == 'terminology.test.com/simple' &&
            it.key == 'mdk2' &&
            it.value == 'mdv2'
        }
        terminology.metadata.any {
            it.namespace == 'terminology.test.com' &&
            it.key == 'mdk2' &&
            it.value == 'mdv2'
        }
    }

    void 'I06 : test inc annotation data import'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        Terminology terminology = importAndConfirm(loadTestFile('terminologyWithAnnotations'))

        then:
        !terminology.metadata
        !terminology.classifiers
        !terminology.termRelationshipTypes

        and:
        terminology.annotations.size() == 1

        when:
        Annotation ann = terminology.annotations[0]

        then:
        ann.description == 'test annotation 1 description'
        ann.label == 'test annotation 1 label'
        ann.multiFacetAwareItemId == terminology.id
    }

    void 'I07 : test complex import'() {
        given:
        setupData()

        expect:
        Terminology.count() == 2

        when:
        Terminology terminology = importAndConfirm(loadTestFile('complexImport'))


        then:
        terminology.annotations.size() == 2
        terminology.metadata.size() == 3
        terminology.classifiers.size() == 2
        terminology.termRelationshipTypes.size() == 4
        terminology.terms.size() == 101

        and:
        for (int i = 0; i <= 100; i++) {
            terminology.terms.any {it.code == "CTT${i}" && it.definition == "Complex Test Term ${i}"}
        }
    }

    void 'I08 : test that trying to import multiple terminologies fails'() {
        given:
        setupData()

        expect:
        !importerService.canImportMultipleDomains()

        when:
        importerService.importTerminologies(admin, loadTestFile('simpleImport'))

        then:
        ApiBadRequestException exception = thrown(ApiBadRequestException)
        exception.message.contains('cannot import multiple Terminologies')
    }
}
