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
package uk.ac.ox.softeng.maurodatamapper.security.policy

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.VirtualSecurableResourceGroupRole

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j

import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

@Slf4j
@CompileStatic
class UserSecurityPolicy {

    // Locked as soon as its created
    // Lock allows us to hold while we wait for access to be updated
    final private AtomicBoolean locked = new AtomicBoolean(true)
    // Destroyed allows us to concurrently return empty lists if the policy is revoked
    final private AtomicBoolean destroyed = new AtomicBoolean(false)

    private CatalogueUser user
    private Set<UserGroup> userGroups
    private List<SecurableResourceGroupRole> securableResourceGroupRoles
    private Map<UUID, SortedSet<VirtualSecurableResourceGroupRole>> virtualSecurableResourceGroupRoles
    private SortedSet<GroupRole> applicationPermittedRoles
    private int maxLock = 5

    private UserSecurityPolicy() {
        userGroups = [] as Set
        applicationPermittedRoles = new TreeSet<>()
        securableResourceGroupRoles = []
        virtualSecurableResourceGroupRoles = [:]
    }

    CatalogueUser getUser() {
        user
    }

    Set<UserGroup> getUserGroups() {
        holdForLock() // Ensure roles are only returned if theres no lock
        destroyed.get() ? Collections.emptySet() : userGroups
    }

    List<UUID> getUserGroupIds() {
        userGroups*.id
    }

    Set<GroupRole> getAssignedUserGroupApplicationRoles() {
        userGroups.collect {it.applicationGroupRole}.findAll().toSet()
    }

    List<SecurableResourceGroupRole> getSecurableResourceGroupRoles() {
        holdForLock() // Ensure roles are only returned if theres no lock
        destroyed.get() ? Collections.emptyList() : securableResourceGroupRoles
    }

    VirtualSecurableResourceGroupRole getVirtualRoleForSecuredResource(Class<? extends SecurableResource> securableResourceClass, UUID id, String roleName) {
        SortedSet roles = getVirtualSecurableResourceGroupRoles()[id]
        roles && roles.first().matchesDomainResourceType(securableResourceClass) ? roles.find {it.matchesGroupRole(roleName)} : null
    }

    Map<UUID, SortedSet<VirtualSecurableResourceGroupRole>> getVirtualSecurableResourceGroupRoles() {
        holdForLock() // Ensure roles are only returned if theres no lock
        destroyed.get() ? Collections.emptyMap() : virtualSecurableResourceGroupRoles
    }

    Set<GroupRole> getApplicationPermittedRoles() {
        holdForLock() // Ensure roles are only returned if theres no lock
        destroyed.get() ? Collections.emptySet() : applicationPermittedRoles
    }

    List<SecurableResourceGroupRole> getSecurableResourceGroupRolesForBuilding() {
        if (!isLocked()) throw new ApiInternalException('USP', 'Cannot build on an unlocked UserPolicy')
        destroyed.get() ? Collections.emptyList() : securableResourceGroupRoles
    }

    Set<VirtualSecurableResourceGroupRole> getVirtualSecurableResourceGroupRolesForBuilding() {
        if (!isLocked()) throw new ApiInternalException('USP', 'Cannot build on an unlocked UserPolicy')
        destroyed.get() ? Collections.emptySet() : virtualSecurableResourceGroupRoles.values() as Set<VirtualSecurableResourceGroupRole>
    }

    Set<VirtualSecurableResourceGroupRole> getVirtualSecurableResourceGroupRolesForBuildingById(UUID id) {
        if (!isLocked()) throw new ApiInternalException('USP', 'Cannot build on an unlocked UserPolicy')
        destroyed.get() ? Collections.emptySet() : virtualSecurableResourceGroupRoles[id] as Set<VirtualSecurableResourceGroupRole>
    }

    Set<GroupRole> getApplicationPermittedRolesForBuilding() {
        if (!isLocked()) throw new ApiInternalException('USP', 'Cannot build on an unlocked UserPolicy')
        destroyed.get() ? Collections.emptySet() : applicationPermittedRoles
    }

    UserSecurityPolicy ensureCatalogueUser(CatalogueUser catalogueUser) {
        if (user.id != catalogueUser.id) throw new ApiInternalException('UP01', 'Cannot change the user of a UserPolicy')
        user = catalogueUser
        this
    }

    UserSecurityPolicy forUser(CatalogueUser catalogueUser) {
        user = catalogueUser
        this
    }

    UserSecurityPolicy inGroups(Set<UserGroup> userGroups) {
        this.userGroups = userGroups
        this
    }

    UserSecurityPolicy withMaxLockTime(int maxLockTime) {
        this.maxLock = maxLockTime
        this
    }

    UserSecurityPolicy updateWithUserGroup(UserGroup userGroup) {
        userGroups << userGroup
        this
    }

    UserSecurityPolicy updateWithoutUserGroup(UserGroup userGroup) {
        userGroups.remove(userGroup)
        this
    }

    UserSecurityPolicy withSecurableRoles(List<SecurableResourceGroupRole> securableResourceGroupRoles) {
        this.securableResourceGroupRoles = securableResourceGroupRoles
        this
    }

    UserSecurityPolicy includeSecurableRoles(List<SecurableResourceGroupRole> securableResourceGroupRoles) {
        this.securableResourceGroupRoles.addAll(securableResourceGroupRoles)
        this
    }

    UserSecurityPolicy withVirtualRoles(Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles) {
        this.virtualSecurableResourceGroupRoles = (virtualSecurableResourceGroupRoles
            .groupBy {it.domainId}
            .collectEntries {
                [it.key, new TreeSet<>(it.value)]
            } as Map<UUID, SortedSet<VirtualSecurableResourceGroupRole>> )
        this
    }

    UserSecurityPolicy withApplicationRoles(Set<GroupRole> applicationPermittedRoles) {
        this.applicationPermittedRoles = new TreeSet<>(applicationPermittedRoles)
        this
    }

    UserSecurityPolicy includeApplicationRoles(Set<GroupRole> applicationPermittedRoles) {
        this.applicationPermittedRoles.addAll(applicationPermittedRoles)
        this
    }

    UserSecurityPolicy includeVirtualRoles(Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles) {
        Map<UUID, List<VirtualSecurableResourceGroupRole>> grouped = virtualSecurableResourceGroupRoles.groupBy {it.domainId}
        grouped.each {id, roles ->
            this.@virtualSecurableResourceGroupRoles.merge(id,
                                                          new TreeSet<VirtualSecurableResourceGroupRole>(roles.toSet()),
                                                          {existing, addtl ->
                                                              existing + addtl
                                                          })
        }
        this
    }

    UserSecurityPolicy removeVirtualRolesForSecurableResource(SecurableResource securableResource) {
        this.virtualSecurableResourceGroupRoles.remove(securableResource.resourceId)
        this
    }

    UserSecurityPolicy removeVirtualRoles(Collection<VirtualSecurableResourceGroupRole> allRolesToBeRemoved) {
        Map<UUID, List<VirtualSecurableResourceGroupRole>> grouped = allRolesToBeRemoved.groupBy {it.domainId}
        grouped.each {id, idRolesToBeRemoved ->
            this.@virtualSecurableResourceGroupRoles.computeIfPresent(id,
                                                                     {k, existing ->
                                                                         existing.removeAll(idRolesToBeRemoved)
                                                                         existing
                                                                     })
        }
        this
    }

    UserSecurityPolicy removeAssignedRoleIf(@ClosureParams(
        value = SimpleType,
        options = 'uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole') Closure predicate) {
        securableResourceGroupRoles.removeIf([test: predicate] as Predicate)
        this
    }

    UserSecurityPolicy lock() {
        locked.set(true)
        this
    }

    UserSecurityPolicy unlock() {
        locked.set(false)
        this
    }

    void destroy() {
        destroyed.set(true)
    }

    boolean isLocked() {
        locked.get()
    }

    boolean isAuthenticated() {
        user && user.emailAddress != UnloggedUser.instance.emailAddress
    }

    boolean hasUserGroups() {
        userGroups
    }

    UserSecurityPolicy setNoAccess() {
        applicationPermittedRoles = new TreeSet<>()
        securableResourceGroupRoles = []
        virtualSecurableResourceGroupRoles = [:]
        this
    }

    boolean isManagedByGroup(UserGroup userGroup) {
        userGroup.id in userGroups*.id
    }

    boolean managesVirtualAccessToSecurableResource(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        Set<VirtualSecurableResourceGroupRole> roles = virtualSecurableResourceGroupRoles[id]
        roles ? roles.first().matchesDomainResourceType(securableResourceClass) : false
    }

    boolean managesVirtualAccessToSecurableResource(SecurableResource securableResource) {
        managesVirtualAccessToSecurableResource(securableResource.class, securableResource.resourceId)
    }

    GroupRole getHighestApplicationLevelAccess() {
        applicationPermittedRoles.first() // Sorted set so the first entry is always the highest acess
    }

    GroupRole findHighestAccessToSecurableResource(String securableResourceDomainType, UUID securableResourceId) {
        Set<VirtualSecurableResourceGroupRole> found = getVirtualSecurableResourceGroupRolesForBuildingById(securableResourceId)
        if (!found || !found.first().matchesDomainResourceType(securableResourceDomainType)) return null
        found.sort().first().groupRole
    }

    GroupRole findHighestAssignedAccessToSecurableResource(String securableResourceDomainType, UUID securableResourceId) {
        List<SecurableResourceGroupRole> found = getSecurableResourceGroupRolesForBuilding().findAll {
            it.securableResourceDomainType == securableResourceDomainType && it.securableResourceId == securableResourceId
        }
        if (!found) return null
        found.collect {it.groupRole}.sort().first()
    }

    void holdForLock() {
        // If the policy is locked then wait till it is unlocked
        // This will block for a max of 30s before erroring
        long start = System.currentTimeMillis()
        while (isLocked()) {
            if (Duration.ofMillis(System.currentTimeMillis() - start) > Duration.ofSeconds(maxLock)) {
                throw new ApiInternalException('USP', "UserSecurityPolicy has been locked for more than ${maxLock} seconds")
            }
            sleep(1) // Wait 1ms
        }
    }

    static UserSecurityPolicy builder() {
        new UserSecurityPolicy()
    }
}
