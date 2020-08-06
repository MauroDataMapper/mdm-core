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
package uk.ac.ox.softeng.maurodatamapper.test.unit.core

import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.diff.FieldDiff
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.util.Version

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
        domain.authority = new Authority(label: 'Test Authority', url: "https://localhost")
    }

    @Override
    void verifyDomainConstraints(K domain) {
        super.verifyDomainConstraints(domain)
        domain.folder == Folder.findByLabel('catalogue')
        domain.documentationVersion == Version.from('1')
        domain.authority = new Authority(label: 'Test Authority', url: "https://localhost")
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
        domain.modelType = ''
        domain.deleted = false
        domain.finalised = false
        domain.documentationVersion = Version.from('1')
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.errorCount == 1
        domain.errors.getFieldError('modelType').code == 'blank'
    }

    void 'M02 : test label uniqueness'() {
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
        second.errors.getFieldError('label').code == 'default.not.unique.message'

        when:
        second.documentationVersion = Version.from('2')
        check(second)

        then:
        noExceptionThrown()
    }

    void 'M03 : test diffing versions and finalising'() {
        given:
        setValidDomainValues()
        K other = createValidDomain(domain.label)
        domain.finalised = true
        domain.dateFinalised = OffsetDateTime.now()
        other.documentationVersion = Version.from('2')

        when:
        ObjectDiff diff = domain.diff(other)

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
