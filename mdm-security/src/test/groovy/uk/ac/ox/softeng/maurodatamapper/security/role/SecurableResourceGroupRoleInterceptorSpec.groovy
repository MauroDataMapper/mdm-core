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
package uk.ac.ox.softeng.maurodatamapper.security.role

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.security.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.test.unit.interceptor.VariableContainedResourceInterceptorSpec

import grails.testing.web.interceptor.InterceptorUnitTest
import groovy.util.logging.Slf4j
import org.grails.web.util.GrailsApplicationAttributes

@Slf4j
class SecurableResourceGroupRoleInterceptorSpec extends VariableContainedResourceInterceptorSpec<SecurableResourceGroupRoleInterceptor>
    implements InterceptorUnitTest<SecurableResourceGroupRoleInterceptor> {

    def setup() {
        mockDomains(CatalogueUser, UserGroup, Folder, BasicModel, BasicModelItem)
    }

    @Override
    String getControllerName() {
        'securableResourceGroupRole'
    }

    @Override
    void setContainingResourceParameters(String id) {
        params.securableResourceDomainType = 'folders'
        params.securableResourceId = id
    }

    @Override
    void setAnyInitialParams() {
        params.securableResourceDomainType = 'folders'
        params.securableResourceId = UUID.randomUUID().toString()
    }

    @Override
    void setUnknownContainingItemDomainType() {
        params.securableResourceDomainType = 'dataModels'
    }

    void 'test execption thrown with non-securableResource item'() {
        given:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        setResourceIdParameter(UUID.randomUUID().toString(), '')
        params.securableResourceDomainType = 'basicModelItems'

        when:
        withRequest([controller: controllerName])
        request.setAttribute(GrailsApplicationAttributes.ACTION_NAME_ATTRIBUTE, 'index')
        interceptor.before()

        then:
        ApiBadRequestException e = thrown(ApiBadRequestException)

        and:
        log.error('e', e)
        e.errorCode == 'SRGRI01'
    }
}
