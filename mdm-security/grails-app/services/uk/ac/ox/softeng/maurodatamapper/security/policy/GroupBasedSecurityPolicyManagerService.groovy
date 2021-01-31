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
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.FolderService
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.VersionAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
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
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import grails.gorm.transactions.Transactional
import grails.plugin.cache.GrailsCache
import groovy.util.logging.Slf4j
import org.grails.plugin.cache.GrailsCacheManager
import org.springframework.beans.factory.annotation.Autowired

@Transactional
@Slf4j
class GroupBasedSecurityPolicyManagerService implements SecurityPolicyManagerService {

    public static final String SECURITY_POLICY_MANAGER_CACHE_KEY = 'securityPolicyManagerCache'

    SecurableResourceGroupRoleService securableResourceGroupRoleService
    GroupRoleService groupRoleService
    GrailsApplication grailsApplication
    GrailsCacheManager grailsCacheManager

    @Autowired(required = false)
    List<SecurableResourceService> securableResourceServices

    @Autowired(required = false)
    List<ContainerService> containerServices

    @Autowired(required = false)
    List<ModelService> modelServices

    CatalogueUserService catalogueUserService
    UserGroupService userGroupService
    FolderService folderService

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
        securableResourceGroupRoleService.createAndSaveSecurableResourceGroupRole(securableResource, role, controlGroup, catalogueUser)

        /*
        If any groups (UserGroups) have been specified then for each check that both UserGroup and GroupRole can be found from the 
        groupId and groupRoleId provided. If yes then create a SecurableResourceGroupRole
        */
        if (securableResource.hasProperty('groups') && securableResource.groups && securableResource.groups.size() > 0) {
            securableResource.groups.each {
                controlGroup = userGroupService.get(it.groupId)

                //groupRoleService throws a null pointer exception if groupRoleId is not valid
                try {
                    role = groupRoleService.get(it.groupRoleId)
                } catch (ex) {
                    role = null;
                }

                if (controlGroup && role) {
                    securableResourceGroupRoleService.createAndSaveSecurableResourceGroupRole(securableResource, role, controlGroup, catalogueUser)
                }
            }
        }

        refreshUserSecurityPolicyManager(catalogueUser)
    }

    @Override
    UserSecurityPolicyManager removeSecurityForSecurableResource(SecurableResource securableResource, User actor) {
        log.info('Removing security for {}:{}', securableResource.domainType, securableResource.resourceId)

        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()
        keys.each { key ->
            GroupBasedUserSecurityPolicyManager securityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            securityPolicyManager.removeAssignedRoleIf { securableResourceGroupRole ->
                securableResourceGroupRole.securableResourceId == securableResource.resourceId &&
                securableResourceGroupRole.securableResourceDomainType == securableResource.domainType
            }
            securityPolicyManager.removeVirtualRoleIf { securableResourceGroupRole ->
                securableResourceGroupRole.domainId == securableResource.resourceId &&
                securableResourceGroupRole.domainType == securableResource.domainType
            }
            cache.put(key, securityPolicyManager)
        }

        securableResourceGroupRoleService.deleteAllForSecurableResource(securableResource)

        actor ? cache.get(actor.emailAddress, GroupBasedUserSecurityPolicyManager) : null
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

        getSecurityPolicyManagerCache().get(currentUser.emailAddress, GroupBasedUserSecurityPolicyManager)
    }

    void updateBuiltInSecurity(SecurableResource securableResource, Set<String> changedSecurityProperties) {
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()
        VirtualGroupRole readerRole = groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME)
        // Build the readable by everyone virtual roles for the securable resource
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as HashSet
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRolesForParents = [] as HashSet
        virtualSecurableResourceGroupRoles.add(new VirtualSecurableResourceGroupRole()
                                                   .forSecurableResource(securableResource)
                                                   .withAccessLevel(readerRole.groupRole))
        if (Utils.parentClassIsAssignableFromChild(Container, securableResource.class)) {
            ContainerService containerService = containerServices.find { it.handles(securableResource.domainType) }
            // Make sure contents of container are all readable as well
            virtualSecurableResourceGroupRoles.addAll(
                buildControlledAccessToContentsOfContainer(securableResource.getResourceId(),
                                                           containerService,
                                                           readerRole.allowedRoles,
                                                           null,
                                                           readerRole.groupRole
                )
            )
            // Make sure the direct tree of containers are readable as well
            virtualSecurableResourceGroupRolesForParents.addAll(
                containerService.findAllWhereDirectParentOfContainer(securableResource as Container)
                    .collect { container ->
                        new VirtualSecurableResourceGroupRole()
                            .forSecurableResource(container as Container)
                            .withAccessLevel(readerRole.groupRole)
                    })
        }
        if (Utils.parentClassIsAssignableFromChild(Model, securableResource.class)) {
            // Make sure that the folder tree that contain this model are now readable by everyone so the tree can be built
            // however the contents of these folder are not to be readable
            virtualSecurableResourceGroupRolesForParents.addAll(
                folderService.findAllWhereDirectParentOfModel(securableResource as Model)
                    .collect { folder ->
                        new VirtualSecurableResourceGroupRole()
                            .forSecurableResource(folder)
                            .withAccessLevel(readerRole.groupRole)
                    })
        }

        if (changedSecurityProperties.contains('readableByEveryone')) {
            log.debug('Changing readable by everyone to {}', securableResource.readableByEveryone)
            keys.each { key ->
                GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
                updateAndStoreBuiltInSecurity(userSecurityPolicyManager, securableResource.readableByEveryone,
                                              virtualSecurableResourceGroupRoles, virtualSecurableResourceGroupRolesForParents)
            }

        }
        if (changedSecurityProperties.contains('readableByAuthenticatedUsers')) {
            log.debug('Changing readable by authenticated users to {}', securableResource.readableByAuthenticatedUsers)
            keys.each { key ->
                GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
                if (userSecurityPolicyManager.isAuthenticated()) {
                    updateAndStoreBuiltInSecurity(userSecurityPolicyManager, securableResource.readableByAuthenticatedUsers,
                                                  virtualSecurableResourceGroupRoles, virtualSecurableResourceGroupRolesForParents)
                }
            }
        }
    }

    @Override
    GroupBasedUserSecurityPolicyManager retrieveUserSecurityPolicyManager(String userEmailAddress) {
        GrailsCache cache = getSecurityPolicyManagerCache()
        GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(userEmailAddress, GroupBasedUserSecurityPolicyManager)
        // Make sure the user object is the latest and attached to the session
        userSecurityPolicyManager?.forUser(catalogueUserService.findByEmailAddress(userEmailAddress))
    }

    @Override
    UserSecurityPolicyManager reloadUserSecurityPolicyManager(String userEmailAddress) {
        removeUserSecurityPolicyManager(userEmailAddress)
        UserSecurityPolicyManager reloaded = buildUserSecurityPolicyManager(catalogueUserService.findByEmailAddress(userEmailAddress))
        storeUserSecurityPolicyManager(reloaded)
    }

    @Override
    void removeUserSecurityPolicyManager(String emailAddress) {
        GrailsCache cache = getSecurityPolicyManagerCache()
        // grails cache is usually concurrent and as such a check for "does exist" causes NPE if the value doesnt exist
        List keys = new ArrayList<>(cache.getAllKeys())
        if (emailAddress in keys) cache.evict(emailAddress)
    }

    @Override
    UserSecurityPolicyManager refreshAllUserSecurityPolicyManagersBySecurableResource(SecurableResource securableResource, User currentUser) {
        if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResource.class)) {
            refreshAllUserSecurityPolicyManagersByCatalogueUser(securableResource as CatalogueUser)
        } else if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResource.class)) {
            refreshAllUserSecurityPolicyManagersByUserGroup(currentUser, securableResource as UserGroup)
        } else if (Utils.parentClassIsAssignableFromChild(Model, securableResource.class)) {
            refreshAllUserSecurityPolicyManagersByModel(currentUser, securableResource as Model, [])
        } else {
            throw new ApiNotYetImplementedException('GBSPMS01', "refreshAllUserSecurityPolicyManagersBySecurableResource ${securableResource.class}")
        }
        getSecurityPolicyManagerCache().get(currentUser.emailAddress, GroupBasedUserSecurityPolicyManager)
    }

    GroupBasedUserSecurityPolicyManager storeUserSecurityPolicyManager(GroupBasedUserSecurityPolicyManager userSecurityPolicyManager) {
        GrailsCache cache = getSecurityPolicyManagerCache()
        cache.put(userSecurityPolicyManager.user.emailAddress, userSecurityPolicyManager)
        userSecurityPolicyManager
    }

    void ensureUserSecurityPolicyManagerHasCatalogueUser(UserSecurityPolicyManager userSecurityPolicyManager) {
        if (!(userSecurityPolicyManager instanceof GroupBasedUserSecurityPolicyManager)) {
            throw new ApiBadRequestException('USMP03', "Unrecognised class of UserSecurityPoloicyManager [${userSecurityPolicyManager.class}]")
        }
        CatalogueUser user = catalogueUserService.findOrCreateUserFromInterface(userSecurityPolicyManager.getUser())
        (userSecurityPolicyManager as GroupBasedUserSecurityPolicyManager).forUser(user)
    }

    GroupBasedUserSecurityPolicyManager refreshUserSecurityPolicyManager(CatalogueUser catalogueUser) {
        GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = getSecurityPolicyManagerCache().get(catalogueUser.emailAddress,
                                                                                                            GroupBasedUserSecurityPolicyManager)
        if (!userSecurityPolicyManager) {
            return storeUserSecurityPolicyManager(buildUserSecurityPolicyManager(catalogueUser))
        }
        userSecurityPolicyManager.forUser(catalogueUser).inGroups(catalogueUser.groups)
        refreshUserSecurityPolicyManager(userSecurityPolicyManager)
    }

    GroupBasedUserSecurityPolicyManager refreshUserSecurityPolicyManager(GroupBasedUserSecurityPolicyManager userSecurityPolicyManager) {
        userSecurityPolicyManager.hasNoAccess()
        storeUserSecurityPolicyManager(buildUserSecurityPolicyManager(userSecurityPolicyManager))
    }

    UserSecurityPolicyManager addUserGroupToUserSecurityPolicyManagers(User currentUser, UserGroup userGroup) {
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()
        Set<String> groupEmailAddresses = userGroup.groupMembers.collect { it.emailAddress }
        keys.each { key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            if (userSecurityPolicyManager.user.emailAddress in groupEmailAddresses) {
                userSecurityPolicyManager.userGroups << userGroup
                storeUserSecurityPolicyManager(buildUserSecurityPolicyManager(userSecurityPolicyManager,
                                                                              userSecurityPolicyManager.userGroups,
                                                                              userSecurityPolicyManager.applicationPermittedRoles,
                                                                              userSecurityPolicyManager.virtualSecurableResourceGroupRoles))
            }
        }
        cache.get(currentUser.emailAddress, GroupBasedUserSecurityPolicyManager)
    }

    UserSecurityPolicyManager removeUserGroupFromUserSecurityPolicyManagers(User currentUser, UserGroup userGroup) {
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()
        keys.each { key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            if (userGroup.id in userSecurityPolicyManager.userGroups*.id) {
                userSecurityPolicyManager.userGroups.remove(userGroup)
                storeUserSecurityPolicyManager(buildUserSecurityPolicyManager(userSecurityPolicyManager,
                                                                              userSecurityPolicyManager.userGroups,
                                                                              userSecurityPolicyManager.applicationPermittedRoles,
                                                                              userSecurityPolicyManager.virtualSecurableResourceGroupRoles))
            }
        }
        cache.get(currentUser.emailAddress, GroupBasedUserSecurityPolicyManager)
    }

    UserSecurityPolicyManager refreshAllUserSecurityPolicyManagersByUserGroup(User currentUser, UserGroup userGroup) {
        log.info('Refreshing all USPMs in user group {}', userGroup.name)
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()

        keys.each { key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            if (userGroup.id in userSecurityPolicyManager.userGroups*.id) {
                storeUserSecurityPolicyManager(refreshUserSecurityPolicyManager(userSecurityPolicyManager))
            }
        }

        cache.get(currentUser.emailAddress, GroupBasedUserSecurityPolicyManager)
    }

    void refreshAllUserSecurityPolicyManagersByCatalogueUser(CatalogueUser catalogueUser) {
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()

        keys.each { key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)
            if (userSecurityPolicyManager.applicationPermittedRoles) {
                GroupRole highestRole = userSecurityPolicyManager.highestApplicationLevelAccess
                VirtualGroupRole virtualGroupRole = groupRoleService.getFromCache(highestRole.name)
                Set<VirtualSecurableResourceGroupRole> additionalRoles = buildIndividualCatalogueUserVirtualRoles(catalogueUser, virtualGroupRole)
                storeUserSecurityPolicyManager(userSecurityPolicyManager.includeVirtualRoles(additionalRoles))
            }
        }
    }

    UserSecurityPolicyManager refreshAllUserSecurityPolicyManagersByModel(User currentUser, Model model, Set<String> changedSecurityProperties) {
        log.info('Refreshing all USPMs with model {}', model.label)
        GrailsCache cache = getSecurityPolicyManagerCache()
        Collection<Object> keys = cache.getAllKeys()
        // Currently only designed to handle finalised changes
        // Handles a folder change
        keys.each { key ->
            GroupBasedUserSecurityPolicyManager userSecurityPolicyManager = cache.get(key, GroupBasedUserSecurityPolicyManager)

            if ('finalised' in changedSecurityProperties) {
                userSecurityPolicyManager.virtualSecurableResourceGroupRoles.each { virtualRole ->
                    if (virtualRole.domainType == model.domainType && virtualRole.domainId == model.id) {
                        virtualRole.asFinalisedModel(model.finalised)
                    }
                }
                userSecurityPolicyManager.securableResourceGroupRoles.each { securableRole ->
                    if (securableRole.securableResourceDomainType == model.domainType && securableRole.securableResourceId == model.id) {
                        securableRole = securableResourceGroupRoleService.updatedFinalisedState(securableRole, model.finalised)
                    }
                }

            }

            if ('folder' in changedSecurityProperties) {
                // Identify new folder's max security level
                GroupRole folderMaxPermission = userSecurityPolicyManager.findHighestAccessToSecurableResource(Folder.simpleName, model.folder.id)
                GroupRole modelMaxAssignedPermission = userSecurityPolicyManager.findHighestAssignedAccessToSecurableResource(model.getDomainType(),
                                                                                                                              model.getResourceId())
                GroupRole modelMaxVirtualPermission = userSecurityPolicyManager.findHighestAccessToSecurableResource(model.getDomainType(),
                                                                                                                     model.getResourceId())
                // If no folder access and no assigned model permission then revoke all model access
                if (!folderMaxPermission && !modelMaxAssignedPermission) {
                    userSecurityPolicyManager.removeVirtualRoleIf { virtualRole ->
                        virtualRole.domainType == model.domainType && virtualRole.domainId == model.id
                    }
                } else {
                    // The access level is controlled from multiple places and as there could be more than one group providing access so lets just
                    // rebuild
                    userSecurityPolicyManager = buildUserSecurityPolicyManager(userSecurityPolicyManager.hasNoAccess())
                }

            }


            cache.put(key, userSecurityPolicyManager)
        }
        cache.get(currentUser.emailAddress, GroupBasedUserSecurityPolicyManager)
    }

    UserSecurityPolicyManager refreshAllUserSecurityPolicyManagersBySecurableResourceGroupRole(
        SecurableResourceGroupRole securableResourceGroupRole,
        User currentUser) {
        refreshAllUserSecurityPolicyManagersByUserGroup(currentUser, securableResourceGroupRole.userGroup)
    }

    void destroyUserSecurityPolicyManagerCache() {
        grailsCacheManager.destroyCache(SECURITY_POLICY_MANAGER_CACHE_KEY)
    }

    GrailsCache getSecurityPolicyManagerCache() {
        grailsCacheManager.getCache(SECURITY_POLICY_MANAGER_CACHE_KEY) as GrailsCache
    }

    GroupBasedUserSecurityPolicyManager buildUserSecurityPolicyManager(CatalogueUser catalogueUser) {
        buildUserSecurityPolicyManager(new GroupBasedUserSecurityPolicyManager()
                                           .inApplication(grailsApplication)
                                           .forUser(catalogueUser)
                                           .inGroups(catalogueUser.groups))
    }

    GroupBasedUserSecurityPolicyManager buildUserSecurityPolicyManager(GroupBasedUserSecurityPolicyManager userSecurityPolicyManager) {
        Set<UserGroup> userGroups = userSecurityPolicyManager.userGroups

        Set<VirtualSecurableResourceGroupRole> personalUserRoles = buildIndividualCatalogueUserVirtualRoles(
            userSecurityPolicyManager.user, groupRoleService.getFromCache(GroupRole.USER_ADMIN_ROLE_NAME)
        )

        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles =
            buildInternalSecurity(userSecurityPolicyManager.isAuthenticated())

        // Add the actual user with full permissions for themselves
        virtualSecurableResourceGroupRoles.addAll(personalUserRoles)

        // If no usergroups then the user has no permissions apart from their own user
        if (!userGroups) {
            return userSecurityPolicyManager
                .withVirtualRoles(personalUserRoles)
                .includeVirtualRoles(virtualSecurableResourceGroupRoles)
        }
        Set<GroupRole> assignedApplicationGroupRoles = userGroups.collect { it.applicationGroupRole }.findAll().toSet()

        buildUserSecurityPolicyManager(userSecurityPolicyManager,
                                       userGroups, assignedApplicationGroupRoles, virtualSecurableResourceGroupRoles)
    }

    GroupBasedUserSecurityPolicyManager buildUserSecurityPolicyManager(GroupBasedUserSecurityPolicyManager userSecurityPolicyManager,
                                                                       Set<UserGroup> userGroups, Set<GroupRole> assignedApplicationGroupRoles,
                                                                       Set<VirtualSecurableResourceGroupRole>
                                                                           virtualSecurableResourceGroupRoles) {


        Set<GroupRole> inheritedApplicationGroupRoles = new HashSet<>()

        VirtualGroupRole fullSecureableResourceAccessRole = null

        // Build the full list of application level roles, this will contain the assigned roles and all their children
        assignedApplicationGroupRoles.each { gr ->
            VirtualGroupRole virtualGroupRole = groupRoleService.getFromCache(gr.name)

            // Application admin and site admin are the roles we know about, store the highest level present as we need it later to define
            // catalogue item access
            if (virtualGroupRole.allowedRoles.any { it.name == GroupRole.SITE_ADMIN_ROLE_NAME }) {
                // If the list of allowed roles is smaller then keep as the highest level of access
                if (!fullSecureableResourceAccessRole ||
                    fullSecureableResourceAccessRole.allowedRoles.size() > virtualGroupRole.allowedRoles.size()) {
                    fullSecureableResourceAccessRole = virtualGroupRole
                }
            }

            // Add all allowed roles into the inherited set
            inheritedApplicationGroupRoles.addAll(virtualGroupRole.allowedRoles)
        }

        // Get all the assigned securable resource group roles
        List<SecurableResourceGroupRole> securableResourceGroupRoles = securableResourceGroupRoleService.findAllByUserGroupIds(
            userGroups.collect { it.id }
        )

        // If user has application admin rights then they have all rights to all securable resources
        if (fullSecureableResourceAccessRole) {
            virtualSecurableResourceGroupRoles.addAll(buildFullAccessToAllSecurableResources(fullSecureableResourceAccessRole))
        } else {
            // Otherwise use the assigned roles to define access
            virtualSecurableResourceGroupRoles.addAll(buildControlledAccessToSecurableResources(securableResourceGroupRoles))
        }

        // If any container admin privileges then we need to make sure the container group admin role is added to the application level
        if (virtualSecurableResourceGroupRoles.any { it.groupRole.name == GroupRole.CONTAINER_ADMIN_ROLE_NAME }) {
            inheritedApplicationGroupRoles.add(groupRoleService.getFromCache(GroupRole.CONTAINER_GROUP_ADMIN_ROLE_NAME).groupRole)
        }

        // Add all users and groups access (only valid for application roles)
        virtualSecurableResourceGroupRoles.addAll(buildCatalogueUserVirtualRoles(inheritedApplicationGroupRoles))
        virtualSecurableResourceGroupRoles.addAll(buildUserGroupVirtualRoles(inheritedApplicationGroupRoles))

        userSecurityPolicyManager
            .withApplicationRoles(inheritedApplicationGroupRoles)
            .withSecurableRoles(securableResourceGroupRoles)
            .withVirtualRoles(virtualSecurableResourceGroupRoles)
    }

    private Set<VirtualSecurableResourceGroupRole> buildIndividualCatalogueUserVirtualRoles(CatalogueUser catalogueUser,
                                                                                            VirtualGroupRole applicationRole) {

        applicationRole.allowedRoles.collect { iur ->
            new VirtualSecurableResourceGroupRole()
                .forSecurableResource(catalogueUser)
                .definedByAccessLevel(applicationRole.groupRole)
                .withAccessLevel(iur)
        }
    }

    Set<VirtualSecurableResourceGroupRole> buildCatalogueUserVirtualRoles(Set<GroupRole> applicationLevelRoles) {
        if (!applicationLevelRoles) return new HashSet<VirtualSecurableResourceGroupRole>()

        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as Set

        GroupRole highestRole = applicationLevelRoles.sort { it.depth }.first()
        VirtualGroupRole virtualGroupRole = groupRoleService.getFromCache(highestRole.name)
        Set<GroupRole> inheritedUserRoles = virtualGroupRole.allowedRoles
        List<CatalogueUser> users = catalogueUserService.list()

        users.each { user ->
            virtualSecurableResourceGroupRoles.addAll(
                inheritedUserRoles.collect { iur ->
                    new VirtualSecurableResourceGroupRole()
                        .forSecurableResource(user)
                        .definedByAccessLevel(highestRole)
                        .withAccessLevel(iur)
                }
            )
        }
        virtualSecurableResourceGroupRoles
    }

    Set<VirtualSecurableResourceGroupRole> buildUserGroupVirtualRoles(Set<GroupRole> applicationLevelRoles) {
        if (!applicationLevelRoles) return new HashSet<VirtualSecurableResourceGroupRole>()

        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as Set

        GroupRole highestRole = applicationLevelRoles.sort { it.depth }.first()
        VirtualGroupRole virtualGroupRole = groupRoleService.getFromCache(highestRole.name)
        Set<GroupRole> inheritedUserRoles = virtualGroupRole.allowedRoles
        List<UserGroup> userGroups = userGroupService.list()

        userGroups.each { user ->
            virtualSecurableResourceGroupRoles.addAll(
                inheritedUserRoles.collect { iur ->
                    new VirtualSecurableResourceGroupRole()
                        .forSecurableResource(user)
                        .definedByAccessLevel(highestRole)
                        .withAccessLevel(iur)
                }
            )
        }
        virtualSecurableResourceGroupRoles
    }

    private Set<VirtualSecurableResourceGroupRole> buildFullAccessToAllSecurableResources(VirtualGroupRole accessRole) {
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as Set
        Set<GroupRole> inheritedContainerRoles = groupRoleService.getFromCache(GroupRole.CONTAINER_ADMIN_ROLE_NAME).allowedRoles
        // Don't bother with usergroups or users as these will be implicitly defined by the other roles under application_admin
        securableResourceServices.findAll { !it.handles(UserGroup) && !it.handles(CatalogueUser) }.each { service ->

            List<SecurableResource> resources = service.list() as List<SecurableResource>
            resources.each { r ->
                virtualSecurableResourceGroupRoles.addAll(
                    inheritedContainerRoles.collect { igr ->
                        new VirtualSecurableResourceGroupRole()
                            .forSecurableResource(r)
                            .definedByAccessLevel(accessRole.groupRole)
                            .withAccessLevel(igr)
                    }
                )
            }

        }
        virtualSecurableResourceGroupRoles
    }

    private Set<VirtualSecurableResourceGroupRole> buildControlledAccessToSecurableResources(
        List<SecurableResourceGroupRole> securableResourceGroupRoles) {

        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as Set
        securableResourceGroupRoles.each { sgr ->
            // Setup all the direct virtual roles
            Set<GroupRole> allowedRoles = groupRoleService.getFromCache(sgr.groupRole.name).allowedRoles
            virtualSecurableResourceGroupRoles.addAll(
                allowedRoles.collect { igr ->
                    new VirtualSecurableResourceGroupRole()
                        .fromSecurableResourceGroupRole(sgr)
                        .withAccessLevel(igr)
                }
            )

            // If we're securing a container then we need to create virtual roles for all its contents
            ContainerService containerService = containerServices.find { it.handles(sgr.securableResourceDomainType) }
            if (containerService) {
                virtualSecurableResourceGroupRoles.addAll(buildControlledAccessToContentsOfContainer(sgr.securableResourceId,
                                                                                                     containerService,
                                                                                                     allowedRoles,
                                                                                                     sgr.userGroup,
                                                                                                     sgr.groupRole))

            }
        }


        virtualSecurableResourceGroupRoles
    }

    private Set<VirtualSecurableResourceGroupRole> buildInternalSecurity(boolean isAuthenticatedUser) {
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as Set
        // Setup readable by everyone
        virtualSecurableResourceGroupRoles.addAll(buildReadableByEveryone())

        // Setup readable by authenticated users
        if (isAuthenticatedUser) {
            virtualSecurableResourceGroupRoles.addAll(buildReadableByAuthenticatedUsers())
        }
        virtualSecurableResourceGroupRoles
    }

    private Set<VirtualSecurableResourceGroupRole> buildReadableByEveryone() {
        VirtualGroupRole readerRole = groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME)
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as HashSet
        containerServices.each { service ->
            List<Container> containers = service.findAllReadableByEveryone() as List<Container>
            containers.each { container ->
                // Add reader access on each container
                virtualSecurableResourceGroupRoles.add(new VirtualSecurableResourceGroupRole()
                                                           .forSecurableResource(container)
                                                           .withAccessLevel(readerRole.groupRole))
                // Make sure contents of container are all readable as well
                virtualSecurableResourceGroupRoles.addAll(
                    buildControlledAccessToContentsOfContainer(container.id,
                                                               service,
                                                               readerRole.allowedRoles,
                                                               null,
                                                               readerRole.groupRole
                    )
                )
            }
        }
        modelServices.each { service ->
            List<Model> models = service.findAllReadableByEveryone() as List<Model>
            models.each { model ->
                // Add reader access on each container
                virtualSecurableResourceGroupRoles.add(new VirtualSecurableResourceGroupRole()
                                                           .forSecurableResource(model)
                                                           .withAccessLevel(readerRole.groupRole))

                // Need to make sure the owning folders are readable
                virtualSecurableResourceGroupRoles.addAll(buildReadableContainerInheritance(model.folder,
                                                                                            readerRole.allowedRoles,
                                                                                            null,
                                                                                            readerRole.groupRole))
            }
        }
        virtualSecurableResourceGroupRoles
    }

    private Set<VirtualSecurableResourceGroupRole> buildReadableByAuthenticatedUsers() {
        VirtualGroupRole readerRole = groupRoleService.getFromCache(GroupRole.READER_ROLE_NAME)
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as HashSet
        containerServices.each { service ->
            List<Container> containers = service.findAllReadableByAuthenticatedUsers() as List<Container>
            containers.each { container ->
                // Add reader access on each container
                virtualSecurableResourceGroupRoles.add(new VirtualSecurableResourceGroupRole()
                                                           .forSecurableResource(container)
                                                           .withAccessLevel(readerRole.groupRole))
                // Make sure contents of container are all readable as well
                virtualSecurableResourceGroupRoles.addAll(
                    buildControlledAccessToContentsOfContainer(container.id,
                                                               service,
                                                               readerRole.allowedRoles,
                                                               null,
                                                               readerRole.groupRole
                    )
                )
            }
        }
        modelServices.each { service ->
            List<Model> models = service.findAllReadableByAuthenticatedUsers() as List<Model>
            models.each { model ->
                // Add reader access on each container
                virtualSecurableResourceGroupRoles.add(new VirtualSecurableResourceGroupRole()
                                                           .forSecurableResource(model)
                                                           .withAccessLevel(readerRole.groupRole))

                // Need to make sure the owning folders are readable
                virtualSecurableResourceGroupRoles.addAll(buildReadableContainerInheritance(model.folder,
                                                                                            readerRole.allowedRoles,
                                                                                            null,
                                                                                            readerRole.groupRole))
            }
        }
        virtualSecurableResourceGroupRoles
    }

    private Set<VirtualSecurableResourceGroupRole> buildReadableContainerInheritance(Container container, Set<GroupRole> accessRoles,
                                                                                     UserGroup userGroup,
                                                                                     GroupRole appliedGroupRole) {

        if (!container) return [] as HashSet

        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = accessRoles.collect { igr ->
            new VirtualSecurableResourceGroupRole()
                .forSecurableResource(container)
                .withAccessLevel(igr)
                .definedByGroup(userGroup)
                .definedByAccessLevel(appliedGroupRole)
        }.toSet()

        // Build parents
        if (container.depth != 0) {

            List<UUID> ids = container.path.split('/').toList().findAll().collect { Utils.toUuid(it) }

            ContainerService containerService = containerServices.find { it.handles(container.domainType) }
            containerService.getAll(ids).each { c ->
                virtualSecurableResourceGroupRoles.addAll(accessRoles.collect { igr ->
                    new VirtualSecurableResourceGroupRole()
                        .forSecurableResource(c)
                        .withAccessLevel(igr)
                        .definedByGroup(userGroup)
                        .definedByAccessLevel(appliedGroupRole)
                })
            }


        }


        virtualSecurableResourceGroupRoles
    }

    private Set<VirtualSecurableResourceGroupRole> buildControlledAccessToContentsOfContainer(UUID containerId,
                                                                                              ContainerService containerService,
                                                                                              Set<GroupRole> accessRoles,
                                                                                              UserGroup userGroup,
                                                                                              GroupRole appliedGroupRole) {
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles

        // Load model access controls for all non-virtual containers
        if (!containerService.isContainerVirtual()) {
            virtualSecurableResourceGroupRoles = buildControlledAccessToModelsInContainer(containerId,
                                                                                          accessRoles,
                                                                                          userGroup,
                                                                                          appliedGroupRole)
        } else virtualSecurableResourceGroupRoles = [] as HashSet

        // Load sub containers
        List<Container> subContainers = containerService.findAllContainersInside(containerId) as List<Container>
        subContainers.each { sc ->
            virtualSecurableResourceGroupRoles.addAll(
                accessRoles.collect { igr ->
                    new VirtualSecurableResourceGroupRole()
                        .forSecurableResource(sc)
                        .withAccessLevel(igr)
                        .definedByGroup(userGroup)
                        .definedByAccessLevel(appliedGroupRole)
                }
            )

            if (!containerService.isContainerVirtual()) {
                // Load models inside container if its non-virtual
                virtualSecurableResourceGroupRoles.
                    addAll(buildControlledAccessToModelsInContainer(sc.resourceId, accessRoles, userGroup, appliedGroupRole))
            }
        }


        virtualSecurableResourceGroupRoles
    }


    private Set<VirtualSecurableResourceGroupRole> buildControlledAccessToModelsInContainer(UUID containerId,
                                                                                            Set<GroupRole> accessRoles,
                                                                                            UserGroup userGroup,
                                                                                            GroupRole appliedGroupRole) {
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as Set

        // Load models
        modelServices.each { modelService ->

            List<Model> models = modelService.findAllByContainerId(containerId) as List<Model>
            models.each { Model m ->
                virtualSecurableResourceGroupRoles.addAll(
                    accessRoles.collect { igr ->
                        new VirtualSecurableResourceGroupRole()
                            .forSecurableResource(m)
                            .withAccessLevel(igr)
                            .definedByGroup(userGroup)
                            .definedByAccessLevel(appliedGroupRole)
                            .asFinalisedModel(m.finalised)
                            .withModelCanBeFinalised(m.branchName == VersionAwareConstraints.DEFAULT_BRANCH_NAME)
                    }
                )
            }
        }

        virtualSecurableResourceGroupRoles
    }

    void updateAndStoreBuiltInSecurity(GroupBasedUserSecurityPolicyManager userSecurityPolicyManager, Boolean addAccess,
                                       Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles,
                                       Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRolesForParents) {
        if (addAccess) {
            // If readable then add to all policy managers
            userSecurityPolicyManager.includeVirtualRoles(virtualSecurableResourceGroupRoles)
            userSecurityPolicyManager.includeVirtualRoles(virtualSecurableResourceGroupRolesForParents)
        } else {
            // Remove from all policy managers we can do a perfect match to only remove the valid roles
            userSecurityPolicyManager.removeVirtualRoleIf { virtualSecurableResourceGroupRole ->
                virtualSecurableResourceGroupRole in virtualSecurableResourceGroupRoles
            }
            // Clean up parent tree of access
            Set<VirtualSecurableResourceGroupRole> allRolesToRemove = virtualSecurableResourceGroupRolesForParents.findAll {
                it in userSecurityPolicyManager.virtualSecurableResourceGroupRoles
            }
            removeRolesWithNoRequiredAccess(userSecurityPolicyManager, allRolesToRemove)
        }
        storeUserSecurityPolicyManager(userSecurityPolicyManager)
    }

    void removeRolesWithNoRequiredAccess(GroupBasedUserSecurityPolicyManager userSecurityPolicyManager,
                                         Set<VirtualSecurableResourceGroupRole> allRolesToRemove) {
        if (!allRolesToRemove) return

        List<UUID> requiredAccessIds = userSecurityPolicyManager.virtualSecurableResourceGroupRoles*.getDependsOnDomainIdAccess().findAll()
        Set<VirtualSecurableResourceGroupRole> canBeRemoved = allRolesToRemove.findAll {
            !(it.domainId in requiredAccessIds)
        }
        if (canBeRemoved) {
            userSecurityPolicyManager.removeVirtualRoles(canBeRemoved)
            allRolesToRemove.removeAll(canBeRemoved)
            removeRolesWithNoRequiredAccess(userSecurityPolicyManager, allRolesToRemove)
        }
    }
}
