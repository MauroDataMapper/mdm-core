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
package uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.enumeration


import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceDataType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferenceEnumerationType
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.datatype.ReferencePrimitiveType
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.ContainedResourceInterceptorUnitSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class ReferenceEnumerationValueInterceptorSpec extends ContainedResourceInterceptorUnitSpec implements InterceptorUnitTest<ReferenceEnumerationValueInterceptor> {

    def setup() {
        log.debug('Setting up ReferenceEnumerationValueInterceptorSpec')
        mockDomains(ReferenceDataModel, ReferenceDataElement, ReferenceDataType, ReferencePrimitiveType,
                    ReferenceEnumerationType, ReferenceEnumerationValue)
    }

    @Override
    String getControllerName() {
        'referenceEnumerationValue'
    }

    @Override
    void setContainingResourceParameters(String id) {
        params.referenceDataModelId = id
    }

    @Override
    void setAnyInitialParams() {
        params.referenceDataModelId = UUID.randomUUID().toString()
    }

    @Override
    String getExpectedExceptionCodeForNoContainingItem() {
        'DMSI01'
    }
}
