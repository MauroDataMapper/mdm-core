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
package uk.ac.ox.softeng.maurodatamapper.security


import uk.ac.ox.softeng.maurodatamapper.core.MdmCoreGrailsPlugin
import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedUserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition

import grails.core.GrailsApplication
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

@Slf4j
class BootStrap implements SecurityDefinition {

    @Autowired
    MessageSource messageSource

    GroupRoleService groupRoleService
    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService

    GrailsApplication grailsApplication

    def init = {servletContext ->

        GroupRole.withNewTransaction {
            // Add all the roles
            checkAndSave(messageSource, GroupRole.getDefaultGroupRoleModelStructure())
            groupRoleService.refreshCacheGroupRoles()

            GroupBasedUserSecurityPolicyManager defaultUserSecurityPolicyManager = grailsApplication.mainContext.getBean(
                MdmCoreGrailsPlugin.DEFAULT_USER_SECURITY_POLICY_MANAGER_BEAN_NAME)

            CatalogueUser unloggedInUser =
                CatalogueUser.findByEmailAddress(UnloggedUser.UNLOGGED_EMAIL_ADDRESS) ?: CatalogueUser.fromInterface(UnloggedUser.instance)
            unloggedInUser.tempPassword = null
            unloggedInUser.save(flush: true)

            defaultUserSecurityPolicyManager.forUser(unloggedInUser).inApplication(grailsApplication)
            groupBasedSecurityPolicyManagerService.buildUserSecurityPolicyManager(defaultUserSecurityPolicyManager)
            groupBasedSecurityPolicyManagerService.storeUserSecurityPolicyManager(defaultUserSecurityPolicyManager)

        }

        CatalogueUser.withNewTransaction {
            admin = CatalogueUser.findByEmailAddress(StandardEmailAddress.ADMIN)
            if (!admin) {
                createAdminUser('admin')
                checkAndSave(messageSource, admin)
            }
            admins = UserGroup.findByName('administrators')
            if (!admins) {
                createAdminGroup('admin')
                checkAndSave(messageSource, admins)
            }
        }

        environments {
            development {

                CatalogueUser.withNewTransaction {

                    getOrCreateModernSecurityUsers('development', false)
                    checkAndSave(messageSource, editor, reader, authenticated, pending, containerAdmin, author, reviewer)

                    getOrCreateBasicGroups('development', false)
                    checkAndSave(messageSource, editors, readers)

                    Folder folder = Folder.findByLabel('Development Folder')

                    if (SecurableResourceGroupRole.bySecurableResourceAndGroupRoleIdAndUserGroupId(
                        folder, groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole.id, editors.id).count() == 0) {
                        // Make editors container admin (existing permissions) of the test folder
                        checkAndSave(messageSource, new SecurableResourceGroupRole(
                            createdBy: userEmailAddresses.development,
                            securableResource: folder,
                            userGroup: editors,
                            groupRole: groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole)
                        )
                    }
                    if (SecurableResourceGroupRole.bySecurableResourceAndGroupRoleIdAndUserGroupId(
                        folder, groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME).groupRole.id, readers.id).count() == 0) {
                        // Make readers reader of the test folder
                        checkAndSave(messageSource, new SecurableResourceGroupRole(
                            createdBy: userEmailAddresses.development,
                            securableResource: folder,
                            userGroup: readers,
                            groupRole: groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME).groupRole)
                        )
                    }
                    Classifier classifier = Classifier.findByLabel('Development Classifier')

                    if (SecurableResourceGroupRole.bySecurableResourceAndGroupRoleIdAndUserGroupId(
                        classifier, groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole.id, editors.id).count() == 0) {
                        // Make editors container admin (existing permissions) of the test classifier
                        checkAndSave(messageSource, new SecurableResourceGroupRole(
                            createdBy: userEmailAddresses.development,
                            securableResource: classifier,
                            userGroup: editors,
                            groupRole: groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole)
                        )
                    }
                    if (SecurableResourceGroupRole.bySecurableResourceAndGroupRoleIdAndUserGroupId(
                        classifier, groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME).groupRole.id, readers.id).count() == 0) {
                        // Make readers reader of the test classifier
                        checkAndSave(messageSource, new SecurableResourceGroupRole(
                            createdBy: userEmailAddresses.development,
                            securableResource: classifier,
                            userGroup: readers,
                            groupRole: groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME).groupRole)
                        )
                    }
                }
            }
        }
    }
    def destroy = {
    }
}
