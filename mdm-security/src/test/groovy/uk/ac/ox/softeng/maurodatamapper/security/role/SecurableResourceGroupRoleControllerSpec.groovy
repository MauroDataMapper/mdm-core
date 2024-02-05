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
package uk.ac.ox.softeng.maurodatamapper.security.role


import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

@Slf4j
class SecurableResourceGroupRoleControllerSpec extends ResourceControllerSpec<SecurableResourceGroupRole> implements
    ControllerUnitTest<SecurableResourceGroupRoleController>,
    DomainUnitTest<SecurableResourceGroupRole>, SecurityUsers {

    Folder folder
    GroupRole editorRole
    UserGroup editors
    UserGroup readers
    UserGroup addtl

    def setup() {
        log.debug('Setting up SecurableResourceGroupRoleControllerSpec')
        mockDomains(CatalogueUser, Edit, UserGroup, GroupRole, Folder)
        implementSecurityUsers('unitTest')
        checkAndSave(GroupRole.getDefaultGroupRoleModelStructure())

        editorRole = GroupRole.findByName('editor')

        editors = new UserGroup(createdBy: userEmailAddresses.unitTest, name: 'editors').addToGroupMembers(editor)
        checkAndSave(editors)
        readers = new UserGroup(createdBy: userEmailAddresses.unitTest, name: 'readers').addToGroupMembers(reader).
            addToGroupMembers(reviewer)
        checkAndSave(readers)
        addtl = new UserGroup(createdBy: userEmailAddresses.unitTest, name: 'addtl').addToGroupMembers(reader)
        checkAndSave(addtl)
        UserGroup folderAdmins = new UserGroup(createdBy: userEmailAddresses.unitTest, name: 'folderAdmins').addToGroupMembers(admin)
        checkAndSave(folderAdmins)

        folder = new Folder(label: 'catalogue', createdBy: admin.emailAddress)
        checkAndSave(folder)

        domain.securableResource = folder
        domain.userGroup = editors
        domain.groupRole = GroupRole.findByName('editor')
        domain.createdBy = userEmailAddresses.unitTest
        checkAndSave(domain)

        SecurableResourceGroupRole readerRole = new SecurableResourceGroupRole(securableResource: folder,
                                                                               userGroup: readers,
                                                                               groupRole: GroupRole.findByName('reader'),
                                                                               createdBy: userEmailAddresses.unitTest)
        checkAndSave(readerRole)
        SecurableResourceGroupRole adminRole = new SecurableResourceGroupRole(securableResource: folder,
                                                                              userGroup: folderAdmins,
                                                                              groupRole: GroupRole.findByName(GroupRole.CONTAINER_ADMIN_ROLE_NAME),
                                                                              createdBy: userEmailAddresses.unitTest)
        checkAndSave(adminRole)


        controller.securableResourceGroupRoleService = Stub(SecurableResourceGroupRoleService) {
            findBySecurableResourceAndId(_, _, _) >> {dt, did, id ->
                if (dt == 'Folder') {
                    if (did == folder.id) {
                        return SecurableResourceGroupRole.findById(id)
                    }
                }
                null
            }
            findAllBySecurableResource(_, _, _) >> {dt, id, p ->
                if (dt == 'Folder') {
                    if (id == folder.id) {
                        return SecurableResourceGroupRole.findAllBySecurableResourceId(id)
                    }
                }
                []
            }
            findSecurableResource(_, _, _) >> {clazz, id, silenceException ->
                id == folder.id ? folder : null
            }
        }
        controller.groupBasedSecurityPolicyManagerService = Stub(GroupBasedSecurityPolicyManagerService) {
            refreshAllUserSecurityPolicyManagersBySecurableResourceGroupRole(_, _) >> PublicAccessSecurityPolicyManager.instance
        }
    }

    @Override
    void givenParameters() {
        super.givenParameters()
        params.securableResourceDomainType = Folder.simpleName
        params.securableResourceId = folder.id
    }

    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 3,
  "items": [
    {
      "securableResourceDomainType": "Folder",
      "securableResourceId": "${json-unit.matches:id}",
      "createdBy": "unit-test@test.com",
      "availableActions": ["delete","show","update"],
      "id": "${json-unit.matches:id}",
      "userGroup": {
        "name": "editors",
        "id": "${json-unit.matches:id}"
      },
      "groupRole": {
        "displayName": "Editor",
        "name": "editor",
        "id": "${json-unit.matches:id}"
      }
    },
    {
      "securableResourceDomainType": "Folder",
      "securableResourceId": "${json-unit.matches:id}",
      "createdBy": "unit-test@test.com",
      "availableActions": ["delete","show","update"],
      "id": "${json-unit.matches:id}",
      "userGroup": {
        "name": "readers",
        "id": "${json-unit.matches:id}"
      },
      "groupRole": {
        "displayName": "Reader",
        "name": "reader",
        "id": "${json-unit.matches:id}"
      }
    },
    {
      "securableResourceDomainType": "Folder",
      "securableResourceId": "${json-unit.matches:id}",
      "createdBy": "unit-test@test.com",
      "availableActions": ["delete","show","update"],
      "id": "${json-unit.matches:id}",
      "userGroup": {
        "name": "folderAdmins",
        "id": "${json-unit.matches:id}"
      },
      "groupRole": {
        "displayName": "Container Administrator",
        "name": "container_admin",
        "id": "${json-unit.matches:id}"
      }
    }
  ]
}'''
    }

    @Override
    String getExpectedNullSavedJson() {
        '''{
  "total": 2,
  "errors": [
    {"message": "Property [userGroup] of class [class uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole] cannot be null"},
    {"message": "Property [groupRole] of class [class uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole] cannot be null"}
    ]
 }'''
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '''{
  "total": 2,
  "errors": [
    {"message": "Property [securableResourceId] of class [class uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole] ''' +
        """with value [${folder.id}] must be unique by usergroup with value [${readers.id}]"},
    {"message": "Property [groupRole] of class [class uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole] """ +
        '''cannot be an application level GroupRole"}
  ]
}'''
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
  "securableResourceDomainType": "Folder",
  "securableResourceId": "${json-unit.matches:id}",
  "createdBy": "unlogged_user@mdm-core.com",
  "availableActions": ["delete","show","update"],
  "id": "${json-unit.matches:id}",
  "userGroup": {
    "name": "addtl",
    "id": "${json-unit.matches:id}"
  },
  "groupRole": {
    "displayName": "Reader",
    "name": "reader",
    "id": "${json-unit.matches:id}"
  }
}'''
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "securableResourceDomainType": "Folder",
  "securableResourceId": "${json-unit.matches:id}",
  "createdBy": "unit-test@test.com",
  "availableActions": ["delete","show","update"],
  "id": "${json-unit.matches:id}",
  "userGroup": {
    "name": "editors",
    "id": "${json-unit.matches:id}"
  },
  "groupRole": {
    "displayName": "Editor",
    "name": "editor",
    "id": "${json-unit.matches:id}"
  }
}'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '''{
  "total": 2,
  "errors": [
    {
      "message": "Property [securableResourceId] of class [class uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole] ''' +
        """with value [${folder.id}] must be unique by usergroup with value [${readers.id}]"
    },
    {
      "message": "Property [userGroup] of class [class uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole] """ +
        '''cannot be changed once set"
    }
  ]
}'''
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
  "securableResourceDomainType": "Folder",
  "securableResourceId": "${json-unit.matches:id}",
  "createdBy": "unit-test@test.com",
  "availableActions": ["delete","show","update"],
  "id": "${json-unit.matches:id}",
  "userGroup": {
    "name": "editors",
    "id": "${json-unit.matches:id}"
  },
  "groupRole": {
    "displayName": "Reader",
    "name": "reader",
    "id": "${json-unit.matches:id}"
  }
}'''
    }

    @Override
    SecurableResourceGroupRole invalidUpdate(SecurableResourceGroupRole instance) {
        instance.userGroup = readers
        instance
    }

    @Override
    SecurableResourceGroupRole validUpdate(SecurableResourceGroupRole instance) {
        instance.groupRole = GroupRole.findByName('reader')
        instance
    }

    @Override
    SecurableResourceGroupRole getInvalidUnsavedInstance() {
        new SecurableResourceGroupRole(securableResource: folder, userGroup: readers, groupRole: GroupRole.findByName('user_admin'))
    }

    @Override
    SecurableResourceGroupRole getValidUnsavedInstance() {
        new SecurableResourceGroupRole(securableResource: folder, userGroup: addtl, groupRole: GroupRole.findByName('reader'))
    }

    @Override
    String getTemplate() {
        '''import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole

model {
    SecurableResourceGroupRole securableResourceGroupRole
}

json {
    securableResourceDomainType securableResourceGroupRole.securableResourceDomainType
    securableResourceId securableResourceGroupRole.securableResourceId
    userGroup tmpl.'/userGroup/userGroup'(securableResourceGroupRole.userGroup)
    groupRole tmpl.'/groupRole/groupRole'(securableResourceGroupRole.groupRole)
}
'''
    }
}
