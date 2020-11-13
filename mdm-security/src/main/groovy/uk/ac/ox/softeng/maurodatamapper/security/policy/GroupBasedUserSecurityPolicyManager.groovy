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
package uk.ac.ox.softeng.maurodatamapper.security.policy

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKey
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser
import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.security.role.VirtualSecurableResourceGroupRole
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.core.GrailsApplication
import grails.core.GrailsClass
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import groovy.util.logging.Slf4j

import java.util.function.Predicate

import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.CHANGE_FOLDER_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.CONTAINER_CONTAINER_ADMIN_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.CONTAINER_EDITOR_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.DELETE_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.FINALISE_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.FULL_DELETE_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.MERGE_INTO_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.MODELITEM_DISALLOWED_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.MODEL_AUTHOR_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.MODEL_CONTAINER_ADMIN_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.MODEL_DISALLOWED_EDITOR_VERSIONING_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.MODEL_DISALLOWED_FINALISED_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.MODEL_EDITOR_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.MODEL_READER_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.MODEL_READER_VERSIONING_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.MODEL_REVIEWER_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.NEW_BRANCH_MODEL_VERSION_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.NEW_DOCUMENTATION_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.NEW_MODEL_VERSION_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.READ_ONLY_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.SAVE_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.SOFT_DELETE_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.STANDARD_CREATE_AND_EDIT_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.STANDARD_EDIT_ACTIONS
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.UPDATE_ACTION
import static uk.ac.ox.softeng.maurodatamapper.security.policy.ResourceActions.USER_ADMIN_ACTIONS
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
class GroupBasedUserSecurityPolicyManager implements UserSecurityPolicyManager {

    CatalogueUser user
    Set<UserGroup> userGroups
    List<SecurableResourceGroupRole> securableResourceGroupRoles
    Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles
    Set<GroupRole> applicationPermittedRoles

    GrailsApplication grailsApplication

    GroupBasedUserSecurityPolicyManager() {
        userGroups = [] as Set
        setNoAccess()
    }

    GroupBasedUserSecurityPolicyManager inApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication
        this
    }

    GroupBasedUserSecurityPolicyManager forUser(CatalogueUser catalogueUser) {
        user = catalogueUser
        this
    }

    GroupBasedUserSecurityPolicyManager inGroups(Set<UserGroup> userGroups) {
        this.userGroups = userGroups
        this
    }

    GroupBasedUserSecurityPolicyManager withSecurableRoles(List<SecurableResourceGroupRole> securableResourceGroupRoles) {
        this.securableResourceGroupRoles = securableResourceGroupRoles
        this
    }

    GroupBasedUserSecurityPolicyManager includeSecurableRoles(List<SecurableResourceGroupRole> securableResourceGroupRoles) {
        this.securableResourceGroupRoles.addAll(securableResourceGroupRoles)
        this
    }

    GroupBasedUserSecurityPolicyManager withVirtualRoles(Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles) {
        this.virtualSecurableResourceGroupRoles = virtualSecurableResourceGroupRoles
        this
    }

    GroupBasedUserSecurityPolicyManager withApplicationRoles(Set<GroupRole> applicationPermittedRoles) {
        this.applicationPermittedRoles = applicationPermittedRoles
        this
    }

    GroupBasedUserSecurityPolicyManager includeApplicationRoles(Set<GroupRole> applicationPermittedRoles) {
        this.applicationPermittedRoles.addAll(applicationPermittedRoles)
        this
    }

    GroupBasedUserSecurityPolicyManager includeVirtualRoles(Set<VirtualSecurableResourceGroupRole> virtualSecurableResourceGroupRoles) {
        this.virtualSecurableResourceGroupRoles.addAll(virtualSecurableResourceGroupRoles)
        this
    }

    GroupBasedUserSecurityPolicyManager hasNoAccess() {
        setNoAccess()
        this
    }

    GroupBasedUserSecurityPolicyManager removeVirtualRoleIf(@ClosureParams(value =
        SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.security.role.VirtualSecurableResourceGroupRole') Closure predicate) {
        virtualSecurableResourceGroupRoles.removeIf([test: predicate] as Predicate)
        this
    }

    GroupBasedUserSecurityPolicyManager removeAssignedRoleIf(@ClosureParams(value =
        SimpleType, options = 'uk.ac.ox.softeng.maurodatamapper.security.role.SecurableResourceGroupRole') Closure predicate) {
        securableResourceGroupRoles.removeIf([test: predicate] as Predicate)
        this
    }

    // If the usergroups are set then the entire security policy is unsure so will need to be rebuilt
    void setUserGroups(Set<UserGroup> userGroups) {
        setNoAccess()
        this.userGroups = userGroups.toSet()
    }

    void setNoAccess() {
        applicationPermittedRoles = [] as Set
        securableResourceGroupRoles = []
        virtualSecurableResourceGroupRoles = [] as Set
    }

    @Override
    List<UUID> listReadableSecuredResourceIds(Class<? extends SecurableResource> securableResourceClass) {
        virtualSecurableResourceGroupRoles
            .findAll { it.domainType == securableResourceClass.simpleName }
            .collect { it.domainId }
            .toSet()
            .toList()
    }

    @Override
    boolean userCanReadResourceId(Class resourceClass, UUID id,
                                  Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanReadSecuredResourceId(resourceClass, id)
        }
        // Allow users to read any user image files
        if (Utils.parentClassIsAssignableFromChild(UserImageFile, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(CatalogueUser, owningSecureResourceClass)) {
            return true
        }
        return userCanReadSecuredResourceId(owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    boolean userCanCreateResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanCreateSecuredResourceId(resourceClass, id)
        }
        // Allow reviewers to create annotations on a model if they have reviewer status
        if (Utils.parentClassIsAssignableFromChild(Annotation, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(Model, owningSecureResourceClass)) {
            return getSpecificLevelAccessToSecuredResource(owningSecureResourceClass, owningSecureResourceId, REVIEWER_ROLE_NAME)
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
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanEditSecuredResourceId(resourceClass, id)
        }
        // Allow users to edit their own user image file
        if (Utils.parentClassIsAssignableFromChild(UserImageFile, resourceClass) &&
            Utils.parentClassIsAssignableFromChild(CatalogueUser, owningSecureResourceClass) &&
            owningSecureResourceId == getUser().id) {
            return true
        }
        return userCanEditSecuredResourceId(owningSecureResourceClass, owningSecureResourceId)
    }

    @Override
    boolean userCanDeleteResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return userCanDeleteSecuredResourceId(resourceClass, id, false)
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
        return userCanDeleteSecuredResourceId(owningSecureResourceClass, owningSecureResourceId, false)
    }

    @Override
    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResourceClass) && !id) {
            return hasApplicationLevelRole(CONTAINER_GROUP_ADMIN_ROLE_NAME)
        }
        if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResourceClass) && !id) {
            return hasApplicationLevelRole(USER_ADMIN_ROLE_NAME)
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

        if (Utils.parentClassIsAssignableFromChild(Container, securableResourceClass)) {
            // If no id then its a top level container and therefore anyone who's logged in can create
            if (!id) return isAuthenticated()
            if (action in [SAVE_ACTION, SOFT_DELETE_ACTION]) {
                // Editors can save new folders and models and SOFT DELETE
                return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
            }
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

        if (Utils.parentClassIsAssignableFromChild(Model, securableResourceClass)) {
            switch (action) {
                case NEW_MODEL_VERSION_ACTION:
                case NEW_BRANCH_MODEL_VERSION_ACTION:
                case NEW_DOCUMENTATION_ACTION:
                    VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                    return role ? role.isFinalisedModel() : false
                case DELETE_ACTION:
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
                case SOFT_DELETE_ACTION:
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                case UPDATE_ACTION:
                    VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, AUTHOR_ROLE_NAME)
                    return role ? !role.isFinalisedModel() : false
                case CHANGE_FOLDER_ACTION:
                    //Changing folder is like an update, but without checking if the model is finalised
                    return getSpecificLevelAccessToSecuredResource(securableResourceClass, id, AUTHOR_ROLE_NAME)                
                case MERGE_INTO_ACTION:
                case SAVE_ACTION:
                    VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                    return role ? !role.isFinalisedModel() : false
                case FINALISE_ACTION:
                    VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
                    return role ? role.canFinaliseModel() : false
                default:
                    log.warn('Attempt to access secured class {} id {} to {}', securableResourceClass.simpleName, id, action)
                    return false
            }
        }

        log.warn('Attempt to access secured class {} id {} to {}', securableResourceClass.simpleName, id, action)
        false

    }

    @Override
    List<String> userAvailableActions(Class resourceClass, UUID id) {
        if (Utils.parentClassIsAssignableFromChild(SecurableResource, resourceClass)) {
            return securedResourceUserAvailableActions(resourceClass, id)
        }
        throw new ApiNotYetImplementedException('USPM01', 'Obtain user actions for non-secured resource')
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
            return userAvailableActions(resourceClass, id)
        }

        List<String> owningResourceActions = userAvailableActions(owningSecureResourceClass, owningSecureResourceId)
        if (Utils.parentClassIsAssignableFromChild(ModelItem, resourceClass)) {
            return owningResourceActions - MODELITEM_DISALLOWED_ACTIONS
        }
        return owningResourceActions
    }

    @Override
    boolean isApplicationAdministrator() {
        applicationPermittedRoles.any { it.name == APPLICATION_ADMIN_ROLE_NAME }
    }

    @Override
    boolean isAuthenticated() {
        user && user.emailAddress != UnloggedUser.instance.emailAddress
    }

    @Override
    boolean isPending() {
        user && user.pending
    }

    GroupRole getHighestApplicationLevelAccess() {
        applicationPermittedRoles.sort().first()
    }

    GroupRole findHighestAccessToSecurableResource(String securableResourceDomainType, UUID securableResourceId) {
        Set<VirtualSecurableResourceGroupRole> found = virtualSecurableResourceGroupRoles.findAll {
            it.domainType == securableResourceDomainType && it.domainId == securableResourceId
        }
        if (!found) return []
        found.sort().first().groupRole
    }

    GroupRole findHighestAssignedAccessToSecurableResource(String securableResourceDomainType, UUID securableResourceId) {
        List<SecurableResourceGroupRole> found = securableResourceGroupRoles.findAll {
            it.securableResourceDomainType == securableResourceDomainType && it.securableResourceId == securableResourceId
        }
        if (!found) return null
        found.collect { it.groupRole }.sort().first()
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

        if (Utils.parentClassIsAssignableFromChild(Container, securableResourceClass)) {
            if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)) {
                return CONTAINER_CONTAINER_ADMIN_ACTIONS
            }
            if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)) {
                return CONTAINER_EDITOR_ACTIONS
            }
            if (getSpecificLevelAccessToSecuredResource(securableResourceClass, id, READER_ROLE_NAME)) {
                return READ_ONLY_ACTIONS
            }
            return []
        }

        if (Utils.parentClassIsAssignableFromChild(Model, securableResourceClass)) {
            VirtualSecurableResourceGroupRole role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, CONTAINER_ADMIN_ROLE_NAME)
            if (role) {
                return MODEL_CONTAINER_ADMIN_ACTIONS - getFinalisedActionsToRemove(role) - getEditorVersioningActionsToRemove(role)
            }
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, EDITOR_ROLE_NAME)
            if (role) {
                return (MODEL_EDITOR_ACTIONS + getCanBeFinalisedAction(role)) - getFinalisedActionsToRemove(role) -
                       getEditorVersioningActionsToRemove(role)
            }
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, AUTHOR_ROLE_NAME)
            if (role) {
                return (MODEL_AUTHOR_ACTIONS + getCanBeFinalisedAction(role)) - getFinalisedActionsToRemove(role) -
                       getEditorVersioningActionsToRemove(role)
            }
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, REVIEWER_ROLE_NAME)
            if (role) {
                return MODEL_REVIEWER_ACTIONS - getReaderVersioningActionsToRemove(role)
            }
            role = getSpecificLevelAccessToSecuredResource(securableResourceClass, id, READER_ROLE_NAME)
            if (role) {
                return MODEL_READER_ACTIONS - getReaderVersioningActionsToRemove(role)
            }
            return []
        }
        log.warn('Attempt to gain available actions for unknown secured class {} id {} to {}', securableResourceClass.simpleName, id)
        []
    }

    private List<String> getFinalisedActionsToRemove(VirtualSecurableResourceGroupRole role) {
        role.isFinalisedModel() ? MODEL_DISALLOWED_FINALISED_ACTIONS : []
    }

    private List<String> getCanBeFinalisedAction(VirtualSecurableResourceGroupRole role) {
        //If resource can be finalised return FINALISE_ACTION
        role.canFinaliseModel() ? [FINALISE_ACTION] : []

    }

    private List<String> getEditorVersioningActionsToRemove(VirtualSecurableResourceGroupRole role) {
        !role.isFinalisedModel() ? MODEL_DISALLOWED_EDITOR_VERSIONING_ACTIONS : []
    }

    private List<String> getReaderVersioningActionsToRemove(VirtualSecurableResourceGroupRole role) {
        !role.isFinalisedModel() ? MODEL_READER_VERSIONING_ACTIONS : []
    }

    private boolean hasAnyAccessToSecuredResource(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        if (!id) {
            log.warn('Checking for any access to secured resource class {} without providing an ID', securableResourceClass.simpleName)

            // No id means indexing endpoint
            // Users and Groups can be indexed with show actions by the bottom layer of application roles
            if (Utils.parentClassIsAssignableFromChild(UserGroup, securableResourceClass)) {
                return hasApplicationLevelRole(CONTAINER_GROUP_ADMIN_ROLE_NAME)
            }

            if (Utils.parentClassIsAssignableFromChild(CatalogueUser, securableResourceClass)) {
                return hasApplicationLevelRole(CONTAINER_GROUP_ADMIN_ROLE_NAME)
            }
            log.info("No id access for read rights for {}, default response is false", securableResourceClass)
            //        return virtualSecurableResourceGroupRoles.any { it.domainType == securableResourceClass.simpleName }
            return false
        }

        return virtualSecurableResourceGroupRoles.any {
            it.matchesDomainResource(securableResourceClass, id)
        }
    }

    private VirtualSecurableResourceGroupRole getSpecificLevelAccessToSecuredResource(Class<? extends SecurableResource> securableResourceClass,
                                                                                      UUID id,
                                                                                      String roleName) {
        virtualSecurableResourceGroupRoles.find {
            it.matchesDomainResource(securableResourceClass, id) && it.matchesGroupRole(roleName)
        }
    }

    private boolean hasApplicationLevelRole(String rolename) {
        applicationPermittedRoles.any { it.name == rolename }
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
