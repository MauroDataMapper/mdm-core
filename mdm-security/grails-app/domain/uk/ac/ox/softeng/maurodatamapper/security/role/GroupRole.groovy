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
package uk.ac.ox.softeng.maurodatamapper.security.role

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.HibernateSearch
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.PaginatedHibernateSearchResult
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.pojo.bridge.binder.PathBinder
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.DetachedCriteria

class GroupRole implements MdmDomain, EditHistoryAware, SecurableResource, Comparable<GroupRole> {

    public static final String APPLICATION_ADMIN_ROLE_NAME = 'application_admin'
    public static final String CONTAINER_ADMIN_ROLE_NAME = 'container_admin'
    public static final String SITE_ADMIN_ROLE_NAME = 'site_admin'
    public static final String EDITOR_ROLE_NAME = 'editor'
    public static final String AUTHOR_ROLE_NAME = 'author'
    public static final String REVIEWER_ROLE_NAME = 'reviewer'
    public static final String READER_ROLE_NAME = 'reader'
    public static final String CONTAINER_GROUP_ADMIN_ROLE_NAME = 'container_group_admin'
    public static final String USER_ADMIN_ROLE_NAME = 'user_admin'
    public static final String GROUP_ADMIN_ROLE_NAME = 'group_admin'

    UUID id
    String name
    String displayName
    boolean applicationLevelRole

    static hasMany = [
        children                 : GroupRole,
        userGroups               : UserGroup,
        securedResourceGroupRoles: SecurableResourceGroupRole,
    ]

    static belongsTo = [parent: GroupRole]

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        parent nullable: true
        name blank: false, validator: { val -> if (val.find(/\s/)) ['invalid.grouprole.name.message'] }, unique: true
        displayName blank: false
        children nullable: false, minSize: 0
        securedResourceGroupRoles nullable: false, minSize: 0
        userGroups nullable: false, minSize: 0, validator: { val, obj ->
            if (val && !obj.applicationLevelRole) ['invalid.grouprole.application.level.sergroups.message']
        }
    }

    static mapping = {
        cache true
    }

    static mappedBy = [
        children: 'parent'
    ]

    static search = {
        name searchable: 'yes'
        path binder: PathBinder
    }

    GroupRole() {
        children = []
        securedResourceGroupRoles = []
        userGroups = []
    }

    def beforeValidate() {
        checkPath()
        children.each { it.beforeValidate() }
    }

    @Override
    String getDomainType() {
        GroupRole.simpleName
    }

    @Override
    String getPathPrefix() {
        'gr'
    }

    @Override
    String getPathIdentifier() {
        name
    }

    @Override
    Path buildPath() {
        if (parent) {
            // We only want to call the getpath method once
            Path parentPath = parent?.getPath()
            parentPath ? Path.from(parentPath, pathPrefix, pathIdentifier) : null
        } else {
            Path.from(pathPrefix, pathIdentifier)
        }
    }

    @Override
    Boolean getReadableByEveryone() {
        false
    }

    @Override
    Boolean getReadableByAuthenticatedUsers() {
        false
    }

    @Override
    String getEditLabel() {
        "GroupRole:${name}"
    }

    @Override
    String toString() {
        ident() ?
        "${getEditLabel()} : ${ident()}" :
        "${getEditLabel()} : unsaved"
    }

    /**
     * @return All allowed GroupRoles which are part of this role, will include this role
     */
    Set<GroupRole> extractAllowedRoles() {
        Set<GroupRole> allowed = [this] as Set
        children.findAll {
            it.applicationLevelRole == this.applicationLevelRole
        }.each {
            allowed.addAll(it.extractAllowedRoles())
        }
        // Make sure the container group admin role is included inside container admin
        // This is breaking sort/comparisons of the allowedRoles as the CGA is an application role
        //        if (name == CONTAINER_ADMIN_ROLE_NAME) allowed.add(findByName(CONTAINER_GROUP_ADMIN_ROLE_NAME))
        allowed
    }

    @Override
    int compareTo(GroupRole that) {
        if (this.applicationLevelRole != that.applicationLevelRole) {
            throw new ApiInternalException('GR01', 'Incomparable group roles')
        }
        this.path.size() <=> that.path.size()
    }

    static GroupRole findByName(String name) {
        new DetachedCriteria<GroupRole>(GroupRole).eq('name', name).get()
    }

    static DetachedCriteria<GroupRole> byApplicationLevelRole() {
        new DetachedCriteria<GroupRole>(GroupRole).eq('applicationLevelRole', true)
    }

    static PaginatedHibernateSearchResult<GroupRole> findAllByGroupRole(GroupRole groupRole, Map pagination) {
        HibernateSearch.paginatedList(GroupRole, pagination) {
            should {
                keyword 'id', groupRole.id.toString()
                keyword 'path', Path.from(groupRole.path.last())
            }
        }
    }

    static PaginatedHibernateSearchResult<GroupRole> findAllFolderLevelRoles(GroupRole topLevelFolderRole, Map pagination) {
        HibernateSearch.paginatedList(GroupRole, pagination) {
            should {
                keyword 'name', 'container_group_admin'
                keyword 'id', topLevelFolderRole.id.toString()
                keyword 'path', Path.from(topLevelFolderRole.path.last())

            }
        }
    }

    static GroupRole findOrCreate(Map args) {
        GroupRole.findByName(args.name) ?: new GroupRole(args)
    }

    static GroupRole getDefaultGroupRoleModelStructure() {
        GroupRole folderGroupAdmin = findOrCreate(name: CONTAINER_GROUP_ADMIN_ROLE_NAME, displayName: 'Container Group Administrator',
                                                  createdBy: 'mdm-security@maurodatamapper.com', applicationLevelRole: true)
        GroupRole groupAdmin = findOrCreate(name: GROUP_ADMIN_ROLE_NAME, displayName: 'Group Administrator', applicationLevelRole: true,
                                            createdBy: 'mdm-security@maurodatamapper.com')
        GroupRole siteAdmin = findOrCreate(name: SITE_ADMIN_ROLE_NAME, displayName: 'Site Administrator', applicationLevelRole: true,
                                           createdBy: 'mdm-security@maurodatamapper.com')
        GroupRole appAdmin = findOrCreate(name: APPLICATION_ADMIN_ROLE_NAME, displayName: 'Application Administrator', applicationLevelRole: true,
                                          createdBy: 'mdm-security@maurodatamapper.com')
        GroupRole userAdmin = findOrCreate(name: USER_ADMIN_ROLE_NAME, displayName: 'User Administrator', applicationLevelRole: true,
                                           createdBy: 'mdm-security@maurodatamapper.com')
        GroupRole containerAdmin = findOrCreate(name: CONTAINER_ADMIN_ROLE_NAME, displayName: 'Container Administrator', applicationLevelRole: false,
                                                createdBy: 'mdm-security@maurodatamapper.com')
        GroupRole editor = findOrCreate(name: EDITOR_ROLE_NAME, displayName: 'Editor', applicationLevelRole: false,
                                        createdBy: 'mdm-security@maurodatamapper.com')
        GroupRole author = findOrCreate(name: AUTHOR_ROLE_NAME, displayName: 'Author', applicationLevelRole: false,
                                        createdBy: 'mdm-security@maurodatamapper.com')
        GroupRole reviewer = findOrCreate(name: REVIEWER_ROLE_NAME, displayName: 'Reviewer', applicationLevelRole: false,
                                          createdBy: 'mdm-security@maurodatamapper.com')
        GroupRole reader = findOrCreate(name: READER_ROLE_NAME, displayName: 'Reader', applicationLevelRole: false,
                                        createdBy: 'mdm-security@maurodatamapper.com')
        siteAdmin
            .addToChildren(appAdmin
                               .addToChildren(userAdmin
                                                  .addToChildren(groupAdmin
                                                                     .addToChildren(folderGroupAdmin
                                                                     )
                                                  )
                               )
            )
            .addToChildren(
                containerAdmin
                    .addToChildren(folderGroupAdmin)
                    .addToChildren(editor
                                       .addToChildren(author
                                                          .addToChildren(reviewer
                                                                             .addToChildren(reader
                                                                             )
                                                          )
                                       )
                    )
            )
        folderGroupAdmin.parent = groupAdmin
        siteAdmin
    }
}
