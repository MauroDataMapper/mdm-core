/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
import uk.ac.ox.softeng.maurodatamapper.security.policy.UserSecurityPolicyService
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition

import grails.core.GrailsApplication
import grails.util.Environment
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
    SecurableResourceGroupRoleService securableResourceGroupRoleService
    UserSecurityPolicyService userSecurityPolicyService

    GrailsApplication grailsApplication

    def init = {servletContext ->

        GroupRole.withNewTransaction {
            // Add all the roles
            checkAndSave(messageSource, GroupRole.getDefaultGroupRoleModelStructure())
            groupRoleService.refreshCacheGroupRoles()

            GroupBasedUserSecurityPolicyManager defaultUserSecurityPolicyManager = grailsApplication.mainContext.getBean(
                MdmCoreGrailsPlugin.DEFAULT_USER_SECURITY_POLICY_MANAGER_BEAN_NAME, GroupBasedUserSecurityPolicyManager)

            CatalogueUser unloggedInUser =
                CatalogueUser.findByEmailAddress(UnloggedUser.UNLOGGED_EMAIL_ADDRESS) ?: CatalogueUser.fromInterface(UnloggedUser.instance)
            unloggedInUser.tempPassword = null
            unloggedInUser.save(flush: true)

            defaultUserSecurityPolicyManager
                .inApplication(grailsApplication)
                .withUserPolicy(userSecurityPolicyService.buildUserSecurityPolicy(unloggedInUser, unloggedInUser.groups))
            groupBasedSecurityPolicyManagerService.storeUserSecurityPolicyManager(defaultUserSecurityPolicyManager)
        }
        // Only allow bootstrapping to be disabled if environment is prod
        if (Environment.current != Environment.PRODUCTION || grailsApplication.config.getProperty('maurodatamapper.bootstrap.adminuser', Boolean, true)) {
            log.info('Bootstrapping admin user and administrators group')
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
        }

        if (Environment.current == Environment.PRODUCTION && !grailsApplication.config.getProperty('maurodatamapper.bootstrap.adminuser', Boolean, true)) {
            CatalogueUser.withNewTransaction {
                if (UserGroup.countByApplicationGroupRole(GroupRole.findByName(GroupRole.SITE_ADMIN_ROLE_NAME)) == 0) {
                    log.warn('Bootstrapping of admin user has been disabled and there are no site admin level groups, the create user endpoint will be opened to allow an ' +
                             'admin user to be created')
                }
            }
        }

        log.debug('Main bootstrap complete')

        environments {
            development {
                //dev env relies almost entirely upon adminusers being bootstrapped
                CatalogueUser.withNewTransaction {

                    createModernSecurityUsers('development', false)
                    checkAndSave(messageSource, editor,
                                 pending,
                                 containerAdmin,
                                 author,
                                 reviewer,
                                 reader,
                                 authenticated,
                                 creator)

                    createBasicGroups('development', false)
                    checkAndSave(messageSource,
                                 editors,
                                 readers,
                                 reviewers,
                                 authors,
                                 containerAdmins,
                                 admins)

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
                log.debug('Development environment bootstrap complete')
            }
            production {
                if (grailsApplication.config.getProperty('maurodatamapper.bootstrap.folder', Boolean, true)) {
                    bootstrapExampleFolder()
                }
                log.debug('Production environment bootstrap complete')
            }
        }
    }
    def destroy = {
    }

    void bootstrapExampleFolder() {
        CatalogueUser.withNewTransaction {
            if (!Folder.count()) {
                Folder folder = new Folder(
                    label: 'Example Folder',
                    createdBy: StandardEmailAddress.ADMIN,
                    readableByAuthenticatedUsers: true,
                    description: 'This folder is readable by all authenticated users, and currently only editable by users in the ' +
                                 'administrators group. Future suggestions: rename this folder to be more descriptive, and alter group ' +
                                 'access.')
                checkAndSave(messageSource, folder)

                // Make sure the folder is secured
                if (SecurableResourceGroupRole.bySecurableResourceAndGroupRoleIdAndUserGroupId(
                    folder, groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole.id, admins.id).count() == 0) {
                    checkAndSave(messageSource, new SecurableResourceGroupRole(
                        createdBy: StandardEmailAddress.ADMIN,
                        securableResource: folder,
                        userGroup: admins,
                        groupRole: groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole))
                }
            }
        }
    }
}
