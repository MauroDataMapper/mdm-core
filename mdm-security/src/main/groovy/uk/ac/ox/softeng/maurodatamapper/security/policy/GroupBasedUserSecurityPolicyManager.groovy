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
package uk.ac.ox.softeng.maurodatamapper.security.policy

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.authority.Authority
import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.container.VersionedFolder
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKey
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.VirtualSecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import grails.core.GrailsClass
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.orm.hibernate.proxy.HibernateProxyHandler

import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.AUTHORITY_ADMIN_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.AUTHOR_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.CHANGE_FOLDER_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.CONTAINER_ADMIN_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.DELETE_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.DISALLOWED_MODELITEM_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.DISALLOWED_ONCE_FINALISED_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.EDITOR_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.EDITOR_VERSIONING_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.EDIT_DESCRIPTION_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.FINALISED_EDIT_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.FINALISED_READ_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.FINALISE_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.FULL_DELETE_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.MERGE_INTO_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.NEW_BRANCH_MODEL_VERSION_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.NEW_DOCUMENTATION_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.NEW_MODEL_VERSION_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.READER_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.READER_VERSIONING_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.READ_BY_AUTHENTICATED_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.READ_BY_EVERYONE_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.READ_ONLY_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.REVIEWER_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.SAVE_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.SAVE_IGNORE_FINALISE
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.SOFT_DELETE_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.STANDARD_CREATE_AND_EDIT_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.STANDARD_EDIT_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.SUBSET_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.UPDATE_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.UPDATE_IGNORE_FINALISE
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.USER_ADMIN_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.CONTAINER_ADMIN_CONTAINER_TREE_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.CONTAINER_ADMIN_FOLDER_TREE_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.CONTAINER_ADMIN_MODEL_TREE_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.CREATE_FOLDER
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.CREATE_MODEL
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.CREATE_MODEL_ITEM
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.CREATE_VERSIONED_FOLDER
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.DISALLOWED_MODELITEM_TREE_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.EDITOR_CONTAINER_TREE_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.EDITOR_FOLDER_TREE_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.EDITOR_MODEL_TREE_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.TreeActions.MOVE_TO_VERSIONED_FOLDER
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.APPLICATION_ADMIN_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.AUTHOR_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.CONTAINER_ADMIN_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.CONTAINER_GROUP_ADMIN_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.EDITOR_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.GROUP_ADMIN_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.READER_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.REVIEWER_ROLE_NAME
import static uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole.USER_ADMIN_ROLE_NAME

/**
 * This class should be built using the GroupBasedSecurityPolicyManagerService which will have transactionality available.
 * All operations on this class and inside this class should assume no session and no transaction are available.
 * Therefore everything needed to determine access rights needs to be defined when the class is created.
 *
 * Application permitted roles should be the complete list of inherited and assigned application roles.
 * Virtual Securable Resource Group Roles should be the complete list of inherited and assigned roles for each securable resource,
 * in the event a user is application or site admin level then all secured resources should be assigned a virtual level, this will allow
 * zero calls to the database when ascertaining access. It also means we can make 1 check for rights to access.
 */
@Slf4j
@CompileStatic
class GroupBasedUserSecurityPolicyManager implements UserSecurityPolicyManager {

    private static final HibernateProxyHandler hibernateProxyHandler = new HibernateProxyHandler()
    private UserSecurityPolicy userPolicy
    private GrailsApplication grailsApplication

    GroupBasedUserSecurityPolicyManager() {
    }

    GroupBasedUserSecurityPolicyManager inApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        this
    }

    GroupBasedUserSecurityPolicyManager withUserPolicy(UserSecurityPolicy userPolicy) {
        if (this.userPolicy) throw new ApiInternalException('GBUSPM', 'Cannot set a UserPolicy when its already set')
        this.userPolicy = userPolicy
        this
    }

    GroupBasedUserSecurityPolicyManager withUpdatedUserPolicy(UserSecurityPolicy userPolicy) {
        if (!this.userPolicy) throw new ApiInternalException('GBUSPM', 'Cannot update a UserPolicy when its not set')
        this.userPolicy = userPolicy
        this
    }

    GroupBasedUserSecurityPolicyManager lock() {
        this.userPolicy.lock()
        this
    }

    GroupBasedUserSecurityPolicyManager unlock() {
        this.userPolicy.unlock()
        this
    }

    boolean isLocked() {
        this.userPolicy.locked
    }

    boolean isUnLocked() {
        !this.userPolicy.locked
    }

    boolean userPolicyIsManagedByGroup(UserGroup userGroup) {
        userPolicy.isManagedByGroup(userGroup)
    }

    boolean userPolicyManagesAccessToSecurableResource(SecurableResource securableResource) {
        userPolicy.managesVirtualAccessToSecurableResource(securableResource)
    }

    boolean userPolicyManagesAccessToSecurableResource(String securableResourceDomainType, UUID securableResourceId) {
        userPolicy.managesVirtualAccessToSecurableResource(securableResourceDomainType, securableResourceId)
    }

    boolean userPolicyHasApplicationRoles() {
        userPolicy.applicationPermittedRoles
    }

    UserSecurityPolicy getUserPolicy() {
        this.userPolicy
    }

    GroupBasedUserSecurityPolicyManager ensureCatalogueUser(CatalogueUser catalogueUser) {
        this.userPolicy.ensureCatalogueUser(catalogueUser)
        this
    }

    GroupBasedUserSecurityPolicyManager withUpdatedUserGroups(Set<UserGroup> userGroups) {
        this.userPolicy.inGroups(userGroups)
        this
    }

    void revoke() {
        userPolicy.destroy()
    }

    @Override
    CatalogueUser getUser() {
        userPolicy.getUser()
    }

    @Override
    List<UUID> listReadableSecuredResourceIds(Class<? extends SecurableResource>... securableResourceClasses) {
        securableResourceClasses.collectMany {securableResourceClass ->
            userPolicy.getVirtualSecurableResourceGroupRoles()
                .findAll {it.value.first().matchesDomainResourceType(securableResourceClass)}
                .collect {it.key}
        }
    }

    @Override
    boolean userCanReadResourceId(Class resourceClass, UUID id,
                                  Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanReadSecuredResourceId(resourceClass as Class<? extends SecurableResource>, id)
        }
        // Allow users to read any user image files
        if (Utils.parentClassIsAssignableFromChild(UserImageFile, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(CatalogueUser, owningSecureResourceClass)) {
            return true
        }

        // Users can read their own api keys and user admins can do as well
        if (Utils.parentClassIsAssignableFromChild(ApiKey, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(CatalogueUser, owningSecureResourceClass)) {
            return getUser().id == owningSecureResourceId || getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, USER_ADMIN_ROLE_NAME)
        }

        return userCanReadSecuredResourceId(owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    boolean userCanCreateResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        // Allow editors of folders to create models in that folder
        if (owningSecureResourceClass && Utils.parentClassIsAssignableFromChild(Model, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(Folder, owningSecureResourceClass)) {
            return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, EDITOR_ROLE_NAME)
        }

        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanCreateSecuredResourceId(resourceClass as Class<? extends SecurableResource>, id)
        }
        // Allow reviewers to create annotations on a versioned folders and models
        if (Utils.parentClassIsAssignableFromChild(Annotation, resourceClass) &&
            (Utils.parentClassIsAssignableFromChild(Model, owningSecureResourceClass) ||
             Utils.parentClassIsAssignableFromChild(VersionedFolder, owningSecureResourceClass))) {
            return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, REVIEWER_ROLE_NAME)
        }
        // Allow authors to create metadata (and therefore profiles) on a versioned folders and models
        if (Utils.parentClassIsAssignableFromChild(Metadata, resourceClass) &&
            (Utils.parentClassIsAssignableFromChild(Model, owningSecureResourceClass) ||
             Utils.parentClassIsAssignableFromChild(VersionedFolder, owningSecureResourceClass))) {
            VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, AUTHOR_ROLE_NAME)
            return role ? !role.isFinalised() : false
        }
        // Allow container admins to edit/create permissions on containers no matter what state the secured resource is in
        if (Utils.parentClassIsAssignableFromChild(SecurableResourceGroupRole, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(Container, owningSecureResourceClass)) {
            return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, CONTAINER_ADMIN_ROLE_NAME)
        }
        // Allow editors to edit/create permissions on models no matter what state the secured resource is in
        if (Utils.parentClassIsAssignableFromChild(SecurableResourceGroupRole, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(Model, owningSecureResourceClass)) {
            return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, EDITOR_ROLE_NAME)
        }
        // Allow users to create their own user image file
        if (Utils.parentClassIsAssignableFromChild(UserImageFile, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(CatalogueUser, owningSecureResourceClass) &&
            owningSecureResourceId == getUser().id) {
            return true
        }

        return userCanCreateSecuredResourceId(owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    boolean userCanEditResourceId(Class resourceClass, UUID id,
                                  Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        userCanWriteResourceId(resourceClass, id, owningSecureResourceClass, owningSecureResourceId, UPDATE_ACTION)
    }

    @Override
    boolean userCanDeleteResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanDeleteSecuredResourceId(resourceClass as Class<? extends SecurableResource>, id, false)
        }
        // Allow users to create their own user image file
        if (Utils.parentClassIsAssignableFromChild(UserImageFile, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(CatalogueUser, owningSecureResourceClass) &&
            owningSecureResourceId == getUser().id) {
            return true
        }
        // Allow users to delete their own api keys
        if (Utils.parentClassIsAssignableFromChild(ApiKey, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(CatalogueUser, owningSecureResourceClass) &&
            owningSecureResourceId == getUser().id) {
            return true
        }

        // Allow authors to delete metadata (and therefore profiles) on a versioned folders and models
        if (Utils.parentClassIsAssignableFromChild(Metadata, resourceClass) &&
            (Utils.parentClassIsAssignableFromChild(Model, owningSecureResourceClass) ||
             Utils.parentClassIsAssignableFromChild(VersionedFolder, owningSecureResourceClass))) {
            VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, AUTHOR_ROLE_NAME)
            return role ? !role.isFinalised() : false
        }

        // Allow container admins to delete permissions on containers no matter what state the secured resource is in
        if (Utils.parentClassIsAssignableFromChild(SecurableResourceGroupRole, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(Container, owningSecureResourceClass)) {
            return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, CONTAINER_ADMIN_ROLE_NAME)
        }
        // Allow editors to delete permissions on models no matter what state the secured resource is in
        if (Utils.parentClassIsAssignableFromChild(SecurableResourceGroupRole, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(Model, owningSecureResourceClass)) {
            return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, EDITOR_ROLE_NAME)
        }

        // Editors can delete ModelItems, however CAs can also delete finalised models which we cant allow the deletion of modelitems so we can't fall through
        // to the SecuredResource check
        if (Utils.parentClassIsAssignableFromChild(ModelItem, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(Model, owningSecureResourceClass)) {
            VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, EDITOR_ROLE_NAME)
            return role ? !role.isFinalised() : false
        }

        return userCanDeleteSecuredResourceId(owningSecureResourceClass, owningSecureResourceId, false)
    }

    @Override
    boolean userCanWriteResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId, String action) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanWriteSecuredResourceId(resourceClass as Class<? extends SecurableResource>, id, action)
        }
        // Allow users to edit their own user image file
        if (Utils.parentClassIsAssignableFromChild(UserImageFile, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(CatalogueUser, owningSecureResourceClass) &&
            owningSecureResourceId == getUser().id) {
            return true
        }
        if (Utils.parentClassIsAssignableFromChild(Metadata, resourceClass) &&
            (Utils.parentClassIsAssignableFromChild(Model, owningSecureResourceClass) ||
             Utils.parentClassIsAssignableFromChild(VersionedFolder, owningSecureResourceClass))) {
            if (action in [SAVE_IGNORE_FINALISE, UPDATE_IGNORE_FINALISE])
                return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, AUTHOR_ROLE_NAME)
            else if (action == UPDATE_ACTION) {
                VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, AUTHOR_ROLE_NAME)
                return role ? !role.isFinalised() : false
            }

        }
        return userCanWriteSecuredResourceId(owningSecureResourceClass, owningSecureResourceId, action)
    }

    @Override
    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResourceClass) && !id) {
            return hasApplicationLevelRole(CONTAINER_GROUP_ADMIN_ROLE_NAME)
        }
        if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResourceClass) && !id) {
            return hasApplicationLevelRole(USER_ADMIN_ROLE_NAME)
        }
        if (Utils.parentClassIsAssignableFromChild(Authority, securableResourceClass)) {
            // Anyone who's logged in can index or read an authority
            return isAuthenticated()
        }
        if (Utils.parentClassIsAssignableFromChild(GroupRole, securableResourceClass)) {
            // Group admin can index and get GroupRoles
            return hasApplicationLevelRole(GROUP_ADMIN_ROLE_NAME)
        }
        hasAnyAccessToSecuredResource(securableResourceClass, id)
    }

    @Override
    boolean userCanCreateSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        userCanWriteSecuredResourceId(securableResourceClass, id, SAVE_ACTION)
    }

    @Override
    boolean userCanEditSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        userCanWriteSecuredResourceId(securableResourceClass, id, UPDATE_ACTION)
    }

    @Override
    boolean userCanDeleteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, boolean permanent) {
        userCanWriteSecuredResourceId(securableResourceClass, id, permanent ? DELETE_ACTION : SOFT_DELETE_ACTION)
    }

    @Override
    boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action) {

        // Initial VF permissions to provide changes where necessary
        if (Utils.parentClassIsAssignableFromChild(VersionedFolder, securableResourceClass)) {
            switch (action) {
                case READ_BY_AUTHENTICATED_ACTION:
                case READ_BY_EVERYONE_ACTION:
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
            }
        }

        // Treat VFs and Models the same for everything else
        if (Utils.parentClassIsAssignableFromChild(Model, securableResourceClass) ||
            Utils.parentClassIsAssignableFromChild(VersionedFolder, securableResourceClass)) {
            switch (action) {
                case DELETE_ACTION:
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
                case SOFT_DELETE_ACTION:
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                case CHANGE_FOLDER_ACTION:
                    //Changing folder is like an update, but without checking if the model is finalised
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, AUTHOR_ROLE_NAME)
                case READ_BY_AUTHENTICATED_ACTION:
                case READ_BY_EVERYONE_ACTION:
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                case FINALISE_ACTION:
                    // Can the model be finalised
                    VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                    return role ? role.canFinalise() : false
                case NEW_MODEL_VERSION_ACTION:
                case NEW_BRANCH_MODEL_VERSION_ACTION:
                case NEW_DOCUMENTATION_ACTION:
                    // If model is finalised then these actions are allowed
                    VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                    return role ? role.canVersion() : false
                case MERGE_INTO_ACTION:
                case SAVE_ACTION:
                case SUBSET_ACTION:
                case UPDATE_ACTION:
                    // If the model is finalised then these actions are NOT allowed
                    VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                    return role ? !role.isFinalised() : false
                case EDIT_DESCRIPTION_ACTION:
                    // An author can update a model description, but once a model is finalised the model can NOT be updated
                    VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, AUTHOR_ROLE_NAME)
                    return role ? !role.isFinalised() : false
                case SAVE_IGNORE_FINALISE:
                    // Special case scenarios where actions are possibly on finalised object which we want to allow
                    // In this situation we dont care if the item is finalised or not as long as they have the right role
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                case UPDATE_IGNORE_FINALISE:
                    // Special case scenarios where actions are possibly on finalised object which we want to allow
                    // In this situation we dont care if the item is finalised or not as long as they have the right role
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, AUTHOR_ROLE_NAME)
                default:
                    log.warn('Attempt to access secured class {} id {} to {}', securableResourceClass.simpleName, id, action)
                    return false
            }
        }

        if (Utils.parentClassIsAssignableFromChild(Container, securableResourceClass)) {
            // If no id then its a top level container and therefore anyone who's logged in can create
            if (!id) return isAuthenticated()
            return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
        }

        if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResourceClass)) {
            // User cannot delete themselves
            if (action in FULL_DELETE_ACTIONS && id == user.id) {
                return false
            }
            return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, USER_ADMIN_ROLE_NAME)
        }

        if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResourceClass)) {
            switch (action) {
                case SAVE_ACTION:
                    return hasApplicationLevelRole(CONTAINER_GROUP_ADMIN_ROLE_NAME)
                default:
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, GROUP_ADMIN_ROLE_NAME)
            }
        }

        if (Utils.parentClassIsAssignableFromChild(Authority, securableResourceClass)) {
            return hasApplicationLevelRole(APPLICATION_ADMIN_ROLE_NAME)
        }

        log.warn('Attempt to access secured class {} id {} to {}', securableResourceClass.simpleName, id, action)
        false

    }

    @Override
    List<String> userAvailableActions(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        return securedResourceUserAvailableActions(securableResourceClass, id).toSet().sort()
    }

    @Override
    List<String> userAvailableActions(String domainType, UUID id) {
        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, domainType)
        return userAvailableActions(grailsClass.clazz, id)
    }

    @Override
    List<String> userAvailableActions(Serializable resourceClass, UUID id) {
        if (resourceClass instanceof Class) {
            return userAvailableActions(resourceClass as Class, id)
        }
        if (resourceClass instanceof String) {
            return userAvailableActions(resourceClass as String, id)
        }
        throw new ApiInternalException('USPM02', "Unrecognised resourceClass type ${resourceClass.class}")
    }

    @Override
    List<String> userAvailableActions(String resourceDomainType, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                      UUID owningSecureResourceId) {
        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, resourceDomainType)
        userAvailableActions(grailsClass.clazz, id, owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    List<String> userAvailableActions(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                      UUID owningSecureResourceId) {

        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userAvailableActions(resourceClass as Class<? extends SecurableResource>, id)
        }

        List<String> owningResourceActions = userAvailableActions(owningSecureResourceClass, owningSecureResourceId)
        if (Utils.parentClassIsAssignableFromChild(ModelItem, resourceClass)) {
            if (getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, EDITOR_ROLE_NAME)) {
                // Editors can delete model items, but they cant delete the models
                owningResourceActions << DELETE_ACTION
            }
            return (owningResourceActions - DISALLOWED_MODELITEM_ACTIONS).toSet().sort()
        }
        return owningResourceActions
    }

    @Override
    List<String> userAvailableTreeActions(String securableResourceDomainType, UUID id) {
        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, securableResourceDomainType)
        userAvailableTreeActions(grailsClass.getClazz(), id)
    }

    List<String> userAvailableTreeActions(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        securedResourceUserAvailableTreeActions(hibernateProxyHandler.unwrapIfProxy(securableResourceClass) as Class<? extends SecurableResource>,
                                                id).toSet().sort()
    }

    @Override
    List<String> userAvailableTreeActions(String resourceDomainType, UUID id, String owningSecureResourceDomainType, UUID owningSecureResourceId) {
        GrailsClass grailsClass = Utils.lookupGrailsDomain(grailsApplication, resourceDomainType)
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, grailsClass.getClazz())) {
            return userAvailableTreeActions(grailsClass.getClazz() as Class<? extends SecurableResource>, id)
        }

        List<String> owningResourceActions = userAvailableTreeActions(owningSecureResourceDomainType, owningSecureResourceId)
        if (Utils.parentClassIsAssignableFromChild(ModelItem, grailsClass.getClazz())) {
            return owningResourceActions - DISALLOWED_MODELITEM_TREE_ACTIONS
        }
        return owningResourceActions
    }

    @Override
    boolean isApplicationAdministrator() {
        hasApplicationLevelRole(APPLICATION_ADMIN_ROLE_NAME)
    }

    @Override
    boolean isAuthenticated() {
        userPolicy.isAuthenticated()
    }

    @Override
    boolean isPending() {
        user && userPolicy.getUser().pending
    }

    boolean hasUserAdminRights() {
        hasApplicationLevelRole(USER_ADMIN_ROLE_NAME)
    }

    boolean hasGroupAdminRights() {
        hasApplicationLevelRole(GROUP_ADMIN_ROLE_NAME)
    }

    private List<String> securedResourceUserAvailableActions(Class<? extends SecurableResource> securableResourceClass, UUID id) {

        if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResourceClass)) {
            if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, USER_ADMIN_ROLE_NAME)) {
                return USER_ADMIN_ACTIONS
            }
            return READ_ONLY_ACTIONS
        }

        if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResourceClass)) {
            return getStandardActionsWithControlRole(securableResourceClass, id, GROUP_ADMIN_ROLE_NAME)
        }

        if (Utils.parentClassIsAssignableFromChild(Model, securableResourceClass) ||
            Utils.parentClassIsAssignableFromChild(VersionedFolder, securableResourceClass)) {
            // The below should get editor level versioning and finalisation rights
            VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
            if (role) {
                return updateBaseModelActionsForEditor(CONTAINER_ADMIN_ACTIONS, role)
            }
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
            if (role) {
                return updateBaseModelActionsForEditor(EDITOR_ACTIONS, role)
            }
            // All the below should only get reader level versioning and finalisation rights
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, AUTHOR_ROLE_NAME)
            if (role) {
                return updateBaseModelActionsForReader(AUTHOR_ACTIONS, role)
            }
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, REVIEWER_ROLE_NAME)
            if (role) {
                return updateBaseModelActionsForReader(REVIEWER_ACTIONS, role)
            }
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, READER_ROLE_NAME)
            if (role) {
                return updateBaseModelActionsForReader(READER_ACTIONS, role)
            }
            return []
        }
        if (Utils.parentClassIsAssignableFromChild(GroupRole, securableResourceClass)) {
            // No actions are allowed directly on GroupRole
            return READ_ONLY_ACTIONS
        }

        if (Utils.parentClassIsAssignableFromChild(Container, securableResourceClass)) {
            if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)) {
                return CONTAINER_ADMIN_ACTIONS
            }
            //            if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)) {
            //                return EDITOR_ACTIONS
            //            }
            if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, READER_ROLE_NAME)) {
                return READ_ONLY_ACTIONS
            }
            return []
        }

        if (Utils.parentClassIsAssignableFromChild(Authority, securableResourceClass)) {
            if (hasApplicationLevelRole(APPLICATION_ADMIN_ROLE_NAME)) {
                return AUTHORITY_ADMIN_ACTIONS
            }
            return READ_ONLY_ACTIONS
        }


        log.warn('Attempt to gain available actions for unknown secured class {} id {}', securableResourceClass.simpleName, id)
        []
    }

    private List<String> securedResourceUserAvailableTreeActions(Class<? extends SecurableResource> securableResourceClass, UUID id) {

        VirtualSecurableResourceGroupRole role

        if (Utils.parentClassIsAssignableFromChild(Model, securableResourceClass)) {
            // The below should get editor level versioning and finalisation rights
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
            if (role) {
                return updateBaseTreeActions(CONTAINER_ADMIN_MODEL_TREE_ACTIONS, role)
            }
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
            if (role) {
                return updateBaseTreeActions(EDITOR_MODEL_TREE_ACTIONS, role)
            }
            return []
        }

        if (Utils.parentClassIsAssignableFromChild(Folder, securableResourceClass)) {
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
            if (role) {
                return updateBaseTreeActions(CONTAINER_ADMIN_FOLDER_TREE_ACTIONS, role)
            }
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
            if (role) {
                return updateBaseTreeActions(EDITOR_FOLDER_TREE_ACTIONS, role)
            }
            return []
        }

        // Allow for non-folder containers
        if (Utils.parentClassIsAssignableFromChild(Container, securableResourceClass)) {
            if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)) {
                return CONTAINER_ADMIN_CONTAINER_TREE_ACTIONS
            }
            if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)) {
                return EDITOR_CONTAINER_TREE_ACTIONS
            }
            return []
        }

        log.warn('Attempt to gain available tree actions for unhandled secured class {} id {}', securableResourceClass.simpleName, id)
        []
    }

    private List<String> updateBaseModelActionsForEditor(List<String> baseActions, VirtualSecurableResourceGroupRole role) {
        List<String> updatedActions = new ArrayList<>(baseActions)
        if (role.canFinalise()) {
            updatedActions << FINALISE_ACTION
        } else {
            // If it cant be finalised its either a non-main branch or inside a VF or finalised
            // The last 2 cases we can remove the action in this method
            updatedActions << MERGE_INTO_ACTION
        }
        if (role.canVersion()) {
            updatedActions.addAll(EDITOR_VERSIONING_ACTIONS)
        }
        if (role.isVersionControlled()) {
            // If cant be versioned its inside a VF therefore shouldn't allow mergeInto
            updatedActions.remove(MERGE_INTO_ACTION)
        }
        if (role.isFinalised()) {
            updatedActions.removeAll(DISALLOWED_ONCE_FINALISED_ACTIONS)
            updatedActions << FINALISED_EDIT_ACTIONS
        }
        updatedActions
    }

    private List<String> updateBaseModelActionsForReader(List<String> baseActions, VirtualSecurableResourceGroupRole role) {
        List<String> updatedActions = new ArrayList<>(baseActions)
        if (role.isFinalised()) {
            updatedActions.removeAll(DISALLOWED_ONCE_FINALISED_ACTIONS)
            updatedActions << FINALISED_READ_ACTIONS
        }
        if (role.canVersion() && isAuthenticated()) {
            updatedActions.addAll(READER_VERSIONING_ACTIONS)
        }
        updatedActions
    }

    private List<String> updateBaseTreeActions(List<String> baseActions, VirtualSecurableResourceGroupRole role) {
        List<String> updatedActions = new ArrayList<>(baseActions)
        if (role.isFinalised()) {
            updatedActions.removeAll([MOVE_TO_VERSIONED_FOLDER, CREATE_FOLDER, CREATE_MODEL, CREATE_MODEL_ITEM, CREATE_VERSIONED_FOLDER])
        }
        if (role.hasVersionedContents()) {
            // Cannot move anything versioned controlled into a VF, this includes a folder which contains VFs
            updatedActions.remove(MOVE_TO_VERSIONED_FOLDER)
        }
        if (role.isVersionable() || role.isVersionControlled()) {
            updatedActions.remove(CREATE_VERSIONED_FOLDER)
        }
        updatedActions
    }

    private boolean hasAnyAccessToSecuredResource(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        if (!id) {

            // No id means indexing endpoint
            // Users and Groups can be indexed with show actions by the bottom layer of application roles
            if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResourceClass)) {
                return hasApplicationLevelRole(CONTAINER_GROUP_ADMIN_ROLE_NAME)
            }

            if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResourceClass)) {
                return hasApplicationLevelRole(CONTAINER_GROUP_ADMIN_ROLE_NAME)
            }
            if (Utils.parentClassIsAssignableFromChild(Container, securableResourceClass) || Utils.parentClassIsAssignableFromChild(Model, securableResourceClass)) {
                // To be clear that containers and models may have open indexing but its not stated in the policy
                return false
            }

            log.warn('Checking for any access to secured resource class {} without providing an ID', securableResourceClass.simpleName)
            return false
        }

        userPolicy.managesVirtualAccessToSecurableResource(securableResourceClass, id)
    }

    private VirtualSecurableResourceGroupRole getSpecificLevelAccessToSecuredResource(Class<? extends SecurableResource> securableResourceClass,
                                                                                      UUID id,
                                                                                      String roleName) {
        userPolicy.getVirtualRoleForSecuredResource(securableResourceClass, id, roleName)
    }

    private boolean hasApplicationLevelRole(String rolename) {
        userPolicy.getApplicationPermittedRoles().any {it.name == rolename}
    }

    private List<String> getStandardActionsWithControlRole(Class<? extends SecurableResource> securableResourceClass, UUID id, String roleName) {
        if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, roleName)) {
            if (id) return STANDARD_EDIT_ACTIONS
            log.error("**** **** BOOM how'd we get here")
            return STANDARD_CREATE_AND_EDIT_ACTIONS
        } else if (hasAnyAccessToSecuredResource(securableResourceClass, id)) {
            return READ_ONLY_ACTIONS
        } else return []
    }
}
