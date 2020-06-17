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
package uk.ac.ox.softeng.maurodatamapper.testing.functional

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.datamodel.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityDefinition

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource

import static uk.ac.ox.softeng.maurodatamapper.util.GormUtils.checkAndSave

@Slf4j
class BootStrap implements SecurityDefinition {

    @Autowired
    MessageSource messageSource

    GroupRoleService groupRoleService

    def init = {servletContext ->
        environments {
            test {
                Folder folder
                Folder folder2
                CatalogueUser.withNewTransaction {

                    createModernSecurityUsers('functionalTest', false)
                    checkAndSave(messageSource, editor, reader, authenticated, pending, containerAdmin, author, reviewer)

                    createBasicGroups('functionalTest', false)
                    checkAndSave(messageSource, editors, readers)

                    folder = new Folder(label: 'Functional Test Folder', createdBy: userEmailAddresses.functionalTest)
                    checkAndSave(messageSource, folder)

                    folder2 = new Folder(label: 'Functional Test Folder 2', createdBy: userEmailAddresses.functionalTest)
                    checkAndSave(messageSource, folder2)

                    // Make editors container admin (existing permissions) of the test folder
                    checkAndSave(messageSource, new SecurableResourceGroupRole(
                        createdBy: userEmailAddresses.functionalTest,
                        securableResource: folder,
                        userGroup: editors,
                        groupRole: groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole)
                    )
                    // Make readers reviewers of the test folder, this will allow "comment" adding testing
                    checkAndSave(messageSource, new SecurableResourceGroupRole(
                        createdBy: userEmailAddresses.functionalTest,
                        securableResource: folder,
                        userGroup: readers,
                        groupRole: groupRoleService.getFromCache(GroupRole.REVIEWER_ROLE_NAME).groupRole)
                    )

                    Classifier classifier = new Classifier(label: 'Functional Test Classifier',
                                                           createdBy: userEmailAddresses.functionalTest)
                    checkAndSave(messageSource, classifier)
                    // Make editors container admin (existing permissions) of the test folder
                    checkAndSave(messageSource, new SecurableResourceGroupRole(
                        createdBy: userEmailAddresses.functionalTest,
                        securableResource: classifier,
                        userGroup: editors,
                        groupRole: groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole)
                    )
                    // Make readers reader of the test folder
                    checkAndSave(messageSource, new SecurableResourceGroupRole(
                        createdBy: userEmailAddresses.functionalTest,
                        securableResource: classifier,
                        userGroup: readers,
                        groupRole: groupRoleService.getFromCache(GroupRole.REVIEWER_ROLE_NAME).groupRole)
                    )
                }

                Folder.withNewTransaction {
                    folder = Folder.findByLabel('Functional Test Folder')
                    BootstrapModels.buildAndSaveComplexDataModel(messageSource, folder)
                    BootstrapModels.buildAndSaveSimpleDataModel(messageSource, folder)
                }
            }
        }
    }

    def destroy = {
    }
}
