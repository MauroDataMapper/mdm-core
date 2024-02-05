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
package uk.ac.ox.softeng.maurodatamapper.terminology

import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolderService
import uk.ac.ox.softeng.maurodatamapper.core.diff.MergeDiffService
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTreeService
import uk.ac.ox.softeng.maurodatamapper.core.path.PathService
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipService
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.UNIT_TEST

@Slf4j
class TerminologyServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<TerminologyService> {

    UUID id
    Terminology complexTerminology
    Terminology simpleTerminology

    def setup() {
        log.debug('Setting up TerminologyServiceSpec unit')
        mockArtefact(MergeDiffService)
        mockArtefact(TermService)
        mockArtefact(BreadcrumbTreeService)
        mockArtefact(TermRelationshipService)
        mockArtefact(TermRelationshipTypeService)
        mockArtefact(PathService)
        mockArtefact(VersionedFolderService)
        mockDomains(Terminology, Term, TermRelationship, TermRelationshipType)

        service.breadcrumbTreeService = Stub(BreadcrumbTreeService) {
            finalise(_) >> {
                BreadcrumbTree bt ->
                    bt.finalised = true
                    bt.buildTree()

            }
        }

        complexTerminology = BootstrapModels.buildAndSaveComplexTerminology(messageSource, testFolder, null, testAuthority)
        simpleTerminology = BootstrapModels.buildAndSaveSimpleTerminology(messageSource, testFolder, testAuthority)

        Terminology terminology1 = new Terminology(createdBy: UNIT_TEST, label: 'test terminology 1', folder: testFolder, authority: testAuthority)
        Terminology terminology2 = new Terminology(createdBy: UNIT_TEST, label: 'test terminology 2', folder: testFolder, authority: testAuthority)
        Terminology terminology3 = new Terminology(createdBy: UNIT_TEST, label: 'test terminology 3', folder: testFolder, authority: testAuthority)

        checkAndSave(terminology1)
        checkAndSave(terminology2)
        checkAndSave(terminology3)

        Term t = new Term(createdBy: UNIT_TEST, code: 'UTT1', definition: 'Unit Test Term 01')
        terminology1.addToTerms(t)
        checkAndSave(terminology1)

        verifyBreadcrumbTrees()
        id = terminology1.id
    }

    void 'test get'() {
        expect:
        service.get(id) != null
    }

    void 'test list'() {
        when:
        List<Terminology> terminologyList = service.list(max: 2, offset: 2, sort: 'dateCreated')

        then:
        terminologyList.size() == 2

        when:
        def dm1 = terminologyList[0]
        def dm2 = terminologyList[1]

        then:
        dm1.label == 'test terminology 1'
        dm1.modelType == Terminology.simpleName

        and:
        dm2.label == 'test terminology 2'
        dm2.modelType == Terminology.simpleName

    }

    void 'test count'() {

        expect:
        service.count() == 5
    }

    void 'test delete'() {

        expect:
        service.count() == 5
        Terminology dm = service.get(id)

        when:
        service.delete(dm)
        service.save(dm)

        then:
        Terminology.countByDeleted(false) == 4
        Terminology.countByDeleted(true) == 1
    }

    void 'test save'() {

        when:
        Terminology terminology = new Terminology(createdBy: UNIT_TEST, label: 'saving test', folder: testFolder, authority: testAuthority)
        service.save(terminology)

        then:
        terminology.id != null

        when:
        Terminology saved = service.get(terminology.id)

        then:
        saved.breadcrumbTree
        saved.breadcrumbTree.domainId == saved.id
    }

    void 'DMSV01 : test validation on valid model'() {
        given:
        Terminology check = complexTerminology

        expect:
        !service.validate(check).hasErrors()
    }

    void 'DMSV02 : test validation on invalid simple model'() {
        given:
        Terminology check = new Terminology(createdBy: UNIT_TEST, folder: testFolder, authority: testAuthority)

        when:
        Terminology invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 2
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 2
        invalid.errors.getFieldError('label')
        invalid.errors.getFieldError('path')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }

    void 'DMSV03 : test validation on invalid term model'() {
        given:
        Terminology check = new Terminology(createdBy: UNIT_TEST, label: 'test invalid', folder: testFolder, authority: testAuthority)
        check.addToTerms(new Term(createdBy: UNIT_TEST))

        when:
        Terminology invalid = service.validate(check)

        then:
        invalid.hasErrors()
        invalid.errors.errorCount == 4
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 4
        invalid.errors.getFieldError('terms[0].code')
        invalid.errors.getFieldError('terms[0].definition')
        invalid.errors.getFieldError('terms[0].label')
        invalid.errors.getFieldError('terms[0].path')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }
}