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
package uk.ac.ox.softeng.maurodatamapper.terminology


import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.terminology.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipType
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermRelationshipTypeService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.TermService
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationship
import uk.ac.ox.softeng.maurodatamapper.terminology.item.term.TermRelationshipService
import uk.ac.ox.softeng.maurodatamapper.test.unit.service.CatalogueItemServiceSpec
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.testing.services.ServiceUnitTest
import groovy.util.logging.Slf4j
import spock.lang.PendingFeature

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.UNIT_TEST

@Slf4j
class TerminologyServiceSpec extends CatalogueItemServiceSpec implements ServiceUnitTest<TerminologyService> {

    UUID id
    Terminology complexTerminology
    Terminology simpleTerminology

    def setup() {
        log.debug('Setting up TerminologyServiceSpec unit')
        mockArtefact(TermService)
        mockArtefact(TermRelationshipService)
        mockArtefact(TermRelationshipTypeService)
        mockDomains(Terminology, Term, TermRelationship, TermRelationshipType)

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

    void "test get"() {
        expect:
        service.get(id) != null
    }

    void "test list"() {
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

    void "test count"() {

        expect:
        service.count() == 5
    }

    void "test delete"() {

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

    void "test save"() {

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

    void 'test finalising model'() {

        when:
        Terminology terminology = service.get(id)

        then:
        !terminology.finalised
        !terminology.dateFinalised
        terminology.documentationVersion == Version.from('1')

        when:
        service.finaliseModel(terminology, admin)

        then:
        checkAndSave(terminology)

        when:
        terminology = service.get(id)

        then:
        terminology.finalised
        terminology.dateFinalised
        terminology.documentationVersion == Version.from('1')
    }

    void 'DMSC01 : test creating a new documentation version on draft model'() {

        when: 'creating new doc version on draft model is not allowed'
        Terminology terminology = service.get(id)
        def result = service.createNewDocumentationVersion(terminology, editor, false, userSecurityPolicyManager, [throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.terminology.new.version.not.finalised.message' }
    }

    void 'DMSC02 : test creating a new documentation version on finalised model'() {
        when: 'finalising model and then creating a new doc version is allowed'
        Terminology terminology = service.get(id)
        service.finaliseModel(terminology, admin)
        checkAndSave(terminology)
        def result = service.createNewDocumentationVersion(terminology, editor, false, userSecurityPolicyManager, [throwErrors: true])

        then:
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        terminology = service.get(id)
        Terminology newDocVersion = service.get(result.id)

        then: 'old model is finalised and superseded'
        terminology.finalised
        terminology.dateFinalised
        terminology.documentationVersion == Version.from('1')

        and: 'new doc version model is draft v2'
        newDocVersion.documentationVersion == Version.from('2')
        !newDocVersion.finalised
        !newDocVersion.dateFinalised

        and: 'new doc version model matches old model'
        newDocVersion.label == terminology.label
        newDocVersion.description == terminology.description
        newDocVersion.author == terminology.author
        newDocVersion.organisation == terminology.organisation
        newDocVersion.modelType == terminology.modelType

        newDocVersion.terms.size() == terminology.terms.size()
        newDocVersion.termRelationshipTypes.size() == terminology.termRelationshipTypes.size()

        and: 'annotations and edits are not copied'
        !newDocVersion.annotations
        newDocVersion.edits.size() == 1

        and: 'new version of link between old and new version'
        newDocVersion.versionLinks.any { it.targetModel.id == terminology.id && it.linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF }

        and:
        terminology.terms.every { odt ->
            newDocVersion.terms.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        terminology.termRelationshipTypes.every { odc ->
            newDocVersion.termRelationshipTypes.any {
                it.label == odc.label &&
                idcs == odcs &&
                ides == odes
            }
        }
    }

    @PendingFeature(reason = 'Terminology permission copying')
    void 'DMSC03 : test creating a new documentation version on finalised model with permission copying'() {
        when: 'finalising model and then creating a new doc version is allowed'
        Terminology terminology = service.get(id)
        service.finaliseModel(terminology, admin)
        checkAndSave(terminology)
        def result = service.createNewDocumentationVersion(terminology, editor, true, userSecurityPolicyManager, [throwErrors: true])

        then:
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        terminology = service.get(id)
        Terminology newDocVersion = service.get(result.id)

        then: 'old model is finalised and superseded'
        terminology.finalised
        terminology.dateFinalised
        terminology.documentationVersion == Version.from('1')

        and: 'new doc version model is draft v2'
        newDocVersion.documentationVersion == Version.from('2')
        !newDocVersion.finalised
        !newDocVersion.dateFinalised

        and: 'new doc version model matches old model'
        newDocVersion.label == terminology.label
        newDocVersion.description == terminology.description
        newDocVersion.author == terminology.author
        newDocVersion.organisation == terminology.organisation
        newDocVersion.modelType == terminology.modelType

        newDocVersion.terms.size() == terminology.terms.size()
        newDocVersion.termRelationshipTypes.size() == terminology.termRelationshipTypes.size()

        and: 'annotations and edits are not copied'
        !newDocVersion.annotations
        newDocVersion.edits.size() == 1

        and: 'new version of link between old and new version'
        newDocVersion.versionLinks.any { it.targetModel.id == terminology.id && it.linkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF }

        and:
        terminology.terms.every { odt ->
            newDocVersion.terms.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        terminology.termRelationshipTypes.every { odc ->
            newDocVersion.termRelationshipTypes.any {
                it.label == odc.label &&
                idcs == odcs &&
                ides == odes
            }
        }
    }


    void 'DMSC04 : test creating a new documentation version on finalised superseded model'() {

        when: 'creating new doc version'
        Terminology terminology = service.get(id)
        service.finaliseModel(terminology, editor)
        def newDocVersion = service.createNewDocumentationVersion(terminology, editor, false, userSecurityPolicyManager, [throwErrors: true])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old terminology'
        def result = service.createNewDocumentationVersion(terminology, editor, false, userSecurityPolicyManager, [throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.terminology.new.version.superseded.message' }
    }

    @PendingFeature(reason = 'Terminology permission copying')
    void 'DMSC05 : test creating a new documentation version on finalised superseded model with permission copying'() {

        when: 'creating new doc version'
        Terminology terminology = service.get(id)
        service.finaliseModel(terminology, editor)
        def newDocVersion = service.createNewDocumentationVersion(terminology, editor, true, userSecurityPolicyManager, [throwErrors: true])

        then:
        checkAndSave(newDocVersion)

        when: 'trying to create a new doc version on the old terminology'
        def result = service.createNewDocumentationVersion(terminology, editor, true, userSecurityPolicyManager, [throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.terminology.new.version.superseded.message' }
    }

    void 'DMSC06 : test creating a new model version on draft model'() {


        when: 'creating new version on draft model is not allowed'
        Terminology terminology = service.get(id)
        def result =
            service.createNewForkModel("${terminology.label}-1", terminology, editor, true, userSecurityPolicyManager, [throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.terminology.new.version.not.finalised.message' }
    }

    void 'DMSC07 : test creating a new model version on finalised model'() {

        when: 'finalising model and then creating a new version is allowed'
        Terminology terminology = service.get(id)
        service.finaliseModel(terminology, admin)
        checkAndSave(terminology)
        def result =
            service.createNewForkModel("${terminology.label}-1", terminology, editor, false, userSecurityPolicyManager, [throwErrors: true])

        then:
        result.instanceOf(Terminology)
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        terminology = service.get(id)
        Terminology newVersion = service.get(result.id)

        then: 'old model is finalised and superseded'
        terminology.finalised
        terminology.dateFinalised
        terminology.documentationVersion == Version.from('1')

        and: 'new  version model is draft v2'
        newVersion.documentationVersion == Version.from('1')
        !newVersion.finalised
        !newVersion.dateFinalised

        and: 'new  version model matches old model'
        newVersion.label != terminology.label
        newVersion.description == terminology.description
        newVersion.author == terminology.author
        newVersion.organisation == terminology.organisation
        newVersion.modelType == terminology.modelType

        newVersion.terms.size() == terminology.terms.size()
        newVersion.termRelationshipTypes.size() == terminology.termRelationshipTypes.size()

        and: 'annotations and edits are not copied'
        !newVersion.annotations
        newVersion.edits.size() == 1


        and: 'link between old and new version'
        newVersion.versionLinks.any { it.targetModel.id == terminology.id && it.linkType == VersionLinkType.NEW_FORK_OF }

        and:
        terminology.terms.every { odt ->
            newVersion.terms.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        terminology.termRelationshipTypes.every { odc ->
            newVersion.termRelationshipTypes.any {
                it.label == odc.label &&
                idcs == odcs &&
                ides == odes
            }
        }
    }

    @PendingFeature(reason = 'Terminology permission copying')
    void 'DMSC08 : test creating a new model version on finalised model with permission copying'() {

        when: 'finalising model and then creating a new version is allowed'
        Terminology terminology = service.get(id)
        service.finaliseModel(terminology, admin)
        checkAndSave(terminology)
        def result =
            service.createNewForkModel("${terminology.label}-1", terminology, editor, true, userSecurityPolicyManager, [throwErrors: true])

        then:
        result.instanceOf(Terminology)
        checkAndSave(result)

        when: 'load from DB to make sure everything is saved'
        terminology = service.get(id)
        Terminology newVersion = service.get(result.id)

        then: 'old model is finalised and superseded'
        terminology.finalised
        terminology.dateFinalised
        terminology.documentationVersion == Version.from('1')

        and: 'new  version model is draft v2'
        newVersion.documentationVersion == Version.from('1')
        !newVersion.finalised
        !newVersion.dateFinalised

        and: 'new  version model matches old model'
        newVersion.label != terminology.label
        newVersion.description == terminology.description
        newVersion.author == terminology.author
        newVersion.organisation == terminology.organisation
        newVersion.modelType == terminology.modelType

        newVersion.terms.size() == terminology.terms.size()
        newVersion.termRelationshipTypes.size() == terminology.termRelationshipTypes.size()

        and: 'annotations and edits are not copied'
        !newVersion.annotations
        newVersion.edits.size() == 1

        and: 'link between old and new version'
        newVersion.versionLinks.any { it.targetModel.id == terminology.id && it.linkType == VersionLinkType.NEW_FORK_OF }

        and:
        terminology.terms.every { odt ->
            newDocVersion.terms.any {
                it.label == odt.label &&
                it.id != odt.id &&
                it.domainType == odt.domainType
            }
        }
        terminology.termRelationshipTypes.every { odc ->
            newDocVersion.termRelationshipTypes.any {
                it.label == odc.label &&
                idcs == odcs &&
                ides == odes
            }
        }
    }

    void 'DMSC09 : test creating a new model version on finalised superseded model'() {

        when: 'creating new version'
        Terminology terminology = service.get(id)
        service.finaliseModel(terminology, editor)
        def newVersion = service.createNewDocumentationVersion(terminology, editor, false, userSecurityPolicyManager, [throwErrors: true])

        then:
        checkAndSave(newVersion)

        when: 'trying to create a new version on the old terminology'
        def result =
            service.createNewForkModel("${terminology.label}-1", terminology, editor, false, userSecurityPolicyManager, [throwErrors: true])

        then:
        result.errors.allErrors.size() == 1
        result.errors.allErrors.find { it.code == 'invalid.terminology.new.version.superseded.message' }
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
        invalid.errors.errorCount == 1
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 1
        invalid.errors.getFieldError('label')

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
        invalid.errors.errorCount == 3
        invalid.errors.globalErrorCount == 0
        invalid.errors.fieldErrorCount == 3
        invalid.errors.getFieldError('terms[0].code')
        invalid.errors.getFieldError('terms[0].definition')
        invalid.errors.getFieldError('terms[0].label')

        cleanup:
        GormUtils.outputDomainErrors(messageSource, invalid)
    }
}