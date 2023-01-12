/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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


import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.DetachedCriteria
import grails.rest.Resource

import java.security.Principal

@Resource(readOnly = false, formats = ['json', 'xml'])
class UserGroup implements MdmDomain, EditHistoryAware, SecurableResource, Principal {

    UUID id
    String name
    String description
    Boolean undeleteable

    static hasMany = [
        groupMembers               : CatalogueUser,
        securableResourceGroupRoles: SecurableResourceGroupRole,
    ]

    static belongsTo = [applicationGroupRole: GroupRole]

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        name unique: true, blank: false
        description nullable: true
        groupMembers nullable: false, minSize: 1
        applicationGroupRole nullable: true, validator: {val ->
            if (val && !val.applicationLevelRole) ['invalid.grouprole.must.be.application.level.message']
        }
        securableResourceGroupRoles nullable: false, minSize: 0
        undeleteable nullable: false
    }

    static mapping = {
        groupMembers joinTable: 'join_catalogue_user_to_user_group', index: 'jcutug_user_group_idx', cascade: 'save-update'
        applicationGroupRole fetch: 'join'
    }

    UserGroup() {
        groupMembers = []
        securableResourceGroupRoles = []
        undeleteable = false
    }

    def beforeValidate() {
        checkPath()
    }

    @Override
    String getDomainType() {
        UserGroup.simpleName
    }

    @Override
    String getPathPrefix() {
        'ug'
    }

    @Override
    String getPathIdentifier() {
        name
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
        "UserGroup:${name}"
    }

    String toString() {
        ident() ?
        "${getEditLabel()} : ${ident()}" :
        "${getEditLabel()} : unsaved"
    }

    Boolean hasMember(CatalogueUser user) {
        groupMembers.any {it.emailAddress == user?.emailAddress}
    }

    static DetachedCriteria<UserGroup> by() {
        new DetachedCriteria<UserGroup>(UserGroup)
    }

    static DetachedCriteria<UserGroup> byApplicationGroupRoleId(UUID groupRoleId) {
        by().eq('applicationGroupRole.id', groupRoleId)
    }

    static DetachedCriteria<UserGroup> bySecurableResourceAndGroupRoleId(String securableResourceDomainType, UUID securableResourceId,
                                                                         UUID groupRoleId) {
        where {
            securableResourceGroupRoles {
                eq 'securableResourceDomainType', securableResourceDomainType
                eq 'securableResourceId', securableResourceId
                eq 'groupRole.id', groupRoleId
            }
        }
    }

    static DetachedCriteria<UserGroup> byNameNotInList(List<String> names) {
        by().not {inList('name', names)}
    }

    static DetachedCriteria<UserGroup> withFilter(Map filters, DetachedCriteria<CatalogueUser> criteria = by()) {
        if (filters.label) criteria = criteria.ilike('name', "%${filters.label}%")
        if (filters.name) criteria = criteria.ilike('name', "%${filters.name}%")
        criteria
    }

    static DetachedCriteria<UserGroup> byIdInList(List<UUID> ids) {
        by().inList('id', ids)
    }
}