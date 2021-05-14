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

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.util.Utils

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
    private boolean finalisable
    // Defines if resource COULD be finalised, other factors may apply
    private boolean versionable
    // Defines if resource COULD be versioned, other factors may apply
    private UUID dependsOnDomainIdAccess

    VirtualSecurableResourceGroupRole() {
        finalised = false
        finalisable = false
        versionable = false
    }

    VirtualSecurableResourceGroupRole fromSecurableResourceGroupRole(SecurableResourceGroupRole securableResourceGroupRole) {
        this.forSecurableResource(securableResourceGroupRole.securableResource)
            .definedByGroup(securableResourceGroupRole.userGroup)
            .definedByAccessLevel(securableResourceGroupRole.groupRole)
    }

    VirtualSecurableResourceGroupRole forSecurableResource(SecurableResource securableResource) {
        this.domainType = securableResource.domainType
        this.domainId = securableResource.resourceId
        if (domainType == VersionedFolder.simpleName) alternateDomainType = Folder.simpleName

        if (Utils.parentClassIsAssignableFromChild(Folder, securableResource.class)) {
            dependsOnDomainIdAccess = (securableResource as Folder).parentFolder?.id
        } else if (Utils.parentClassIsAssignableFromChild(Classifier, securableResource.class)) {
            dependsOnDomainIdAccess = (securableResource as Classifier).parentClassifier?.id
        }

        if (Utils.parentClassIsAssignableFromChild(Model, securableResource.class)) {
            Model model = securableResource as Model
            dependsOnDomainIdAccess = model.folder?.id
            asFinalised model.finalised
            // If the container is versioned then models inside it cannot be finalised
            asFinalisable model.folder.domainType != VersionedFolder.simpleName &&
                          model.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
            asVersionable model.folder.domainType != VersionedFolder.simpleName
        }

        if (Utils.parentClassIsAssignableFromChild(VersionedFolder, securableResource.class)) {
            VersionedFolder versionedFolder = securableResource as VersionedFolder
            asFinalised versionedFolder.finalised
            asFinalisable versionedFolder.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME
            asVersionable true
        }

        this
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

    @Override
    String toString() {
        "${groupRole?.name ?: 'Unassigned'} : ${domainType}:${domainId}"
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

    boolean matchesDomainResource(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        (domainType == securableResourceClass.simpleName || alternateDomainType == securableResourceClass.simpleName) && domainId == id
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
        finalised && versionable
    }

    int getOrder() {
        order
    }

    UUID getDependsOnDomainIdAccess() {
        dependsOnDomainIdAccess
    }
}
