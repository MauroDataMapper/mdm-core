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

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.terminology.item.Term
import uk.ac.ox.softeng.maurodatamapper.test.unit.core.ModelSpec

import grails.testing.gorm.DomainUnitTest

class CodeSetSpec extends ModelSpec<CodeSet> implements DomainUnitTest<CodeSet> {

    def setup() {
        mockDomains(CodeSet, Terminology, Term)
    }

    @Override
    void setValidDomainOtherValues() {
    }

    @Override
    void verifyDomainOtherConstraints(CodeSet domain) {
    }

    @Override
    CodeSet createValidDomain(String label) {
        new CodeSet(label: label, createdBy: StandardEmailAddress.UNIT_TEST, folder: testFolder)
    }

    @Override
    CodeSet findById() {
        CodeSet.findById(domain.id)
    }

    @Override
    int getExpectedConstrainedBlankErrors() {
        3 // No breadcrumbtree.label
    }
}
