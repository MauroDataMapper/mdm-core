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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype

import uk.ac.ox.softeng.maurodatamapper.datamodel.test.DataTypeSpec

import grails.testing.gorm.DomainUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class ModelDataTypeSpec extends DataTypeSpec<ModelDataType> implements DomainUnitTest<ModelDataType> {

    UUID testId

    def setup() {
        testId = UUID.randomUUID()
    }

    @Override
    void setValidDomainOtherValues() {
        domain.modelResourceId = testId
        domain.modelResourceDomainType = 'Terminology'
    }

    @Override
    void verifyDomainOtherConstraints(ModelDataType domain) {
        assert domain.modelResourceId == testId
        assert domain.modelResourceDomainType == 'Terminology'
    }

    @Override
    ModelDataType createValidDomain(String label) {
        ModelDataType modelDataType = super.createValidDomain(label)
        modelDataType.modelResourceId = testId
        modelDataType.modelResourceDomainType = 'Terminology'
        modelDataType
    }
}
