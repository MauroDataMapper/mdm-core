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
package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError
import spock.lang.PendingFeature

import java.time.OffsetDateTime

import static uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress.getUNIT_TEST

class VersionedFolderSpec extends ContainerSpec<VersionedFolder> implements DomainUnitTest<VersionedFolder> {

    @Override
    VersionedFolder newContainerClass(Map<String, Object> args) {
        new VersionedFolder(args)
    }

    @Override
    Class<VersionedFolder> getContainerClass() {
        VersionedFolder
    }

    @Override
    void verifyC04Error(VersionedFolder other) {
        other.errors.fieldErrors.any { it.field == 'label' && it.code == 'version.aware.label.not.unique.same.branch.names' }
    }

    @Override
    def setup() {
        mockDomains(Authority)
        checkAndSave(new Authority(label: 'Test Authority', url: "https://localhost", createdBy: UNIT_TEST))
    }

    VersionedFolder createValidDomain(String label) {
        new VersionedFolder(authority: testAuthority, label: label, createdBy: UNIT_TEST)
    }

    @Override
    void setValidDomainOtherValues() {
        super.setValidDomainOtherValues()
        domain.authority = testAuthority
    }

    @Override
    Map<String, Object> getChildFolderArgs() {
        [createdBy: UNIT_TEST, label: 'child']
    }

    @Override
    Map<String, Object> getOtherFolderArgs() {
        [createdBy: UNIT_TEST, label: 'other', authority: testAuthority]
    }

    @Override
    void verifyDomainOtherConstraints(VersionedFolder subDomain) {
        super.verifyDomainOtherConstraints(subDomain)
        assert subDomain.authority == testAuthority
    }

    Authority getTestAuthority() {
        Authority.findByLabel('Test Authority')
    }


    void 'M01 : test constrained properties'() {
        given:
        setValidDomainValues()
        domain.deleted = null
        domain.finalised = null
        domain.documentationVersion = null

        when:
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.errorCount == 3
        domain.errors.getFieldError('deleted').code == 'nullable'
        domain.errors.getFieldError('finalised').code == 'nullable'
        domain.errors.getFieldError('documentationVersion').code == 'nullable'

        when:
        domain.deleted = false
        domain.finalised = false
        domain.documentationVersion = Version.from('1')
        check(domain)

        then:
        notThrown(InternalSpockError)
        !domain.hasErrors()
    }

    void 'M02 : test creating model with same label is not allowed'() {
        given:
        setValidDomainValues()

        when:
        checkAndSave(domain)

        then:
        noExceptionThrown()

        when:
        VersionedFolder second = createValidDomain(domain.label)
        check(second)

        then:
        thrown(InternalSpockError)
        second.errors.getFieldError('label').code == 'version.aware.label.not.unique.same.branch.names'
    }

    void 'M03 : test updating a label to a used label is not allowed'() {
        when:
        setValidDomainValues()
        checkAndSave(domain)

        then:
        domain.count() == 1

        when:
        VersionedFolder second = createValidDomain('a safe label')
        checkAndSave(second)

        then:
        noExceptionThrown()

        when:
        second.label = domain.label
        check(second)

        then:
        thrown(InternalSpockError)
        second.errors.getFieldError('label').code == 'version.aware.label.not.unique.same.branch.names'
    }

    void 'M04 : test updating a label to a new unused label is allowed'() {
        when:
        setValidDomainValues()
        checkAndSave(domain)

        then:
        domain.count() == 1

        when:
        domain.label = 'a new better label'
        check(domain)

        then:
        noExceptionThrown()
    }

    void 'M05 : test creating a model with the same label and different branch names is allowed'() {
        when:
        setValidDomainValues()
        checkAndSave(domain)

        then:
        domain.count() == 1

        when:
        VersionedFolder second = createValidDomain(domain.label).tap {
            branchName = 'test'
        }
        checkAndSave(second)

        then:
        noExceptionThrown()
    }

    void 'M06 : test creating 3 models with the same label 2 with the same branch name and 1 different is not allowed'() {
        when:
        setValidDomainValues()
        checkAndSave(domain)

        then:
        domain.count() == 1

        when:
        VersionedFolder second = createValidDomain(domain.label).tap {
            branchName = 'test'
        }
        checkAndSave(second)

        then:
        noExceptionThrown()

        when:
        VersionedFolder third = createValidDomain(domain.label).tap {
            branchName = 'test'
        }
        check(third)

        then:
        thrown(InternalSpockError)
        third.errors.getFieldError('label').code == 'version.aware.label.not.unique.same.branch.names'
    }

    void 'M07 : test updating a doc version is not allowed'() {
        when:
        setValidDomainValues()
        checkAndSave(domain)

        then:
        domain.count() == 1

        when:
        domain.documentationVersion = Version.nextMajorVersion(domain.documentationVersion)
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.getFieldError('documentationVersion').code == 'version.aware.documentation.version.change.not.allowed'
    }

    void 'M08 : test setting a model version and finalising is allowed'() {
        when: 'branch name == main'
        setValidDomainValues()
        checkAndSave(domain)

        then:
        domain.count() == 1

        when: 'branch name == main'
        domain.finalised = true
        domain.dateFinalised = OffsetDateTime.now()
        domain.modelVersion = Version.from('1.0.0')
        check(domain)

        then:
        noExceptionThrown()
    }

    void 'M09 : test setting a model version without finalising is not allowed'() {
        when: 'branch name == main'
        setValidDomainValues()
        checkAndSave(domain)

        then:
        domain.count() == 1

        when: 'branch name == main'
        domain.modelVersion = Version.from('1.0.0')
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.getFieldError('modelVersion').code == 'version.aware.model.version.can.only.set.on.finalised.model'
    }

    void 'M10 : test finalising a model without a model version is not allowed'() {
        when: 'branch name == main'
        setValidDomainValues()
        checkAndSave(domain)

        then:
        domain.count() == 1

        when: 'branch name == main'
        domain.finalised = true
        domain.dateFinalised = OffsetDateTime.now()
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.getFieldError('modelVersion').code == 'version.aware.model.version.must.be.set.on.finalised.model'
    }

    void 'M11 : test setting a model version and then updating it is not allowed'() {
        when: 'branch name == main'
        setValidDomainValues()
        checkAndSave(domain)

        then:
        domain.count() == 1

        when:
        domain.finalised = true
        domain.dateFinalised = OffsetDateTime.now()
        domain.modelVersion = Version.from('1.0.0')
        checkAndSave(domain)

        then:
        noExceptionThrown()

        when: 'branch name == main'
        domain.modelVersion = Version.nextMajorVersion(domain.modelVersion)
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.getFieldError('modelVersion').code == 'version.aware.model.version.change.not.allowed'
    }

    void 'M12 : test setting a model version on branch is not allowed'() {
        when: 'branch name == main'
        setValidDomainValues()
        domain.branchName = 'test'
        checkAndSave(domain)

        then:
        domain.count() == 1

        when: 'branch name == main'
        domain.modelVersion = Version.from('1.0.0')
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.errors.getFieldError('modelVersion').code == 'version.aware.model.version.cannot.be.set.on.branch'
    }

    @PendingFeature
    void 'M13 : test creating a new model with the same name as existing and no model version is allowed'() {
        when: 'branch name == main'
        setValidDomainValues()
        checkAndSave(domain)
        VersionedFolder versionedFolder = domain as VersionedFolder
        versionedFolder.finalised = true
        versionedFolder.dateFinalised = OffsetDateTime.now()
        versionedFolder.modelVersion = Version.from('1.0.0')
        checkAndSave(versionedFolder)

        then:
        VersionedFolder.count() == 1
        versionedFolder.modelVersion

        when: 'branch name == main'
        VersionedFolder second = createValidDomain(versionedFolder.label)
        check(second)

        then:
        noExceptionThrown()
    }

    @PendingFeature
    void 'M14 : test setting model version on model with same name and branch is allowed'() {
        when: 'branch name == main'
        setValidDomainValues()
        domain.finalised = true
        domain.dateFinalised = OffsetDateTime.now()
        domain.modelVersion = Version.from('1.0.0')
        checkAndSave(domain)

        then:
        domain.count() == 1

        when: 'branch name == main'
        VersionedFolder second = createValidDomain(domain.label)
        checkAndSave(second)

        then:
        noExceptionThrown()

        when:
        second.finalised = true
        second.dateFinalised = OffsetDateTime.now()
        second.modelVersion = Version.nextMajorVersion(domain.modelVersion)
        check(second)

        then:
        noExceptionThrown()
    }


    void 'M15 : test creating a model with the same label, same branch versions, same model version and different doc versions is allowed'() {
        when: 'branch name == main'
        setValidDomainValues()
        domain.finalised = true
        domain.dateFinalised = OffsetDateTime.now()
        domain.modelVersion = Version.from('1.0.0')
        checkAndSave(domain)

        then:
        domain.count() == 1

        when: 'branch name == main'
        VersionedFolder second = createValidDomain(domain.label).tap {
            finalised = true
            dateFinalised = OffsetDateTime.now()
            modelVersion = Version.from('1.0.0')
            documentationVersion = Version.nextMajorVersion(domain.documentationVersion)
        }
        check(second)

        then:
        noExceptionThrown()
    }

    @PendingFeature
    void 'M16 : test creating a model with the same label, same branch versions, same model version and same doc versions is not allowed'() {
        when: 'branch name == main'
        setValidDomainValues()
        domain.finalised = true
        domain.dateFinalised = OffsetDateTime.now()
        domain.modelVersion = Version.from('1.0.0')
        checkAndSave(domain)

        then:
        domain.count() == 1

        when: 'branch name == main, equivalent to importing a finalised model'
        VersionedFolder second = createValidDomain(domain.label).tap {
            finalised = true
            dateFinalised = domain.dateFinalised
            modelVersion = domain.modelVersion
            documentationVersion = domain.documentationVersion
        }
        check(second)

        then:
        thrown(InternalSpockError)
        second.errors.getFieldError('label').code == 'version.aware.label.not.unique.same.versions'
    }

    @PendingFeature
    void 'M17 : test creating models as various branches'() {
        when: 'branch name == main, domain is finalised model version'
        setValidDomainValues()
        domain.finalised = true
        domain.dateFinalised = OffsetDateTime.now()
        domain.modelVersion = Version.from('1.0.0')
        checkAndSave(domain)

        then:
        domain.count() == 1

        when: 'branch name == main'
        VersionedFolder second = createValidDomain(domain.label)
        checkAndSave(second)

        then:
        noExceptionThrown()

        when: 'branch name == test'
        VersionedFolder third = createValidDomain(domain.label).tap {
            branchName = 'test'
        }
        checkAndSave(third)

        then:
        noExceptionThrown()

        when: 'branch name == another'
        VersionedFolder fourth = createValidDomain(domain.label).tap {
            branchName = 'another'
        }
        checkAndSave(fourth)

        then:
        noExceptionThrown()

        when: 'branch name == main, second is finalised'
        second.finalised = true
        second.dateFinalised = OffsetDateTime.now()
        second.modelVersion = Version.from('2.0.0')
        checkAndSave(second)

        then:
        noExceptionThrown()

        when: 'branch name == main'
        VersionedFolder fifth = createValidDomain(domain.label)
        checkAndSave(fifth)

        then:
        noExceptionThrown()

        when: 'branch name == test'
        VersionedFolder sixth = createValidDomain(domain.label).tap {
            branchName = 'test'
        }
        checkAndSave(sixth)

        then:
        thrown(InternalSpockError)
        sixth.errors.getFieldError('label').code == 'version.aware.label.not.unique.same.branch.names'
    }
}
