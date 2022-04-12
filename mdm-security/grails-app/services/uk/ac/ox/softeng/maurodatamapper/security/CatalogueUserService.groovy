/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.core.bootstrap.StandardEmailAddress
import uk.ac.ox.softeng.maurodatamapper.core.facet.EditTitle
import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFileService
import uk.ac.ox.softeng.maurodatamapper.core.security.UserService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.AnonymisableService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.security.rest.transport.UserProfilePicture
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityUtils

import grails.gorm.transactions.Transactional
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.springframework.beans.factory.annotation.Autowired

import java.security.NoSuchAlgorithmException
import java.time.OffsetDateTime

@Transactional
class CatalogueUserService implements UserService, MdmDomainService<CatalogueUser> {

    UserImageFileService userImageFileService
    UserGroupService userGroupService

    @Autowired(required = false)
    List<AnonymisableService> anonymisableServices

    CatalogueUser get(Serializable id) {
        CatalogueUser.get(id) ?: id instanceof String ? findByEmailAddress(id) : null
    }

    @Override
    List<CatalogueUser> getAll(Collection<UUID> resourceIds) {
        CatalogueUser.getAll(resourceIds)
    }

    List<CatalogueUser> list(Map pagination) {
        pagination ? CatalogueUser.withFilter(pagination).join('groups').list(pagination) : CatalogueUser.list()
    }

    Long count() {
        CatalogueUser.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(CatalogueUser catalogueUser, boolean permanent = false) {
        if (permanent) {
            anonymiseAndPermanentlyDelete(catalogueUser)
        } else {
            catalogueUser.disabled = true
        }
    }

    /**
     * When it is necessary to completely scrub the user's name and email address from the database,
     * this method:
     * - Use the anonymise method on every AnonymisableService
     * - Explicitly anonymise the CatalogueUser table
     * - Removes the CatalogueUser from any UserGroups
     * - Finally, permanently delete the CatalogueUser.
     *
     * @param catalogueUser
     */
    void anonymiseAndPermanentlyDelete(CatalogueUser catalogueUser) {
        // Anonymise everything
        anonymisableServices.each {
            it.anonymise(catalogueUser.emailAddress)
        }

        anonymise(catalogueUser.emailAddress)

        // Remove user from any user groups
        catalogueUser.groups.each {group ->
            catalogueUser.removeFromGroups(group)
        }

        // Delete
        catalogueUser.delete(flush: true)
    }

    @Override
    CatalogueUser findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier) {
        return null
    }

    CatalogueUser findByEmailAddress(String emailAddress) {
        CatalogueUser.findByEmailAddress(emailAddress)
    }

    Boolean emailAddressExists(String emailAddress) {
        CatalogueUser.countByEmailAddress(emailAddress)
    }

    //    Boolean userIsInRole(CatalogueUser user, List<UserRole> roles) {
    //        user && roles.any {it == user.userRole}
    //    }

    List<CatalogueUser> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(CatalogueUser)
        ids ? CatalogueUser.withFilter(pagination, CatalogueUser.byIdInList(ids)).list(pagination) : []
    }

    CatalogueUser createNewUser(Map args) {
        log.debug("Creating new user for '${args.emailAddress}'")
        String pwd = args.remove('password')
        CatalogueUser user = new CatalogueUser(args)
        if (!user.createdBy) user.createdBy = args.emailAddress
        user.encryptAndSetPassword pwd
        user
    }

    CatalogueUser findOrCreateUserFromInterface(User user) {
        CatalogueUser catalogueUser = null
        if (user instanceof CatalogueUser || user.domainType == CatalogueUser.simpleName) {
            catalogueUser = user as CatalogueUser
            if (!catalogueUser.isAttached()) catalogueUser = null
        }

        if (!catalogueUser) {
            catalogueUser = get(user.id) ?: findByEmailAddress(user.emailAddress)
        }

        catalogueUser ?: CatalogueUser.fromInterface(user)
    }

    CatalogueUser changeUserPassword(CatalogueUser actor, CatalogueUser user, String oldPassword, String newPassword) {
        log.debug("Changing '${user.emailAddress}' password (Actor: '${actor.emailAddress}')")

        try {
            if (validateUserPassword(user, oldPassword)) {
                user.encryptAndSetPassword(newPassword)
                user.validate()
            } else if (validateTempPassword(user, oldPassword)) {
                log.warn("Replacing user's temp password")
                user.encryptAndSetPassword(newPassword)
                user.validate()
            } else {
                log.warn("Invalid attempt to change '${user.emailAddress}' password (Actor: '${actor.emailAddress}')")
                user.errors.reject('invalid.change.password.message', [user.emailAddress].toArray(),
                                   'Cannot change password for user [{0}] as old password is not valid')
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            log.error('Something went wrong', e)
        }
        user
    }

    CatalogueUser changeUserPassword(CatalogueUser user, UUID resetToken, String newPassword) {
        log.debug("Changing '${user.emailAddress}' password using reset token")

        try {
            if (user.resetToken && user.resetToken == resetToken) {
                user.encryptAndSetPassword(newPassword)
                user.validate()
            } else {
                log.warn("Invalid attempt to change '${user.emailAddress}' password")
                user.errors.reject('invalid.change.password.message', [user.emailAddress].toArray(),
                                   'Cannot change password for user [{0}] as old password is not valid')
            }
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            log.error('Something went wrong', e)
        }
        user
    }

    def administratorRegisterNewUser(CatalogueUser actor, String emailAddress, String firstName, String lastName, String role,
                                     String organisation) {
        CatalogueUser user = new CatalogueUser(emailAddress: emailAddress, firstName: firstName, lastName: lastName,
                                               organisation: organisation, jobTitle: role, createdBy: actor.emailAddress)
        if (!user.validate()) return user.errors
        administratorRegisterNewUser(actor, user)
    }

    CatalogueUser createInitialAdminUser(CatalogueUser initialAdminUser) {
        log.debug('Creating initial admin user')

        initialAdminUser.pending = false
        initialAdminUser.createdBy = StandardEmailAddress.ADMIN

        if (!initialAdminUser.password) {
            initialAdminUser.tempPassword = SecurityUtils.generateRandomPassword()
        }
        initialAdminUser.validate()

        userGroupService.createAndSaveAdministratorsGroup(initialAdminUser)

        initialAdminUser
    }

    CatalogueUser administratorRegisterNewUser(CatalogueUser actor, CatalogueUser user) {
        log.debug("Registering user '${user.emailAddress}' (Actor: '${actor.emailAddress}')")

        user.pending = false
        user.createdBy = actor.emailAddress
        user.tempPassword = SecurityUtils.generateRandomPassword()
        user.password = null
        user.validate()
        user
    }

    CatalogueUser administratorPasswordReset(CatalogueUser actor, CatalogueUser user) {
        log.debug("Resetting user '${user.emailAddress}' password(Actor: '${actor.emailAddress}')")
        user.tempPassword = SecurityUtils.generateRandomPassword()
        user.validate()
        user
    }

    CatalogueUser approveUserRegistration(CatalogueUser actor, CatalogueUser user) {
        log.debug("Approving user '${user.emailAddress}' registration (Actor: '${actor.emailAddress}')")

        if (!user.pending) {
            user.errors.reject('invalid.already.registered.message', [user.emailAddress].toArray(),
                               'Cannot approve or reject user registration for [{0}] as user is already registered')
            return user
        }
        user.pending = false
        user.validate()
        user
    }

    CatalogueUser rejectUserRegistration(CatalogueUser actor, CatalogueUser user) {
        log.debug("Rejecting user '${user.emailAddress}' registration (Actor: '${actor.emailAddress}')")

        if (!user.pending) {
            user.errors.reject('invalid.already.registered.message', [user.emailAddress].toArray(),
                               'Cannot approve or reject user registration for [{0}] as user is already registered')
            return user
        }

        user.disabled = true
        user.validate()
        user
    }

    CatalogueUser updateProfilePicture(CatalogueUser actor, CatalogueUser user, UserProfilePicture picture) {

        user.profilePicture = userImageFileService.createNewFile("${user.id}-profile", picture.getDecodedImage(), picture.type, actor)
        user.profilePicture.validate()

        user.validate()
        user
    }

    CatalogueUser removeProfilePicture(CatalogueUser actor, CatalogueUser user) {
        user.profilePicture = null
        user.validate()
        user
    }

    List<CatalogueUser> findAllWhereAnyMatchToTerm(String searchTerm, Map pagination = [:]) {
        CatalogueUser.whereAnyMatchToTerm(searchTerm).list(pagination)
    }

    List<CatalogueUser> findAllByUserAndUserGroupId(UserSecurityPolicyManager userSecurityPolicyManager, UUID userGroupId, Map pagination = [:]) {
        userSecurityPolicyManager.isApplicationAdministrator() ?
        findAllByUserGroupId(userGroupId, pagination) : findAllByUserGroupIdAndNotDisabled(userGroupId, pagination)
    }

    List<CatalogueUser> findAllByUserGroupId(UUID userGroupId, Map pagination = [:]) {
        CatalogueUser.byUserGroupId(userGroupId).list(pagination)
    }

    List<CatalogueUser> findAllByUserGroupIdAndNotDisabled(UUID userGroupId, Map pagination = [:]) {
        CatalogueUser.byUserGroupIdAndNotDisabled(userGroupId).list(pagination)
    }

    List<CatalogueUser> findAllPendingUsers(Map pagination = [:]) {
        CatalogueUser.withFilter(pagination, CatalogueUser.byPending()).list(pagination)
    }

    ByteArrayOutputStream convertToCsv(List<CatalogueUser> allUsers) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        OutputStreamWriter streamWriter = new OutputStreamWriter(outputStream)
        CSVPrinter csvPrinter = new CSVPrinter(new BufferedWriter(streamWriter), CSVFormat.DEFAULT)
        csvPrinter.printRecord('Email Address', 'First Name', 'Last Name', 'Last Login', 'Organisation', 'Job Title', 'Disabled', 'Pending')
        allUsers.each {user ->
            csvPrinter.printRecord(user.emailAddress, user.firstName, user.lastName, user.lastLogin, user.organisation, user.jobTitle,
                                   user.disabled, user.pending)
        }
        csvPrinter.close()
        streamWriter.close()
        outputStream
    }

    Long countPendingUsers(Map pagination = [:]) {
        CatalogueUser.withFilter(pagination, CatalogueUser.byPending()).count(pagination)
    }

    String generatePasswordResetLink(CatalogueUser catalogueUser, String baseUrl) {
        catalogueUser.resetToken = UUID.randomUUID()
        "${baseUrl}/#/resetpassword?uid=${catalogueUser.id}&token=${catalogueUser.resetToken.toString()}"
    }

    Boolean validateUserPassword(CatalogueUser user, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        Boolean valid = user.password ? new String(user.password, 'UTF-8') == SecurityUtils.saltPassword(password, user.salt) : false
        if (valid && user.tempPassword) {
            user.tempPassword = null
            user.save(validate: false, flush: true)
        }
        valid
    }

    Boolean validateTempPassword(CatalogueUser user, String password) {
        Boolean valid = user.tempPassword && user.tempPassword == password
        if (valid && user.password) {
            user.password = null
            user.save(validate: false, flush: true)
        }
        valid
    }

    void setUserLastLoggedIn(CatalogueUser user) {
        user.lastLogin = OffsetDateTime.now()
        if (user.resetToken) {
            user.resetToken = null
            user.save(validate: false, flush: true)
        }
        user
    }

    @Override
    User findUser(String emailAddress) {
        findByEmailAddress(emailAddress)
    }

    @Override
    UserImageFile addImageCreatedEditToUser(User creator, UserImageFile domain, UUID userId) {
        CatalogueUser user = get(userId)
        user.addToEditsTransactionally EditTitle.CREATE, creator, "[$domain.editLabel] added to component [${user.editLabel}]"


        domain
    }

    @Override
    UserImageFile addImageUpdatedEditToUser(User editor, UserImageFile domain, UUID userId, List<String> dirtyPropertyNames) {
        CatalogueUser user = get(userId)
        user.addToEditsTransactionally EditTitle.UPDATE, editor, domain.editLabel, dirtyPropertyNames
        domain
    }

    @Override
    UserImageFile addImageDeletedEditToUser(User deleter, UserImageFile domain, UUID userId) {
        CatalogueUser user = get(userId)
        user.addToEditsTransactionally EditTitle.DELETE, deleter, "[$domain.editLabel] removed from component [${user.editLabel}]"
        domain
    }
}