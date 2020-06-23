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
package uk.ac.ox.softeng.maurodatamapper.security.role

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.PathAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.search.Lucene
import uk.ac.ox.softeng.maurodatamapper.search.PaginatedLuceneResult
import uk.ac.ox.softeng.maurodatamapper.search.PathTokenizerAnalyzer
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup

import grails.gorm.DetachedCriteria
import org.grails.datastore.gorm.GormEntity

class GroupRole implements EditHistoryAware, PathAware, SecurableResource, Comparable<GroupRole> {

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
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        parent nullable: true
        name blank: false, validator: {val -> if (val.find(/\s/)) ['invalid.grouprole.name.message']}, unique: true
        displayName blank: false
        children nullable: false, minSize: 0
        securedResourceGroupRoles nullable: false, minSize: 0
        userGroups nullable: false, minSize: 0, validator: {val, obj ->
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
        name index: 'yes'
        path index: 'yes', analyzer: PathTokenizerAnalyzer
    }

    GroupRole() {
        children = []
        securedResourceGroupRoles = []
        userGroups = []
    }

    @Override
    GormEntity getPathParent() {
        parent
    }

    @Override
    def beforeValidate() {
        buildPath()
        children.each {it.beforeValidate()}
    }

    @Override
    def beforeInsert() {
        buildPath()
    }

    @Override
    def beforeUpdate() {
        buildPath()
    }

    @Override
    String getDomainType() {
        GroupRole.simpleName
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
            this.applicationLevelRole ? it.applicationLevelRole == this.applicationLevelRole : true
        }.each {
            allowed.addAll(it.extractAllowedRoles())
        }
        // Make sure the container group admin role is included inside container admin
        if (name == CONTAINER_ADMIN_ROLE_NAME) allowed.add(findByName(CONTAINER_GROUP_ADMIN_ROLE_NAME))
        allowed
    }

    @Override
    int compareTo(GroupRole that) {
        if (this.applicationLevelRole != that.applicationLevelRole) {
            throw new ApiInternalException('GR01', 'Incomparable group roles')
        }
        this.depth <=> that.depth
    }

    static GroupRole findByName(String name) {
        new DetachedCriteria<GroupRole>(GroupRole).eq('name', name).get()
    }

    static DetachedCriteria<GroupRole> byApplicationLevelRole() {
        new DetachedCriteria<GroupRole>(GroupRole).eq('applicationLevelRole', true)
    }

    static PaginatedLuceneResult<GroupRole> findAllByGroupRole(GroupRole groupRole, Map pagination) {
        Lucene.paginatedList(GroupRole, pagination) {
            should {
                keyword 'id', groupRole.id.toString()
                keyword 'path', groupRole.id.toString()
            }
        }
    }

    static PaginatedLuceneResult<GroupRole> findAllFolderLevelRoles(GroupRole topLevelFolderRole, Map pagination) {
        Lucene.paginatedList(GroupRole, pagination) {
            should {
                keyword 'name', 'container_group_admin'
                keyword 'id', topLevelFolderRole.id.toString()
                keyword 'path', topLevelFolderRole.id.toString()

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
