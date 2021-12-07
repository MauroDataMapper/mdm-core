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
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.ContainerService
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUserService
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.UserGroupService
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
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired

@Transactional
@Slf4j
@CompileStatic
class UserSecurityPolicyService {

    SecurableResourceGroupRoleService securableResourceGroupRoleService
    GroupRoleService groupRoleService
    VirtualSecurableResourceGroupRoleService virtualSecurableResourceGroupRoleService
    CatalogueUserService catalogueUserService
    UserGroupService userGroupService

    @Autowired(required = false)
    List<SecurableResourceService> securableResourceServices = []

    @Autowired(required = false)
    List<ContainerService> containerServices = []

    @Autowired(required = false)
    List<ModelService> modelServices = []

    @Autowired
    GrailsApplication grailsApplication

    UserSecurityPolicy buildUserSecurityPolicy(CatalogueUser catalogueUser, Set<UserGroup> userGroups) {
        int maxLockTime = grailsApplication.config.getProperty('maurodatamapper.security.max.lock.time', Integer, 5)
        buildUserSecurityPolicy UserSecurityPolicy
                                    .builder()
                                    .forUser(catalogueUser)
                                    .inGroups(userGroups)
                                    .withMaxLockTime(maxLockTime)
    }

    UserSecurityPolicy buildUserSecurityPolicy(UserSecurityPolicy userSecurityPolicy) {
        if (!userSecurityPolicy.isLocked()) throw new ApiInternalException('GBSPMS', 'Cannot build on an unlocked UserPolicy')
        log.debug('Building new UserSecurityPolicy for {}', userSecurityPolicy.user)
        userSecurityPolicy.setNoAccess()

        Set<VirtualSecurableResourceGroupRole> personalUserRoles = buildIndividualCatalogueUserVirtualRoles(
            userSecurityPolicy.user, groupRoleService.getFromCache(GroupRole.USER_ADMIN_ROLE_NAME)
        )

        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = buildInternalSecurity(userSecurityPolicy.isAuthenticated())

        // Add the actual user with full permissions for themselves
        virtualSecurableResourceGroupRoles.addAll(personalUserRoles)

        // If no usergroups then the user has no permissions apart from their own user
        if (!userSecurityPolicy.hasUserGroups()) {
            return userSecurityPolicy
                .withVirtualRoles(personalUserRoles)
                .includeVirtualRoles(virtualSecurableResourceGroupRoles)
        }

        buildUserSecurityPolicyForContent(userSecurityPolicy,
                                          userSecurityPolicy.getAssignedUserGroupApplicationRoles(),
                                          virtualSecurableResourceGroupRoles)
    }

    UserSecurityPolicy updatePolicyWithAccessInUserGroup(UserSecurityPolicy userSecurityPolicy, UserGroup userGroup) {
        if (!userSecurityPolicy.isLocked()) throw new ApiInternalException('GBSPMS', 'Cannot update on an unlocked UserPolicy')
        log.debug('Updating UserSecurityPolicy for access with UserGroup')
        userSecurityPolicy.updateWithUserGroup(userGroup)
        buildUserSecurityPolicyForContent(userSecurityPolicy,
                                          userSecurityPolicy.getApplicationPermittedRolesForBuilding(),
                                          userSecurityPolicy.getVirtualSecurableResourceGroupRolesForBuilding())
    }

    UserSecurityPolicy updatePolicyWithoutAccessInUserGroup(UserSecurityPolicy userSecurityPolicy, UserGroup userGroup) {
        if (!userSecurityPolicy.isLocked()) throw new ApiInternalException('GBSPMS', 'Cannot update on an unlocked UserPolicy')
        log.debug('Updating UserSecurityPolicy for access with UserGroup')
        userSecurityPolicy.updateWithoutUserGroup(userGroup)
        buildUserSecurityPolicyForContent(userSecurityPolicy,
                                          userSecurityPolicy.getApplicationPermittedRolesForBuilding(),
                                          userSecurityPolicy.getVirtualSecurableResourceGroupRolesForBuilding())
    }

    UserSecurityPolicy updatePolicyForAccessToUser(UserSecurityPolicy userSecurityPolicy, CatalogueUser userToAccess) {
        if (!userSecurityPolicy.isLocked()) throw new ApiInternalException('GBSPMS', 'Cannot update on an unlocked UserPolicy')
        log.debug('Updating UserSecurityPolicy to access new user')
        GroupRole highestRole = userSecurityPolicy.highestApplicationLevelAccess
        VirtualGroupRole virtualGroupRole = groupRoleService.getFromCache(highestRole.name)
        Set<VirtualSecurableResourceGroupRole> additionalRoles = buildIndividualCatalogueUserVirtualRoles(userToAccess, virtualGroupRole)
        userSecurityPolicy.includeVirtualRoles(additionalRoles)
    }

    UserSecurityPolicy updatePolicyToRemoveSecurableResource(UserSecurityPolicy userSecurityPolicy, SecurableResource securableResource) {
        if (!userSecurityPolicy.isLocked()) throw new ApiInternalException('GBSPMS', 'Cannot update on an unlocked UserPolicy')
        log.debug('Updating UserSecurityPolicy to remove SecurableResource')
        userSecurityPolicy.removeAssignedRoleIf {securableResourceGroupRole ->
            securableResourceGroupRole.securableResourceId == securableResource.resourceId &&
            securableResourceGroupRole.securableResourceDomainType == securableResource.domainType
        }
        userSecurityPolicy.removeVirtualRoleIf {securableResourceGroupRole ->
            securableResourceGroupRole.domainId == securableResource.resourceId &&
            securableResourceGroupRole.domainType == securableResource.domainType
        }
    }

    UserSecurityPolicy updatePolicyForAccessToModel(UserSecurityPolicy userSecurityPolicy, Model model,
                                                    Set<String> changedSecurityProperties) {
        if (!userSecurityPolicy.isLocked()) throw new ApiInternalException('GBSPMS', 'Cannot update on an unlocked UserPolicy')
        log.debug('Updating UserSecurityPolicy to access Model')
        if ('finalised' in changedSecurityProperties) {
            userSecurityPolicy.getVirtualSecurableResourceGroupRolesForBuilding().each {virtualRole ->
                if (virtualRole.domainType == model.domainType && virtualRole.domainId == model.id) {
                    virtualRole.asFinalised(model.finalised)
                }
            }
        }

        if ('folder' in changedSecurityProperties) {
            // Identify new folder's max security level
            GroupRole folderMaxPermission = userSecurityPolicy.findHighestAccessToSecurableResource(Folder.simpleName, model.folder.id)
            GroupRole modelMaxAssignedPermission = userSecurityPolicy.findHighestAssignedAccessToSecurableResource(model.getDomainType(),
                                                                                                                   model.getResourceId())

            // If no folder access and no assigned model permission then revoke all model access
            if (!folderMaxPermission && !modelMaxAssignedPermission) {
                userSecurityPolicy.removeVirtualRoleIf {virtualRole ->
                    virtualRole.domainType == model.domainType && virtualRole.domainId == model.id
                }
            } else {
                // The access level is controlled from multiple places and as there could be more than one group providing access so lets just
                // rebuild
                userSecurityPolicy = buildUserSecurityPolicy(userSecurityPolicy.setNoAccess())
            }

        }
        userSecurityPolicy
    }

    UserSecurityPolicy updatePolicyForAccessToVersionedFolder(UserSecurityPolicy userSecurityPolicy, VersionedFolder versionedFolder,
                                                              Set<String> changedSecurityProperties) {
        if (!userSecurityPolicy.isLocked()) throw new ApiInternalException('GBSPMS', 'Cannot update on an unlocked UserPolicy')
        log.debug('Updating UserSecurityPolicy to access VersionedFolder')
        if ('finalised' in changedSecurityProperties) {
            userSecurityPolicy.getVirtualSecurableResourceGroupRolesForBuilding().each {virtualRole ->
                if (virtualRole.domainType == versionedFolder.domainType && virtualRole.domainId == versionedFolder.id) {
                    virtualRole.asFinalised(versionedFolder.finalised)
                }
            }
        }
        userSecurityPolicy
    }

    UserSecurityPolicy updateAndStoreBuiltInSecurity(UserSecurityPolicy userSecurityPolicy, Boolean addAccess,
                                                     Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles,
                                                     Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRolesForParents) {
        if (!userSecurityPolicy.isLocked()) throw new ApiInternalException('GBSPMS', 'Cannot update on an unlocked UserPolicy')
        log.debug('Updating UserSecurityPolicy for builtInSecurity')
        if (addAccess) {
            // If readable then add to all policy managers
            userSecurityPolicy.includeVirtualRoles(virtualSecurableResourceGroupRoles)
            userSecurityPolicy.includeVirtualRoles(virtualSecurableResourceGroupRolesForParents)
        } else {
            // Remove from all policy managers we can do a perfect match to only remove the valid roles
            userSecurityPolicy.removeVirtualRoleIf {virtualSecurableResourceGroupRole ->
                virtualSecurableResourceGroupRole in virtualSecurableResourceGroupRoles
            }
            // Clean up parent tree of access
            Set<VirtualSecurableResourceGroupRole> allRolesToRemove = virtualSecurableResourceGroupRolesForParents.findAll {
                it in userSecurityPolicy.getVirtualSecurableResourceGroupRolesForBuilding()
            }
            removeRolesWithNoRequiredAccess(userSecurityPolicy, allRolesToRemove)
        }
        userSecurityPolicy
    }

    private UserSecurityPolicy buildUserSecurityPolicyForContent(
        UserSecurityPolicy userSecurityPolicy,
        Set<GroupRole> assignedApplicationGroupRoles,
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles) {

        Set<GroupRole> inheritedApplicationGroupRoles = new HashSet<>()

        VirtualGroupRole fullSecureableResourceAccessRole = null

        // Build the full list of application level roles, this will contain the assigned roles and all their children
        assignedApplicationGroupRoles.each {gr ->
            VirtualGroupRole virtualGroupRole = groupRoleService.getFromCache(gr.name)

            // Application admin and site admin are the roles we know about, store the highest level present as we need it later to define
            // catalogue item access
            if (virtualGroupRole.allowedRoles.any {it.name == GroupRole.SITE_ADMIN_ROLE_NAME}) {
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
            userSecurityPolicy.userGroupIds
        )

        // If user has application admin rights then they have all rights to all securable resources
        if (fullSecureableResourceAccessRole) {
            virtualSecurableResourceGroupRoles.addAll(buildFullAccessToAllSecurableResources(fullSecureableResourceAccessRole))
        } else {
            // Otherwise use the assigned roles to define access
            virtualSecurableResourceGroupRoles.addAll(buildControlledAccessToSecurableResources(securableResourceGroupRoles))
        }

        // If any container admin privileges then we need to make sure the container group admin role is added to the application level
        if (virtualSecurableResourceGroupRoles.any {it.groupRole.name == GroupRole.CONTAINER_ADMIN_ROLE_NAME}) {
            inheritedApplicationGroupRoles.add(groupRoleService.getFromCache(GroupRole.CONTAINER_GROUP_ADMIN_ROLE_NAME).groupRole)
        }

        // Add all users and groups access (only valid for application roles)
        virtualSecurableResourceGroupRoles.addAll(buildCatalogueUserVirtualRoles(inheritedApplicationGroupRoles))
        virtualSecurableResourceGroupRoles.addAll(buildUserGroupVirtualRoles(inheritedApplicationGroupRoles))

        userSecurityPolicy
            .withApplicationRoles(inheritedApplicationGroupRoles)
            .withSecurableRoles(securableResourceGroupRoles)
            .withVirtualRoles(virtualSecurableResourceGroupRoles)
    }

    protected Set<VirtualSecurableResourceGroupRole> buildControlledAccessToContentsOfContainer(Container container,
                                                                                                ContainerService containerService,
                                                                                                Set<GroupRole> accessRoles,
                                                                                                UserGroup userGroup,
                                                                                                GroupRole appliedGroupRole) {
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles

        // Load model access controls for all non-virtual containers
        if (!containerService.isContainerVirtual()) {
            virtualSecurableResourceGroupRoles = buildControlledAccessToModelsInContainer(container,
                                                                                          accessRoles,
                                                                                          userGroup,
                                                                                          appliedGroupRole)
        } else virtualSecurableResourceGroupRoles = [] as HashSet

        // Load sub containers
        List<Container> subContainers = containerService.findAllContainersInside(container.id) as List<Container>
        subContainers.each {subContainer ->
            virtualSecurableResourceGroupRoles.addAll(
                accessRoles.collect {igr ->
                    virtualSecurableResourceGroupRoleService.buildForSecurableResource(subContainer)
                        .withAccessLevel(igr)
                        .definedByGroup(userGroup)
                        .definedByAccessLevel(appliedGroupRole)
                }
            )

            if (!containerService.isContainerVirtual()) {
                // Load models inside container if its non-virtual
                virtualSecurableResourceGroupRoles.
                    addAll(buildControlledAccessToModelsInContainer(subContainer, accessRoles, userGroup, appliedGroupRole))
            }
        }

        virtualSecurableResourceGroupRoles
    }

    private Set<VirtualSecurableResourceGroupRole> buildCatalogueUserVirtualRoles(Set<GroupRole> applicationLevelRoles) {
        if (!applicationLevelRoles) return new HashSet<VirtualSecurableResourceGroupRole>()

        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as Set

        GroupRole highestRole = applicationLevelRoles.sort {it.depth}.first()
        VirtualGroupRole virtualGroupRole = groupRoleService.getFromCache(highestRole.name)
        Set<GroupRole> inheritedUserRoles = virtualGroupRole.allowedRoles
        List<CatalogueUser> users = catalogueUserService.list([:])

        users.each {user ->
            virtualSecurableResourceGroupRoles.addAll(
                inheritedUserRoles.collect {iur ->
                    virtualSecurableResourceGroupRoleService.buildForSecurableResource(user)
                        .definedByAccessLevel(highestRole)
                        .withAccessLevel(iur)
                }
            )
        }
        virtualSecurableResourceGroupRoles
    }

    private Set<VirtualSecurableResourceGroupRole> buildUserGroupVirtualRoles(Set<GroupRole> applicationLevelRoles) {
        if (!applicationLevelRoles) return new HashSet<VirtualSecurableResourceGroupRole>()

        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as Set

        GroupRole highestRole = applicationLevelRoles.sort {it.depth}.first()
        VirtualGroupRole virtualGroupRole = groupRoleService.getFromCache(highestRole.name)
        Set<GroupRole> inheritedUserRoles = virtualGroupRole.allowedRoles
        List<UserGroup> userGroups = userGroupService.list([:])

        userGroups.each {user ->
            virtualSecurableResourceGroupRoles.addAll(
                inheritedUserRoles.collect {iur ->
                    virtualSecurableResourceGroupRoleService.buildForSecurableResource(user)
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
        securableResourceServices.findAll {!it.handles(UserGroup) && !it.handles(CatalogueUser)}.each {service ->

            List<SecurableResource> resources = service.list() as List<SecurableResource>
            resources.each {securableResource ->
                virtualSecurableResourceGroupRoles.addAll(
                    inheritedContainerRoles.collect {igr ->
                        virtualSecurableResourceGroupRoleService.buildForSecurableResource(securableResource)
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
        securableResourceGroupRoles.each {sgr ->

            // Load in the actual securable resource
            sgr.setSecurableResource(securableResourceGroupRoleService.findSecurableResource(sgr.securableResourceDomainType,
                                                                                             sgr.securableResourceId), true)
            // Setup all the direct virtual roles
            Set<GroupRole> allowedRoles = groupRoleService.getFromCache(sgr.groupRole.name).allowedRoles
            virtualSecurableResourceGroupRoles.addAll(
                allowedRoles.collect {igr ->
                    virtualSecurableResourceGroupRoleService.buildFromSecurableResourceGroupRole(sgr)
                        .withAccessLevel(igr)
                }
            )

            // If we're securing a container then we need to create virtual roles for all its contents
            ContainerService containerService = containerServices.find {it.handles(sgr.securableResourceDomainType)}
            if (containerService) {
                virtualSecurableResourceGroupRoles.addAll(buildControlledAccessToContentsOfContainer(sgr.securableResource as Container,
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
        containerServices.each {service ->
            List<Container> containers = service.findAllReadableByEveryone() as List<Container>
            containers.each {container ->
                // Add reader access on each container
                virtualSecurableResourceGroupRoles.add(virtualSecurableResourceGroupRoleService.buildForSecurableResource(container)
                                                           .withAccessLevel(readerRole.groupRole))
                // Make sure contents of container are all readable as well
                virtualSecurableResourceGroupRoles.addAll(
                    buildControlledAccessToContentsOfContainer(container,
                                                               service,
                                                               readerRole.allowedRoles,
                                                               null,
                                                               readerRole.groupRole
                    )
                )
            }
        }
        modelServices.each {service ->
            List<Model> models = service.findAllReadableByEveryone() as List<Model>
            models.each {model ->
                // Add reader access on each container
                virtualSecurableResourceGroupRoles.add(virtualSecurableResourceGroupRoleService.buildForSecurableResource(model)
                                                           .withAccessLevel(readerRole.groupRole)
                )

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
        containerServices.each {service ->
            List<Container> containers = service.findAllReadableByAuthenticatedUsers() as List<Container>
            containers.each {container ->
                // Add reader access on each container
                virtualSecurableResourceGroupRoles.add(virtualSecurableResourceGroupRoleService.buildForSecurableResource(container)
                                                           .withAccessLevel(readerRole.groupRole))
                // Make sure contents of container are all readable as well
                virtualSecurableResourceGroupRoles.addAll(
                    buildControlledAccessToContentsOfContainer(container,
                                                               service,
                                                               readerRole.allowedRoles,
                                                               null,
                                                               readerRole.groupRole
                    )
                )
            }
        }
        modelServices.each {service ->
            List<Model> models = service.findAllReadableByAuthenticatedUsers() as List<Model>
            models.each {model ->
                // Add reader access on each container
                virtualSecurableResourceGroupRoles.add(virtualSecurableResourceGroupRoleService.buildForSecurableResource(model)
                                                           .withAccessLevel(readerRole.groupRole)
                )


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

        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = accessRoles.collect {igr ->
            virtualSecurableResourceGroupRoleService.buildForSecurableResource(container)
                .withAccessLevel(igr)
                .definedByGroup(userGroup)
                .definedByAccessLevel(appliedGroupRole)
        }.toSet()

        // Build parents
        if (container.depth != 0) {

            List<UUID> ids = container.path.split('/').toList().findAll().collect {Utils.toUuid(it)}

            ContainerService containerService = containerServices.find {it.handles(container.domainType)}
            containerService.getAll(ids).each {alternateContainer ->
                virtualSecurableResourceGroupRoles.addAll(accessRoles.collect {igr ->
                    virtualSecurableResourceGroupRoleService.buildForSecurableResource(alternateContainer as Container)
                        .withAccessLevel(igr)
                        .definedByGroup(userGroup)
                        .definedByAccessLevel(appliedGroupRole)
                })
            }
        }

        virtualSecurableResourceGroupRoles
    }

    private Set<VirtualSecurableResourceGroupRole> buildControlledAccessToModelsInContainer(Container container,
                                                                                            Set<GroupRole> accessRoles,
                                                                                            UserGroup userGroup,
                                                                                            GroupRole appliedGroupRole) {
        Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles = [] as Set

        // Load models
        modelServices.each {modelService ->

            List<Model> models = modelService.findAllByContainerId(container.id) as List<Model>
            models.each {Model model ->
                virtualSecurableResourceGroupRoles.addAll(
                    accessRoles.collect {igr ->
                        virtualSecurableResourceGroupRoleService
                            .buildForSecurableResource(model)
                            .withAccessLevel(igr)
                            .definedByGroup(userGroup)
                            .definedByAccessLevel(appliedGroupRole)
                    }
                )
            }
        }

        virtualSecurableResourceGroupRoles
    }


    private Set<VirtualSecurableResourceGroupRole> buildIndividualCatalogueUserVirtualRoles(CatalogueUser catalogueUser,
                                                                                            VirtualGroupRole applicationRole) {

        applicationRole.allowedRoles.collect {iur ->
            virtualSecurableResourceGroupRoleService.buildForSecurableResource(catalogueUser)
                .definedByAccessLevel(applicationRole.groupRole)
                .withAccessLevel(iur)
        }.toSet()
    }

    private void removeRolesWithNoRequiredAccess(UserSecurityPolicy userSecurityPolicy,
                                                 Set<VirtualSecurableResourceGroupRole> allRolesToRemove) {
        if (!allRolesToRemove) return

        List<UUID> requiredAccessIds = userSecurityPolicy
            .getVirtualSecurableResourceGroupRolesForBuilding()
            .collect {it.getDependsOnDomainIdAccess()}
            .findAll()
        Set<VirtualSecurableResourceGroupRole> canBeRemoved = allRolesToRemove.findAll {
            !(it.domainId in requiredAccessIds)
        }
        if (canBeRemoved) {
            userSecurityPolicy.removeVirtualRoles(canBeRemoved)
            allRolesToRemove.removeAll(canBeRemoved)
            removeRolesWithNoRequiredAccess(userSecurityPolicy, allRolesToRemove)
        }
    }
}
