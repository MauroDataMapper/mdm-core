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

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.DetachedCriteria

class SecurableResourceGroupRole implements EditHistoryAware {

    UUID id
    String securableResourceDomainType
    UUID securableResourceId
    Boolean finalisedModel
    Boolean canFinaliseModel

    static belongsTo = [
        groupRole: GroupRole,
        userGroup: UserGroup
    ]

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        groupRole validator: {val -> if (val && val.applicationLevelRole) ['invalid.grouprole.cannot.be.application.level.message']}
        userGroup validator: {val, obj ->
            if (val && obj.ident() && obj.isDirty('userGroup')) ['invalid.grouprole.cannot.change.usergroup.message']
        }
        finalisedModel nullable: true
        canFinaliseModel nullable: true
    }

    static mapping = {
        groupRole fetch: 'join'
        userGroup fetch: 'join'
    }

    @Override
    String getEditLabel() {
        "SecuredResourceGroupRole:${userGroup.editLabel}:${securableResourceDomainType}:${securableResourceId}"
    }

    @Override
    String getDomainType() {
        SecurableResourceGroupRole.simpleName
    }

    boolean canFinalise(SecurableResource securableResource){
        securableResource.branchName == ModelConstraints.DEFAULT_BRANCH_NAME
    }

    void setSecurableResource(SecurableResource securableResource) {
        securableResourceDomainType = securableResource.domainType
        securableResourceId = securableResource.resourceId
        if (Utils.parentClassIsAssignableFromChild(Model, securableResource.class)) {
            finalisedModel = securableResource.finalised
            canFinaliseModel = !securableResource.finalised && securableResource.branchName == ModelConstraints.DEFAULT_BRANCH_NAME
        }
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
