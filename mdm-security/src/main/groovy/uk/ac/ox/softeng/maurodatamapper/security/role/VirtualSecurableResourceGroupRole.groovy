/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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


import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup

import org.springframework.core.Ordered

class VirtualSecurableResourceGroupRole implements Ordered, Comparable<VirtualSecurableResourceGroupRole> {

    private GroupRole groupRole
    private String domainType
    private String alternateDomainType
    private UUID domainId
    private GroupRole appliedGroupRole
    private UserGroup userGroup
    private int order
    private boolean finalised
    // Defines if resource COULD be finalised, other factors may apply
    private boolean finalisable
    // Defines if resource COULD be versioned, other factors may apply
    private boolean versionable
    // Has the resource had a version set outside of its control, i.e. by the parent
    private boolean versionControlled
    // Does the resource contain versioned contents
    private boolean versionedContents

    private UUID dependsOnDomainIdAccess

    // Should only use the service to build a new object
    protected VirtualSecurableResourceGroupRole() {
        finalised = false
        finalisable = false
        versionable = false
        versionControlled = false
    }

    protected VirtualSecurableResourceGroupRole forSecurableResource(SecurableResource securableResource) {
        this.domainType = securableResource.domainType
        this.domainId = securableResource.resourceId
        this
    }

    VirtualSecurableResourceGroupRole withAlternateDomainType(String alternateDomainType) {
        this.alternateDomainType = alternateDomainType
        this
    }

    VirtualSecurableResourceGroupRole withDependencyOnAccessToDomainId(UUID domainIdDependency) {
        this.dependsOnDomainIdAccess = domainIdDependency
        this
    }

    VirtualSecurableResourceGroupRole withAccessLevel(GroupRole groupRole) {
        this.groupRole = groupRole
        this.order = groupRole.path?.size()
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

    VirtualSecurableResourceGroupRole asFinalised(boolean finalised) {
        this.finalised = finalised
        this
    }

    VirtualSecurableResourceGroupRole asFinalisable(boolean canFinalise) {
        this.finalisable = canFinalise
        this
    }

    VirtualSecurableResourceGroupRole asVersionable(boolean canVersion) {
        this.versionable = canVersion
        this
    }

    VirtualSecurableResourceGroupRole asVersionControlled(boolean versionControlled) {
        this.versionControlled = versionControlled
        this
    }

    VirtualSecurableResourceGroupRole withVersionedContents(boolean versionedContents) {
        this.versionedContents = versionedContents
        this
    }

    @Override
    String toString() {
        "${groupRole?.name ?: 'Unassigned'} : ${domainType}:${domainId}"
    }

    @Override
    int compareTo(VirtualSecurableResourceGroupRole that) {
        if (this.domainType != that.domainType || this.domainId != that.domainId) return LOWEST_PRECEDENCE
        if (this.groupRole.applicationLevelRole && !that.groupRole.applicationLevelRole) return HIGHEST_PRECEDENCE
        if (!this.groupRole.applicationLevelRole && that.groupRole.applicationLevelRole) return LOWEST_PRECEDENCE
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

    boolean matchesDomainResource(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        (domainType == securableResourceClass.simpleName || alternateDomainType == securableResourceClass.simpleName) && domainId == id
    }

    boolean matchesDomainResourceType(Class<? extends SecurableResource> securableResourceClass) {
        (domainType == securableResourceClass.simpleName || alternateDomainType == securableResourceClass.simpleName)
    }

    boolean matchesDomainResourceType(String securableResourceDomainType) {
        (domainType == securableResourceDomainType || alternateDomainType == securableResourceDomainType)
    }

    boolean matchesGroupRole(String roleName) {
        groupRole.name == roleName
    }

    GroupRole getGroupRole() {
        groupRole
    }

    String getDomainType() {
        domainType
    }

    String getAlternateDomainType() {
        alternateDomainType
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

    boolean isFinalised() {
        finalised
    }

    boolean canFinalise() {
        !finalised && finalisable
    }

    boolean canVersion() {
        !versionControlled && finalised && versionable
    }

    boolean isVersionControlled() {
        versionControlled
    }

    boolean isVersionable() {
        versionable
    }

    boolean hasVersionedContents() {
        versionedContents
    }

    int getOrder() {
        order
    }

    UUID getDependsOnDomainIdAccess() {
        dependsOnDomainIdAccess
    }
}
