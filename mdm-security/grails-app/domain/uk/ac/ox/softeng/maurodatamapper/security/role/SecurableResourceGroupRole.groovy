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


import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.gorm.DetachedCriteria

class SecurableResourceGroupRole implements MdmDomain, EditHistoryAware {

    UUID id
    String securableResourceDomainType
    UUID securableResourceId
    SecurableResource securableResource

    static belongsTo = [
        groupRole: GroupRole,
        userGroup: UserGroup
    ]

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        securableResourceId validator: {val, obj ->
            if (val && obj.userGroup) {
                if (obj.id) {
                    if (SecurableResourceGroupRole.bySecurableResourceIdAndUserGroupAndNotId(val, obj.userGroup, obj.id).count()) {
                        ['invalid.securableresourcegrouprole.not.unique.message', obj.userGroup.id]
                    }
                } else {
                    if (SecurableResourceGroupRole.bySecurableResourceIdAndUserGroup(val, obj.userGroup).count()) {
                        ['invalid.securableresourcegrouprole.not.unique.message', obj.userGroup.id]
                    }
                }
            }
        }
        groupRole validator: {val -> if (val && val.applicationLevelRole) ['invalid.grouprole.cannot.be.application.level.message']}
        userGroup validator: {val, obj ->
            if (val && obj.ident() && obj.isDirty('userGroup')) ['invalid.grouprole.cannot.change.usergroup.message']
        }
    }

    static transients = ['securableResource']

    static mapping = {
        groupRole fetch: 'join'
        userGroup fetch: 'join'
    }

    @Override
    String getEditLabel() {
        "SecuredResourceGroupRole:${userGroup?.editLabel}:${securableResourceDomainType}:${securableResourceId}"
    }

    @Override
    String getDomainType() {
        SecurableResourceGroupRole.simpleName
    }

    void setSecurableResource(SecurableResource securableResource, boolean justLoad) {
        this.securableResource = securableResource
        if (!justLoad) {
            securableResourceDomainType = securableResource.domainType
            securableResourceId = securableResource.resourceId
        }
    }

    void setSecurableResource(SecurableResource securableResource) {
        setSecurableResource(securableResource, false)
    }

    @Override
    String toString() {
        String idStr = ident() ? ident().toString() : '(unsaved)'
        "${getEditLabel()} : ${idStr}"
    }

    String getPathPrefix() {
        null
    }

    String getPathIdentifier() {
        null
    }

    static DetachedCriteria<SecurableResourceGroupRole> by() {
        new DetachedCriteria<SecurableResourceGroupRole>(SecurableResourceGroupRole)
    }

    static DetachedCriteria<SecurableResourceGroupRole> byUserGroupId(UUID userGroupId) {
        by().eq('userGroup.id', userGroupId)
    }

    static DetachedCriteria<SecurableResourceGroupRole> byUserGroupIds(List<UUID> userGroupIds) {
        by().inList('userGroup.id', userGroupIds)
    }

    static DetachedCriteria<SecurableResourceGroupRole> byUserGroupIdAndId(UUID userGroupId, UUID id) {
        byUserGroupId(userGroupId).idEq(id)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResource(String securableResourceDomainType,
                                                                            UUID securableResourceId) {
        by()
            .eq('securableResourceDomainType', securableResourceDomainType)
            .eq('securableResourceId', securableResourceId)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResourceDomainType(String securableResourceDomainType) {
        by().eq('securableResourceDomainType', securableResourceDomainType)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResourceId(UUID securableResourceId) {
        by().eq('securableResourceId', securableResourceId)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResourceIds(List<UUID> securableResourceIds) {
        by().inList('securableResourceId', securableResourceIds)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResourceDomainTypeAndSecurableResourceIdInList(String securableResourceDomainType,
                                                                                                                  Collection<UUID> securableResourceIds) {
        by()
            .eq('securableResourceDomainType', securableResourceDomainType)
            .inList('securableResourceId', securableResourceIds)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResourceAndUserGroupId(String securableResourceDomainType,
                                                                                          UUID securableResourceId,
                                                                                          UUID userGroupId) {
        bySecurableResource(securableResourceDomainType, securableResourceId).eq('userGroup.id', userGroupId)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResourceIdAndUserGroupAndNotId(UUID securableResourceId,
                                                                                                  UserGroup userGroup,
                                                                                                  UUID id) {
        bySecurableResourceId(securableResourceId).eq('userGroup', userGroup).ne('id', id)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResourceIdAndUserGroup(UUID securableResourceId,
                                                                                          UserGroup userGroup) {
        bySecurableResourceId(securableResourceId).eq('userGroup', userGroup)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResourceAndGroupRoleId(String securableResourceDomainType,
                                                                                          UUID securableResourceId,
                                                                                          UUID groupRoleId) {
        bySecurableResource(securableResourceDomainType, securableResourceId).eq('groupRole.id', groupRoleId)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResourceAndId(String securableResourceDomainType,
                                                                                 UUID securableResourceId,
                                                                                 UUID id) {
        bySecurableResource(securableResourceDomainType, securableResourceId).idEq(id)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResourceAndGroupRoleIdAndUserGroupId(String securableResourceDomainType,
                                                                                                        UUID securableResourceId,
                                                                                                        UUID groupRoleId,
                                                                                                        UUID userGroupId) {
        bySecurableResourceAndGroupRoleId(securableResourceDomainType, securableResourceId, groupRoleId).eq('userGroup.id', userGroupId)
    }

    static DetachedCriteria<SecurableResourceGroupRole> bySecurableResourceAndGroupRoleIdAndUserGroupId(SecurableResource securableResource,
                                                                                                        UUID groupRoleId,
                                                                                                        UUID userGroupId) {
        bySecurableResourceAndGroupRoleIdAndUserGroupId(securableResource.domainType, securableResource.resourceId, groupRoleId, userGroupId)
    }
}
