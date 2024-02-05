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
package uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.enumeration

import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataElement
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.DataType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationTypeService
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.PrimitiveType
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.ReferenceType
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.ContainedResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class EnumerationValueInterceptorSpec extends ContainedResourceInterceptorUnitSpec implements InterceptorUnitTest<EnumerationValueInterceptor> {

    def setup() {
        log.debug('Setting up EnumerationValueInterceptorSpec')
        mockDomains(DataModel, DataClass, DataElement, DataType, PrimitiveType,
                    ReferenceType, EnumerationType, EnumerationValue)
        interceptor.enumerationTypeService = Stub(EnumerationTypeService) {
            existsByDataModelIdAndId(_, _) >> true
        }
    }

    @Override
    String getControllerName() {
        'enumerationValue'
    }

    @Override
    void setContainingResourceParameters(String id) {
        params.dataModelId = id
    }

    @Override
    void setAnyInitialParams() {
        params.dataModelId = UUID.randomUUID().toString()
    }

    @Override
    String getExpectedExceptionCodeForNoContainingItem() {
        'MII01'
    }
}
