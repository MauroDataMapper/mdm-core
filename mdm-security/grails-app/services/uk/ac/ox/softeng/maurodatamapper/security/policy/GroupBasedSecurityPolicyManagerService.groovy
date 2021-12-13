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

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.SecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.UserGroupService
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRoleService
import uk.ac.ox.softeng.maurodatamapper.security.role.VirtualGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.VirtualSecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.VirtualSecurableResourceGroupRoleService
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.plugin.cache.GrailsCache
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.plugin.cache.GrailsCacheManager
import org.springframework.beans.factory.annotation.Autowired

@Transactional
@Slf4j
@CompileStatic
class GroupBasedSecurityPolicyManagerService implements SecurityPolicyManagerService {

    public static final String SECURITY_POLICY_MANAGER_CACHE_KEY = 'securityPolicyManagerCache'

    FolderService folderService
    CatalogueUserService catalogueUserService
    UserGroupService userGroupService
    SecurableResourceGroupRoleService securableResourceGroupRoleService
    GroupRoleService groupRoleService
    UserSecurityPolicyService userSecurityPolicyService
    VirtualSecurableResourceGroupRoleService virtualSecurableResourceGroupRoleService

    GrailsApplication grailsApplication
    GrailsCacheManager grailsCacheManager

    @Autowired(required = false)
    List<ContainerService> containerServices = []

    @Override
    UserSecurityPolicyManager addSecurityForSecurableResource(SecurableResource securableResource, User creator, String resourceName) {
        log.info('Adding initial security for {}:{}', securableResource.domainType, securableResource.resourceId)
        CatalogueUser catalogueUser = catalogueUserService.findOrCreateUserFromInterface(creator)
        GroupRole role
        if (Utils.parentClassIsAssignableFromChild(Container, securableResource.class)) {
            role = groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).groupRole
        } else {
            role = groupRoleService.getFromCache(GroupRole.EDITOR_ROLE_NAME).groupRole
        }

        /*
        Assign the resource to a group based on the user's name, creating that group if it does not already exist
        */
        String controlGroupName = "${creator.getEmailAddress()} Group"
        UserGroup controlGroup
        log.debug("Looking for group ${controlGroupName}")
        controlGroup = userGroupService.findByName(controlGroupName)
        if (!controlGroup) {
            log.debug('Creating group {} and assigning role {}', "${controlGroupName}", role.name)
            controlGroup = userGroupService.generateAndSaveNewGroup(catalogueUser,
                                                                    controlGroupName,
                                                                    "Control group for ${creator.getEmailAddress()}").save(flush: true)

        }
        SecurableResourceGroupRole existing = securableResourceGroupRoleService.findBySecurableResourceAndUserGroup(securableResource.domainType,
                                                                                                                    securableResource.resourceId,
                                                                                                                    controlGroup.id)
        // To ensure only 1 usergroup exists for each role access on a secured service there is now an added constraint on the domain
        // We should also ensure we dont add more security for an existing group, we should only ever update a group's acces to a resource
        if (existing) {
            securableResourceGroupRoleService.updateAndSaveSecurableResourceGroupRole(existing, role)
        } else {
            securableResourceGroupRoleService.createAndSaveSecurableResourceGroupRole(securableResource, role, controlGroup, catalogueUser)
        }

        /*
        If any groups (UserGroups) have been specified then for each check that both UserGroup and GroupRole can be found from the
        groupId and groupRoleId provided. If yes then create a SecurableResourceGroupRole
        */
        processSecurableResourceWithGroups(securableResource, catalogueUser)

        refreshUserSecurityPolicyManager(catalogueUser)
    }

    @Override
    UserSecurityPolicyManager removeSecurityForSecurableResource(SecurableResource securableResource, User currentUser) {
        log.info('Removing security for {}:{}', securableResource.domainType, securableResource.resourceId)

        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()
        keys.each {key ->
            GroupBasedUserSecurityPolicyManager securityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            securityPolicyManager.lock()
            UserSecurityPolicy updatedPolicy = userSecurityPolicyService.updatePolicyToRemoveSecurableResource(securityPolicyManager.userPolicy,
                                                                                                               securableResource)
            storeUserSecurityPolicyManager(securityPolicyManager.withUpdatedUserPolicy(updatedPolicy))
        }

        securableResourceGroupRoleService.deleteAllForSecurableResource(securableResource)

        retrieveUserSecurityPolicyManager(currentUser)
    }

    @Override
    UserSecurityPolicyManager updateSecurityForSecurableResource(SecurableResource securableResource, Set<String> changedSecurityProperties,
                                                                 User currentUser) {
        log.info('Updating the security for {}:{}', securableResource.domainType, securableResource.resourceId)

        if (changedSecurityProperties.contains('readableByEveryone') || changedSecurityProperties.contains('readableByAuthenticatedUsers')) {
            updateBuiltInSecurity(securableResource, changedSecurityProperties)
        }
        if (Utils.parentClassIsAssignableFromChild(Model, securableResource.class)) {
            log.info('Updating security for model due to {} changes', changedSecurityProperties)
            refreshAllUserSecurityPolicyManagersByModel(currentUser, securableResource as Model, changedSecurityProperties)
        }
        if (Utils.parentClassIsAssignableFromChild(VersionedFolder, securableResource.class)) {
            log.info('Updating security for VersionedFolder due to {} changes', changedSecurityProperties)
            refreshAllUserSecurityPolicyManagersByVersionedFolder(currentUser, securableResource as VersionedFolder, changedSecurityProperties)
        }

        retrieveUserSecurityPolicyManager(currentUser)
    }

    @Override
    GroupBasedUserSecurityPolicyManager retrieveUserSecurityPolicyManager(String userEmailAddress) {
        if (!userEmailAddress) return null
        GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = getSecurityPolicyManagerCache().get(userEmailAddress,
                                                                                                            GroupBasedUserSecurityPolicyManager)
        // Make sure the user object is the latest and attached to the session
        //  userSecurityPolicyManager?.forUser(catalogueUserService.findByEmailAddress(userEmailAddress)) TODO do we need this??
        userSecurityPolicyManager
    }

    GroupBasedUserSecurityPolicyManager retrieveUserSecurityPolicyManager(User catalogueUser) {
        if (!catalogueUser) return null
        getSecurityPolicyManagerCache().get(catalogueUser.emailAddress, GroupBasedUserSecurityPolicyManager)
    }

    @Override
    UserSecurityPolicyManager reloadUserSecurityPolicyManager(String userEmailAddress) {
        removeUserSecurityPolicyManager(userEmailAddress)
        UserSecurityPolicyManager reloaded = buildNewUserSecurityPolicyManager(catalogueUserService.findByEmailAddress(userEmailAddress))
        storeUserSecurityPolicyManager(reloaded)
    }

    @Override
    void removeUserSecurityPolicyManager(String emailAddress) {
        if (!emailAddress) return
        GrailsCache cache = getSecurityPolicyManagerCache()
        GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(emailAddress, GroupBasedUserSecurityPolicyManager)
        if (userSecurityPolicyManager) {
            // Make sure the policy is marked as destroyed to stop concurrent usage
            userSecurityPolicyManager.revoke()
            cache.evict(emailAddress)
        }
    }

    @Override
    UserSecurityPolicyManager refreshAllUserSecurityPolicyManagersBySecurableResource(SecurableResource securableResource, User currentUser) {
        if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResource.class)) {
            refreshAllUserSecurityPolicyManagersByCatalogueUser(securableResource as CatalogueUser)
        } else if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResource.class)) {
            refreshAllUserSecurityPolicyManagersByUserGroup(currentUser, securableResource as UserGroup)
        } else if (Utils.parentClassIsAssignableFromChild(Model, securableResource.class)) {
            refreshAllUserSecurityPolicyManagersByModel(currentUser, securableResource as Model, [] as Set)
        } else {
            throw new ApiNotYetImplementedException('GBSPMS01', "refreshAllUserSecurityPolicyManagersBySecurableResource ${securableResource.class}")
        }
        retrieveUserSecurityPolicyManager(currentUser)
    }

    GroupBasedUserSecurityPolicyManager storeUserSecurityPolicyManager(GroupBasedUserSecurityPolicyManager userSecurityPolicyManager) {
        if (!userSecurityPolicyManager.isLocked()) throw new ApiInternalException('GBSPMS',
                                                                                  'Cannot store on an unlocked GroupBasedUserSecurityPolicyManager')
        GrailsCache cache = getSecurityPolicyManagerCache()
        cache.put(userSecurityPolicyManager.user.emailAddress, userSecurityPolicyManager)
        userSecurityPolicyManager.unlock()
    }

    void ensureUserSecurityPolicyManagerHasCatalogueUser(UserSecurityPolicyManager userSecurityPolicyManager) {
        if (!(userSecurityPolicyManager instanceof GroupBasedUserSecurityPolicyManager)) {
            throw new ApiBadRequestException('USMP03', "Unrecognised class of UserSecurityPolicyManager [${userSecurityPolicyManager.class}]")
        }
        GroupBasedUserSecurityPolicyManager groupBasedUserSecurityPolicyManager = userSecurityPolicyManager as GroupBasedUserSecurityPolicyManager
        groupBasedUserSecurityPolicyManager.lock()
        CatalogueUser user = catalogueUserService.findOrCreateUserFromInterface(groupBasedUserSecurityPolicyManager.getUser())
        groupBasedUserSecurityPolicyManager.ensureCatalogueUser(user)
        groupBasedUserSecurityPolicyManager.unlock()
    }

    GroupBasedUserSecurityPolicyManager refreshUserSecurityPolicyManager(CatalogueUser catalogueUser) {
        GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = getSecurityPolicyManagerCache().get(catalogueUser.emailAddress,
                                                                                                            GroupBasedUserSecurityPolicyManager)
        if (!userSecurityPolicyManager) {
            userSecurityPolicyManager = buildNewUserSecurityPolicyManager(catalogueUser)
        } else {
            refreshUserSecurityPolicyManager(userSecurityPolicyManager.ensureCatalogueUser(catalogueUser))
        }
        storeUserSecurityPolicyManager(userSecurityPolicyManager)
    }

    GroupBasedUserSecurityPolicyManager refreshUserSecurityPolicyManager(GroupBasedUserSecurityPolicyManager userSecurityPolicyManager) {
        userSecurityPolicyManager.lock()
        UserSecurityPolicy updatedPolicy = userSecurityPolicyService.buildUserSecurityPolicy(userSecurityPolicyManager
                                                                                                 .userPolicy
                                                                                                 .inGroups(userSecurityPolicyManager.user.groups))
        userSecurityPolicyManager.withUpdatedUserPolicy(updatedPolicy)
    }

    UserSecurityPolicyManager addUserGroupToUserSecurityPolicyManagers(User currentUser, UserGroup userGroup) {
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()
        Set<String> groupEmailAddresses = userGroup.groupMembers.collect {it.emailAddress}.toSet()
        keys.each {key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            if (userSecurityPolicyManager.user.emailAddress in groupEmailAddresses) {
                userSecurityPolicyManager.lock()
                UserSecurityPolicy updatedPolicy = userSecurityPolicyService.updatePolicyWithAccessInUserGroup(userSecurityPolicyManager.userPolicy,
                                                                                                               userGroup)
                storeUserSecurityPolicyManager(userSecurityPolicyManager.withUpdatedUserPolicy(updatedPolicy))

            }
        }
        retrieveUserSecurityPolicyManager(currentUser)
    }

    UserSecurityPolicyManager removeUserGroupFromUserSecurityPolicyManagers(User currentUser, UserGroup userGroup) {
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()
        keys.each {key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            if (userSecurityPolicyManager.userPolicyIsManagedByGroup(userGroup)) {
                userSecurityPolicyManager.lock()
                UserSecurityPolicy updatedPolicy =
                    userSecurityPolicyService.updatePolicyWithoutAccessInUserGroup(userSecurityPolicyManager.userPolicy, userGroup)
                storeUserSecurityPolicyManager(userSecurityPolicyManager.withUpdatedUserPolicy(updatedPolicy))
            }
        }
        retrieveUserSecurityPolicyManager(currentUser)
    }

    GroupBasedUserSecurityPolicyManager buildNewUserSecurityPolicyManager(CatalogueUser catalogueUser) {
        new GroupBasedUserSecurityPolicyManager()
            .inApplication(grailsApplication)
            .withUserPolicy(userSecurityPolicyService.buildUserSecurityPolicy(catalogueUser, catalogueUser.groups))
    }

    UserSecurityPolicyManager refreshAllUserSecurityPolicyManagersByUserGroup(User currentUser, UserGroup userGroup) {
        log.info('Refreshing all USPMs in user group {}', userGroup.name)
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()

        keys.each {key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            if (userSecurityPolicyManager.userPolicyIsManagedByGroup(userGroup)) {
                storeUserSecurityPolicyManager(refreshUserSecurityPolicyManager(userSecurityPolicyManager))
            }
        }

        retrieveUserSecurityPolicyManager(currentUser)
    }

    UserSecurityPolicyManager refreshAllUserSecurityPolicyManagersBySecurableResourceGroupRole(SecurableResourceGroupRole securableResourceGroupRole, User currentUser) {
        refreshAllUserSecurityPolicyManagersByUserGroup(currentUser, securableResourceGroupRole.userGroup)
    }

    private void refreshAllUserSecurityPolicyManagersByCatalogueUser(CatalogueUser catalogueUser) {
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()

        keys.each {key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            if (userSecurityPolicyManager.userPolicyHasApplicationRoles()) {
                userSecurityPolicyManager.lock()
                UserSecurityPolicy updatedPolicy = userSecurityPolicyService.updatePolicyForAccessToUser(userSecurityPolicyManager.userPolicy,
                                                                                                         catalogueUser)
                storeUserSecurityPolicyManager(userSecurityPolicyManager.withUpdatedUserPolicy(updatedPolicy))
            }
        }
    }

    private UserSecurityPolicyManager refreshAllUserSecurityPolicyManagersByModel(User currentUser, Model model,
                                                                                  Set<String> changedSecurityProperties) {
        log.info('Refreshing all USPMs with model {}', model.label)
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()
        // Currently only designed to handle finalised changes
        // Handles a folder change
        keys.each {key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            userSecurityPolicyManager.lock()
            UserSecurityPolicy updatedPolicy = userSecurityPolicyService.updatePolicyForAccessToModel(userSecurityPolicyManager.userPolicy,
                                                                                                      model, changedSecurityProperties)
            storeUserSecurityPolicyManager(userSecurityPolicyManager.withUpdatedUserPolicy(updatedPolicy))
        }
        retrieveUserSecurityPolicyManager(currentUser)
    }

    private UserSecurityPolicyManager refreshAllUserSecurityPolicyManagersByVersionedFolder(User currentUser, VersionedFolder versionedFolder,
                                                                                            Set<String> changedSecurityProperties) {
        log.info('Refreshing all USPMs with versioned folder {}', versionedFolder.label)
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()
        // Currently only designed to handle finalised changes
        keys.each {key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            userSecurityPolicyManager.lock()
            UserSecurityPolicy updatedPolicy = userSecurityPolicyService.updatePolicyForAccessToVersionedFolder(userSecurityPolicyManager.userPolicy,
                                                                                                                versionedFolder,
                                                                                                                changedSecurityProperties)
            storeUserSecurityPolicyManager(userSecurityPolicyManager.withUpdatedUserPolicy(updatedPolicy))
        }
        retrieveUserSecurityPolicyManager(currentUser)
    }

    @CompileDynamic
    private void processSecurableResourceWithGroups(SecurableResource securableResource, CatalogueUser catalogueUser) {
        if (securableResource.hasProperty('groups')) {
            if (securableResource.groups && securableResource.groups.size() > 0) {

                securableResource.groups.each {group ->
                    UserGroup controlGroup = userGroupService.get(group.groupId)
                    GroupRole role = groupRoleService.get(group.groupRoleId)

                    if (controlGroup && role) {
                        securableResourceGroupRoleService
                            .createAndSaveSecurableResourceGroupRole(securableResource, role, controlGroup, catalogueUser)
                    }
                }
            }
        }
    }

    private void updateBuiltInSecurity(SecurableResource securableResource, Set<String> changedSecurityProperties) {

        // If we're revoking any access we will rebuild the policies so no need to check for allowing accees
        if (
        (!securableResource.readableByEveryone && changedSecurityProperties.contains('readableByEveryone')) ||
        (!securableResource.readableByAuthenticatedUsers && changedSecurityProperties.contains('readableByAuthenticatedUsers'))
        ) {
            revokeReadableBy(securableResource)
        } else if (
        (securableResource.readableByEveryone && changedSecurityProperties.contains('readableByEveryone')) ||
        (securableResource.readableByAuthenticatedUsers && changedSecurityProperties.contains('readableByAuthenticatedUsers'))
        ) {
            allowReadableBy(securableResource, changedSecurityProperties)
        }

    }

    private void allowReadableBy(SecurableResource securableResource, Set<String> changedSecurityProperties) {

        // Build the readable by xxx "true" virtual roles for the securable resource,
        // as we're expanding access we just need all the resources and contents to be iterated as reader access and added to the policy
        VirtualGroupRole readerRole = groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME)


        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as HashSet
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRolesForParents = [] as HashSet

        virtualSecurableResourceGroupRoles.add(virtualSecurableResourceGroupRoleService
                                                   .buildForSecurableResource(securableResource)
                                                   .withAccessLevel(readerRole.groupRole))

        if (Utils.parentClassIsAssignableFromChild(Container, securableResource.class)) {
            ContainerService containerService = containerServices.find {it.handles(securableResource.domainType)}
            // Make sure contents of container are all readable as well
            virtualSecurableResourceGroupRoles.addAll(
                userSecurityPolicyService.buildControlledAccessToContentsOfContainer(securableResource as Container,
                                                                                     containerService,
                                                                                     readerRole.allowedRoles,
                                                                                     null,
                                                                                     readerRole.groupRole
                )
            )
            // Make sure the direct tree of containers are readable as well
            virtualSecurableResourceGroupRolesForParents.addAll(
                containerService.findAllWhereDirectParentOfContainer(securableResource as Container)
                    .collect {container ->
                        virtualSecurableResourceGroupRoleService.buildForSecurableResource(container as Container)
                            .withAccessLevel(readerRole.groupRole)
                    })
        }

        if (Utils.parentClassIsAssignableFromChild(Model, securableResource.class)) {
            // Make sure that the folder tree that contain this model are now readable by everyone so the tree can be built
            // however the contents of these folder are not to be readable
            virtualSecurableResourceGroupRolesForParents.addAll(
                folderService.findAllWhereDirectParentOfModel(securableResource as Model)
                    .collect {folder ->
                        virtualSecurableResourceGroupRoleService.buildForSecurableResource(folder)
                            .withAccessLevel(readerRole.groupRole)
                    })
        }

        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()

        if (changedSecurityProperties.contains('readableByEveryone')) {
            log.debug('Changing readable by everyone to {}', securableResource.readableByEveryone)
            keys.each {key ->
                GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
                userSecurityPolicyManager.lock()
                UserSecurityPolicy updatedPolicy =
                    userSecurityPolicyService.addAccessAndStoreBuiltInSecurity(userSecurityPolicyManager.userPolicy,
                                                                               virtualSecurableResourceGroupRoles,
                                                                               virtualSecurableResourceGroupRolesForParents)
                storeUserSecurityPolicyManager(userSecurityPolicyManager.withUpdatedUserPolicy(updatedPolicy))
            }
        }
        if (changedSecurityProperties.contains('readableByAuthenticatedUsers')) {
            log.debug('Changing readable by authenticated users to {}', securableResource.readableByAuthenticatedUsers)
            keys.each {key ->
                GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
                if (userSecurityPolicyManager.isAuthenticated()) {
                    userSecurityPolicyManager.lock()

                    UserSecurityPolicy updatedPolicy =
                        userSecurityPolicyService.addAccessAndStoreBuiltInSecurity(userSecurityPolicyManager.userPolicy,
                                                                                   virtualSecurableResourceGroupRoles,
                                                                                   virtualSecurableResourceGroupRolesForParents)
                    storeUserSecurityPolicyManager(userSecurityPolicyManager.withUpdatedUserPolicy(updatedPolicy))
                }
            }
        }
    }

    private void revokeReadableBy(SecurableResource securableResource) {
        // Build the readable by xxx "false" virtual roles for the securable resource,
        // as we're limiting access we need to be careful about the roles we'll be removing as inheritance can be overridden with an expanded access
        // therefore easiest to wipe the security and rebuild as we have no idea about other access but only do this is the policy manages the access to the SR

        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()

        keys.each {key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            if (userSecurityPolicyManager.userPolicyManagesAccessToSecurableResource(securableResource)) {
                userSecurityPolicyManager.lock()
                UserSecurityPolicy updatedPolicy = userSecurityPolicyService.buildUserSecurityPolicy(userSecurityPolicyManager.userPolicy)
                storeUserSecurityPolicyManager(userSecurityPolicyManager.withUpdatedUserPolicy(updatedPolicy))
            }
        }


    }

    private void destroyUserSecurityPolicyManagerCache() {
        grailsCacheManager.destroyCache(SECURITY_POLICY_MANAGER_CACHE_KEY)
    }

    private GrailsCache getSecurityPolicyManagerCache() {
        grailsCacheManager.getCache(SECURITY_POLICY_MANAGER_CACHE_KEY) as GrailsCache
    }
}
