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


import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.VersionAware
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.springframework.core.Ordered

class VirtualSecurableResourceGroupRole implements Ordered, Comparable<VirtualSecurableResourceGroupRole> {

    private GroupRole groupRole
    private String domainType
    private UUID domainId
    private GroupRole appliedGroupRole
    private UserGroup userGroup
    private int order
    private boolean finalisedModel
    private boolean finalisableModel

    VirtualSecurableResourceGroupRole() {
        finalisedModel = false
        finalisableModel = false
    }

    VirtualSecurableResourceGroupRole fromSecurableResourceGroupRole(SecurableResourceGroupRole securableResourceGroupRole) {
        this.forSecurableResource(securableResourceGroupRole.securableResourceDomainType, securableResourceGroupRole.securableResourceId)
            .asFinalisedModel(securableResourceGroupRole.finalisedModel ?: false)
            .definedByGroup(securableResourceGroupRole.userGroup)
            .definedByAccessLevel(securableResourceGroupRole.groupRole)
            .withModelCanBeFinalised(securableResourceGroupRole.canFinaliseModel ?: false)
    }

    VirtualSecurableResourceGroupRole forSecurableResource(String domainType, UUID domainId) {
        this.domainType = domainType
        this.domainId = domainId
        this
    }

    VirtualSecurableResourceGroupRole forSecurableResource(SecurableResource securableResource) {

        if (Utils.parentClassIsAssignableFromChild(VersionAware, securableResource.class)) {
            VersionAware versionAware = (securableResource as VersionAware)
            return this.forSecurableResource(securableResource.domainType, securableResource.resourceId)
                .asFinalisedModel(versionAware.finalised)
                .withModelCanBeFinalised(versionAware.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME)
        }
        this.forSecurableResource(securableResource.domainType, securableResource.resourceId)
    }

    VirtualSecurableResourceGroupRole withAccessLevel(GroupRole groupRole) {
        this.groupRole = groupRole
        this.order = groupRole.path ? groupRole.path.split('/').size() : 0
        this
    }

    VirtualSecurableResourceGroupRole definedByAccessLevel(GroupRole appliedGroupRole) {
        this.appliedGroupRole = appliedGroupRole
        this
    }

    VirtualSecurableResourceGroupRole definedByGroup(UserGroup userGroup) {
        this.userGroup = userGroup
        this
    }

    VirtualSecurableResourceGroupRole asFinalisedModel(boolean finalised) {
        this.finalisedModel = finalised
        if (finalised) this.finalisableModel = false
        this
    }

    VirtualSecurableResourceGroupRole withModelCanBeFinalised(boolean canFinalise) {
        this.finalisableModel = this.finalisedModel ? false : canFinalise
        this
    }


    @Override
    String toString() {
        "${groupRole.name} : ${domainType}:${domainId}"
    }

    @Override
    int compareTo(VirtualSecurableResourceGroupRole that) {
        if (this.domainType != that.domainType || this.domainId != that.domainId) return LOWEST_PRECEDENCE
        this.order <=> that.order
    }

    boolean equals(Object o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        VirtualSecurableResourceGroupRole that = (VirtualSecurableResourceGroupRole) o

        // We want to store all applied roles for each domain type and id in a set, we can then sort by the order
        if (groupRole != that.groupRole) return false
        if (domainId != that.domainId) return false
        if (domainType != that.domainType) return false
        if (userGroup != that.userGroup) return false

        return true
    }

    int hashCode() {
        int result
        result = (domainType != null ? domainType.hashCode() : 0)
        result = 31 * result + (domainId != null ? domainId.hashCode() : 0)
        result = 31 * result + (groupRole != null ? groupRole.hashCode() : 0)
        result = 31 * result + (userGroup != null ? userGroup.hashCode() : 0)
        return result
    }

    GroupRole getGroupRole() {
        groupRole
    }

    String getDomainType() {
        domainType
    }

    UUID getDomainId() {
        domainId
    }

    GroupRole getAppliedGroupRole() {
        appliedGroupRole
    }

    UserGroup getUserGroup() {
        userGroup
    }

    boolean isFinalisedModel() {
        finalisedModel
    }

    boolean canFinaliseModel() {
        finalisableModel
    }

    int getOrder() {
        order
    }
}
