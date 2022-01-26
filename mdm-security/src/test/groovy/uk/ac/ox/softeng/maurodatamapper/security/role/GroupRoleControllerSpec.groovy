/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Edit
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.security.test.BasicModelItem
import uk.ac.ox.softeng.maurodatamapper.security.test.SecurityUsers
import uk.ac.ox.softeng.maurodatamapper.test.unit.ResourceControllerSpec
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.testing.gorm.DomainUnitTest
import grails.testing.web.controllers.ControllerUnitTest
import groovy.util.logging.Slf4j

import static io.micronaut.http.HttpStatus.METHOD_NOT_ALLOWED
import static io.micronaut.http.HttpStatus.OK

@Slf4j
class GroupRoleControllerSpec extends ResourceControllerSpec<GroupRole> implements ControllerUnitTest<GroupRoleController>,
    DomainUnitTest<GroupRole>, SecurityUsers {

    def setup() {
        log.debug('Setting up GroupRoleControllerSpec')
        mockDomains(CatalogueUser, Edit, UserGroup, Folder, BasicModelItem, BasicModel)
        implementSecurityUsers('unitTest')
        checkAndSave(GroupRole.getDefaultGroupRoleModelStructure())
        GroupRole d = GroupRole.findByName('group_admin')
        domain.name = d.name
        domain.displayName = d.displayName
        domain.createdBy = d.createdBy
        domain.applicationLevelRole = d.applicationLevelRole
        domain.id = d.id

        controller.groupRoleService = Stub(GroupRoleService) {
            get(_) >> {UUID id -> GroupRole.get(id)}
            findAllByUser(_, _) >> GroupRole.list()
            save(_) >> {GroupRole gr -> gr.save(flush: true)}
            delete(_) >> {_ -> throw new ApiInternalException('GRSXX', 'Deletion of GroupRoles is not permitted')}
            findAllApplicationLevelRoles(_) >> GroupRole.byApplicationLevelRole().list()
            findAllSecurableResourceLevelRoles(_) >> {Class resourceClass ->
                if (Utils.parentClassIsAssignableFromChild(Container, resourceClass)) {
                    return [GroupRole.findByName(GroupRole.CONTAINER_ADMIN_ROLE_NAME),
                            GroupRole.findByName('container_group_admin'),
                            GroupRole.findByName('editor'),
                            GroupRole.findByName('author'),
                            GroupRole.findByName('reviewer'),
                            GroupRole.findByName('reader')] as HashSet
                }
                if (Utils.parentClassIsAssignableFromChild(Model, resourceClass)) {
                    return [GroupRole.findByName('editor'),
                            GroupRole.findByName('author'),
                            GroupRole.findByName('reviewer'),
                            GroupRole.findByName('reader')] as HashSet
                }
                throw new ApiBadRequestException('GRS02', "Cannot get roles for unknown securable resource class ${resourceClass.simpleName}")
            }
        }

        givenParameters()
    }

    @Override
    String getExpectedIndexJson() {
        '''{
  "count": 10,
  "items": [
     {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Reader",
      "name": "reader",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Reviewer",
      "name": "reviewer",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Author",
      "name": "author",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Editor",
      "name": "editor",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Container Administrator",
      "name": "container_admin",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": true,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Container Group Administrator",
      "name": "container_group_admin",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": true,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Group Administrator",
      "name": "group_admin",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": true,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "User Administrator",
      "name": "user_admin",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": true,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Application Administrator",
      "name": "application_admin",
      "id": "${json-unit.matches:id}"
    },
    {
      "applicationLevelRole": true,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Site Administrator",
      "name": "site_admin",
      "id": "${json-unit.matches:id}"
    }
  ]
}'''
    }

    @Override
    String getExpectedNullSavedJson() {
        '{"total": 2,"errors": [' +
        '{"message": "Property [displayName] of class [class uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole] cannot be null"},' +
        '{"message": "Property [name] of class [class uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole] cannot be null"}' +
        ']}'
    }

    @Override
    String getExpectedInvalidSavedJson() {
        '{"total": 2,"errors": [' +
        '{"message": "Property [displayName] of class [class uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole] cannot be null"},' +
        '{"message": "Property [name] of class [class uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole] with value [new role] cannot' +
        ' contain spaces"}' +
        ']}'
    }

    @Override
    String getExpectedValidSavedJson() {
        '''{
  "parent": "${json-unit.matches:id}",
  "applicationLevelRole": false,
  "createdBy": "unlogged_user@mdm-core.com",
  "availableActions": ["delete","show","update"],
  "displayName": "New Role",
  "name": "new_role",
  "id": "${json-unit.matches:id}"
}'''
    }

    @Override
    String getExpectedShowJson() {
        '''{
  "applicationLevelRole": true,
  "createdBy": "mdm-security@maurodatamapper.com",
  "availableActions": ["delete","show","update"],
  "displayName": "Group Administrator",
  "name": "group_admin",
  "id": "${json-unit.matches:id}",
  "parent": "${json-unit.matches:id}"
}'''
    }

    @Override
    String getExpectedInvalidUpdatedJson() {
        '{"total": 1,"errors": [' +
        '{"message": "Property [name] of class [class uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole] with value [updated role] ' +
        'cannot contain spaces"}' +
        ']}'
    }

    @Override
    String getExpectedValidUpdatedJson() {
        '''{
  "applicationLevelRole": true,
  "createdBy": "mdm-security@maurodatamapper.com",
  "availableActions": ["delete","show","update"],
  "displayName": "Updated display name",
  "name": "group_admin",
  "id": "${json-unit.matches:id}",
  "parent": "${json-unit.matches:id}"
}'''
    }

    @Override
    GroupRole invalidUpdate(GroupRole instance) {
        instance.name = 'updated role'
        instance
    }

    @Override
    GroupRole validUpdate(GroupRole instance) {
        instance.displayName = 'Updated display name'
        instance
    }

    @Override
    GroupRole getInvalidUnsavedInstance() {
        new GroupRole(name: 'new role')
    }

    @Override
    GroupRole getValidUnsavedInstance() {
        new GroupRole(name: 'new_role', displayName: 'New Role', parent: GroupRole.findByName('reader'))
    }

    @Override
    void verifyR53DeleteActionWithAnInstanceResponse() {
        verifyResponse METHOD_NOT_ALLOWED
    }

    @Override
    String getTemplate() {
        '''import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole

model {
    GroupRole groupRole
}

json {
    id groupRole.id
    name groupRole.name
    displayName groupRole.displayName
    if (groupRole.parent) parent groupRole.parent.id
}'''
    }

    void 'test listing application group roles'() {
        when:
        controller.listApplicationGroupRoles()

        then:
        verifyJsonResponse OK, '''{
  "count": 5,
  "items": [
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": true,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Container Group Administrator",
      "name": "container_group_admin",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": true,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Group Administrator",
      "name": "group_admin",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": true,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "User Administrator",
      "name": "user_admin",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": true,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Application Administrator",
      "name": "application_admin",
      "id": "${json-unit.matches:id}"
    },
    {
      "applicationLevelRole": true,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Site Administrator",
      "name": "site_admin",
      "id": "${json-unit.matches:id}"
    }
  ]
}'''
    }

    void 'test listing group roles for valid container securable resource'() {
        given:
        params.securableResourceClass = Folder

        when:
        controller.listGroupRolesAvailableToSecurableResource()

        then:
        verifyJsonResponse OK, '''{
  "count": 6,
  "items": [
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Container Administrator",
      "name": "container_admin",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": true,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Container Group Administrator",
      "name": "container_group_admin",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Editor",
      "name": "editor",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Author",
      "name": "author",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Reviewer",
      "name": "reviewer",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Reader",
      "name": "reader",
      "id": "${json-unit.matches:id}"
    }
  ]
}'''
    }

    void 'test listing group roles for valid model securable resource'() {
        given:
        params.securableResourceClass = BasicModel

        when:
        controller.listGroupRolesAvailableToSecurableResource()

        then:
        verifyJsonResponse OK, '''{
  "count": 4,
  "items": [
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Editor",
      "name": "editor",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Author",
      "name": "author",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Reviewer",
      "name": "reviewer",
      "id": "${json-unit.matches:id}"
    },
    {
      "parent": "${json-unit.matches:id}",
      "applicationLevelRole": false,
      "createdBy": "mdm-security@maurodatamapper.com",
      "availableActions": ["delete","show","update"],
      "displayName": "Reader",
      "name": "reader",
      "id": "${json-unit.matches:id}"
    }
  ]
}'''
    }

    void 'test listing group roles for non-securable resource'() {
        given:
        params.securableResourceClass = BasicModelItem

        when:
        controller.listGroupRolesAvailableToSecurableResource()

        then:
        Exception ex = thrown(ApiBadRequestException)

        and:
        ex.errorCode == 'GRS02'
    }
}