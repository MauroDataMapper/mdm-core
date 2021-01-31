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
package uk.ac.ox.softeng.maurodatamapper.security.authentication

import uk.ac.ox.softeng.maurodatamapper.core.MdmCoreGrailsPlugin
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedUserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.VirtualGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import org.spockframework.util.Assert
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * <pre>
 * Controller: apiKey
 * |   PUT    | /api/catalogueUsers/${catalogueUserId}/apiKeys/${apiKeyId}/enable                    | Action: enableApiKey
 * |   PUT    | /api/catalogueUsers/${catalogueUserId}/apiKeys/${apiKeyId}/disable                   | Action: disableApiKey
 * |   PUT    | /api/catalogueUsers/${catalogueUserId}/apiKeys/${apiKeyId}/refresh/${expiresInDays}  | Action: refreshApiKey
 * |   POST   | /api/catalogueUsers/${catalogueUserId}/apiKeys                                       | Action: save
 * |   GET    | /api/catalogueUsers/${catalogueUserId}/apiKeys                                       | Action: index
 * |  DELETE  | /api/catalogueUsers/${catalogueUserId}/apiKeys/${id}                                 | Action: delete
 *  </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKeyController
 */
@Integration
@Slf4j
class ApiKeyFunctionalSpec extends ResourceFunctionalSpec<ApiKey> implements SecurityUsers {

    @Autowired
    ApplicationContext applicationContext
    @Autowired
    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService
    @Autowired
    GroupRoleService groupRoleService

    @OnceBefore
    @Transactional
    def checkAndSetupData() {
        log.debug('Check and setup test data')
        sessionFactory.currentSession.flush()
        assert CatalogueUser.count() == 2 // Unlogged user & admin user
        implementSecurityUsers('functionalTest')
        assert CatalogueUser.count() == 9
    }

    @Transactional
    def cleanupSpec() {
        CatalogueUser.list().findAll {
            !(it.emailAddress in [UnloggedUser.UNLOGGED_EMAIL_ADDRESS, StandardEmailAddress.ADMIN])
        }.each { it.delete(flush: true) }
        if (CatalogueUser.count() != 2) {
            Assert.fail("Resource Class ${CatalogueUser.simpleName} has not been emptied")
        }
    }

    @Transactional
    String getGroupId(String name) {
        UserGroup.findByName(name).id.toString()
    }

    @Transactional
    String getUserId(String emailAddress) {
        CatalogueUser.findByEmailAddress(emailAddress).id.toString()
    }

    @Transactional
    CatalogueUser getUser(String emailAddress) {
        CatalogueUser.findByEmailAddress(emailAddress)
    }

    @Override
    String getResourcePath() {
        "catalogueUsers/${getUserId(StandardEmailAddress.ADMIN)}/apiKeys"
    }

    @Override
    Map getValidJson() {
        [name       : 'functionalTest',
         expiryDate : LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
         refreshable: true]
    }

    @Override
    Map getInvalidJson() {
        [name: 'functionalTest',]
    }

    @Override
    String getExpectedShowJson() {
        """{"name"       : "functionalTest",
         "expiryDate" : "${LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)}",
         "refreshable": true}"""
    }

    @Override
    void verifyR4InvalidUpdateResponse() {
        verifyResponse(HttpStatus.NOT_FOUND, response)
    }

    @Override
    void verifyR4UpdateResponse() {
        verifyResponse(HttpStatus.NOT_FOUND, response)
    }

    @Override
    void verifyR5ShowResponse() {
        verifyResponse(HttpStatus.NOT_FOUND, jsonCapableResponse)
    }

    @Override
    def setup() {
        reconfigureDefaultUserPrivileges(true)
    }

    @Override
    void cleanUpData(String id = null) {
        super.cleanUpData(id)
        reconfigureDefaultUserPrivileges(false)
    }

    @Transactional
    void reconfigureDefaultUserPrivileges(boolean accessGranted) {

        GroupBasedUserSecurityPolicyManager defaultUserSecurityPolicyManager = applicationContext.getBean(
            MdmCoreGrailsPlugin.DEFAULT_USER_SECURITY_POLICY_MANAGER_BEAN_NAME)

        if (accessGranted) {
            VirtualGroupRole applicationLevelRole = groupRoleService.getFromCache(GroupRole.USER_ADMIN_ROLE_NAME)
            defaultUserSecurityPolicyManager.withApplicationRoles(applicationLevelRole.allowedRoles).withVirtualRoles(
                groupBasedSecurityPolicyManagerService.buildCatalogueUserVirtualRoles([applicationLevelRole.groupRole] as HashSet)
            )
        } else {
            defaultUserSecurityPolicyManager.withApplicationRoles([] as HashSet)
            groupBasedSecurityPolicyManagerService.buildUserSecurityPolicyManager(defaultUserSecurityPolicyManager)
        }
        groupBasedSecurityPolicyManagerService.storeUserSecurityPolicyManager(defaultUserSecurityPolicyManager)
    }

    void 'Test disabling an ApiKey'() {

        given:
        String id = createNewItem(validJson)

        when:
        PUT("${id}/disable", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().disabled
    }

    void 'Test enabling a disabled ApiKey'() {

        given:
        String id = createNewItem(validJson)
        PUT("${id}/disable", [:])
        verifyResponse(HttpStatus.OK, response)

        when:
        PUT("${id}/enable", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        !responseBody().disabled
    }

    void 'Test refreshing an expired refreshable ApiKey'() {

        given:
        int refreshDays = 4
        String id = createNewItem([name       : 'functionalTest',
                                   expiryDate : LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
                                   refreshable: true])

        when:
        PUT("${id}/refresh/${refreshDays}", [:])

        then:
        verifyResponse(HttpStatus.OK, response)
        responseBody().expiryDate == LocalDate.now().plusDays(refreshDays).format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    void 'Test refreshing an expired unrefreshable ApiKey'() {

        given:
        int refreshDays = 4
        String id = createNewItem([name       : 'functionalTest',
                                   expiryDate : LocalDate.now().minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE),
                                   refreshable: false])

        when:
        PUT("${id}/refresh/${refreshDays}", [:])

        then:
        verifyResponse(HttpStatus.UNPROCESSABLE_ENTITY, response)
        responseBody().total == 1
        responseBody().errors[0].message == 'Cannot refresh ApiKey as it is not marked refreshable'
    }
}
