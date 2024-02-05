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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkInterceptor
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.VariableContainedResourceInterceptorSpec

import grails.testing.web.interceptor.InterceptorUnitTest

class SemanticLinkInterceptorSpec extends VariableContainedResourceInterceptorSpec implements InterceptorUnitTest<SemanticLinkInterceptor> {

    def setup() {
        mockDomains(BasicModel)
    }

    @Override
    String getControllerName() {
        'semanticLink'
    }

    @Override
    void setContainingResourceParameters(String id) {
        params.catalogueItemDomainType = 'basicModels'
        params.catalogueItemId = id
    }

    @Override
    void setAnyInitialParams() {
        params.catalogueItemDomainType = 'basicModels'
        params.catalogueItemId = UUID.randomUUID().toString()
    }

    @Override
    void setUnknownContainingItemDomainType() {
        params.catalogueItemDomainType = 'dataModels'
    }
}
