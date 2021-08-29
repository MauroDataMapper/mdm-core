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
package uk.ac.ox.softeng.maurodatamapper.test.unit.core

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.FieldDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.bidirectional.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.version.Version

import groovy.util.logging.Slf4j
import org.spockframework.util.InternalSpockError

import java.time.OffsetDateTime

/**
 * @since 21/09/2017
 */
@Slf4j
abstract class ModelSpec<K extends Model> extends CatalogueItemSpec<K> {

    @Override
    void setValidDomainValues() {
        super.setValidDomainValues()
        domain.folder = Folder.findByLabel('catalogue')
        domain.documentationVersion = Version.from('1')
        domain.authority = testAuthority
    }

    @Override
    void verifyDomainConstraints(K domain) {
        super.verifyDomainConstraints(domain)
        domain.folder == Folder.findByLabel('catalogue')
        domain.documentationVersion == Version.from('1')
        domain.authority = testAuthority
        assert domain.modelType
        domain.finalised != null
        domain.deleted != null

    }

    void 'M01 : test constrained properties'() {
        given:
        setValidDomainValues()
        domain.folder = null
        domain.deleted = null
        domain.finalised = null
        domain.modelType = null
        domain.documentationVersion = null

        when:
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.errorCount == 5
        domain.errors.getFieldError('folder').code == 'nullable'
        domain.errors.getFieldError('deleted').code == 'nullable'
        domain.errors.getFieldError('finalised').code == 'nullable'
        domain.errors.getFieldError('modelType').code == 'nullable'
        domain.errors.getFieldError('documentationVersion').code == 'nullable'

        when:
        domain.folder = Folder.findByLabel('catalogue')
        domain.modelType = null
        domain.deleted = false
        domain.finalised = false
        domain.documentationVersion = Version.from('1')
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.errorCount == 1
        domain.errors.getFieldError('modelType').code == 'nullable'
    }

    void 'M02 : test creating model with same label is not allowed'() {
        given:
        setValidDomainValues()

        when:
        checkAndSave(domain)

        then:
        noExceptionThrown()

        when:
        K second = createValidDomain(domain.label)
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
        K second = createValidDomain('a safe label')
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
        K second = createValidDomain(domain.label).tap {
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
        K second = createValidDomain(domain.label).tap {
            branchName = 'test'
        }
        checkAndSave(second)

        then:
        noExceptionThrown()

        when:
        K third = createValidDomain(domain.label).tap {
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

    void 'M13 : test creating a new model with the same name as existing and no model version is allowed'() {
        when: 'branch name == main'
        setValidDomainValues()
        domain.finalised = true
        domain.dateFinalised = OffsetDateTime.now()
        domain.modelVersion = Version.from('1.0.0')
        checkAndSave(domain)

        then:
        domain.count() == 1

        when: 'branch name == main'
        K second = createValidDomain(domain.label)
        check(second)

        then:
        noExceptionThrown()
    }

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
        K second = createValidDomain(domain.label)
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
        K second = createValidDomain(domain.label).tap {
            finalised = true
            dateFinalised = OffsetDateTime.now()
            modelVersion = Version.from('1.0.0')
            documentationVersion = Version.nextMajorVersion(domain.documentationVersion)
        }
        check(second)

        then:
        noExceptionThrown()
    }

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
        K second = createValidDomain(domain.label).tap {
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
        K second = createValidDomain(domain.label)
        checkAndSave(second)

        then:
        noExceptionThrown()

        when: 'branch name == test'
        K third = createValidDomain(domain.label).tap {
            branchName = 'test'
        }
        checkAndSave(third)

        then:
        noExceptionThrown()

        when: 'branch name == another'
        K fourth = createValidDomain(domain.label).tap {
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
        K fifth = createValidDomain(domain.label)
        checkAndSave(fifth)

        then:
        noExceptionThrown()

        when: 'branch name == test'
        K sixth = createValidDomain(domain.label).tap {
            branchName = 'test'
        }
        checkAndSave(sixth)

        then:
        thrown(InternalSpockError)
        sixth.errors.getFieldError('label').code == 'version.aware.label.not.unique.same.branch.names'
    }


    void 'M2X : test diffing versions and finalising'() {
        given:
        setValidDomainValues()
        K other = createValidDomain(domain.label)
        domain.finalised = true
        domain.dateFinalised = OffsetDateTime.now()
        other.documentationVersion = Version.from('2')

        when:
        ObjectDiff diff = domain.diff(other, null)

        then:
        noExceptionThrown()

        and:
        diff.getNumberOfDiffs() == 3

        when:
        FieldDiff<Boolean> finalisingDiff = diff.find {it.fieldName == 'finalised'}
        FieldDiff<String> docVerDiff = diff.find {it.fieldName == 'documentationVersion'}
        FieldDiff<OffsetDateTime> dateFinDiff = diff.find {it.fieldName == 'dateFinalised'}

        then:
        finalisingDiff.left == true
        finalisingDiff.right == false

        and:
        docVerDiff.left == Version.from('1').toString()
        docVerDiff.right == Version.from('2').toString()

        and:
        dateFinDiff.left
        !dateFinDiff.right
    }
}
