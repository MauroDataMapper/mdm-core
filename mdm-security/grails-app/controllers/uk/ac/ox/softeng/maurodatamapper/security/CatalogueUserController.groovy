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
package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController
import uk.ac.ox.softeng.maurodatamapper.core.email.EmailService
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.SearchParams
import uk.ac.ox.softeng.maurodatamapper.security.policy.GroupBasedSecurityPolicyManagerService
import uk.ac.ox.softeng.maurodatamapper.security.rest.transport.ChangePassword
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.http.HttpStatus

import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_ADMIN_CONFIRM_REGISTRATION_BODY
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_ADMIN_CONFIRM_REGISTRATION_SUBJECT
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_ADMIN_REGISTER_BODY
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_ADMIN_REGISTER_SUBJECT
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_FORGOTTEN_PASSWORD_BODY
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_FORGOTTEN_PASSWORD_SUBJECT
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_PASSWORD_RESET_BODY
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_PASSWORD_RESET_SUBJECT
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_SELF_REGISTER_BODY
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.EMAIL_SELF_REGISTER_SUBJECT
import static uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum.SITE_URL

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

@Slf4j
class CatalogueUserController extends EditLoggingController<CatalogueUser> /* implements RestResponder */ {
    static responseFormats = ['json', 'xml']

    static allowedMethods = [pending              : 'GET',
                             userExists           : 'GET',
                             userPreferences      : 'GET',
                             adminRegister        : 'POST',
                             advancedSearch       : 'POST',
                             search               : ['POST', 'GET'],
                             changePassword       : ['POST', 'PUT'],
                             approveRegistration  : ['GET', 'PUT', 'POST'],
                             makeAdministrator    : ['GET', 'PUT'],
                             revokeAdministrator  : ['GET', 'PUT'],
                             inviteToEditModel    : ['POST', 'PUT'],
                             inviteToViewModel    : ['POST', 'PUT'],
                             profileImage         : 'GET',
                             updateProfileImage   : ['POST', 'PUT'],
                             removeProfileImage   : ['POST', 'PUT', 'DELETE'],
                             sendPasswordResetLink: 'GET'
    ]

    private CatalogueUser internalCurrentUser

    CatalogueUserService catalogueUserService
    EmailService emailService

    ApiPropertyService apiPropertyService

    GroupBasedSecurityPolicyManagerService groupBasedSecurityPolicyManagerService
    MauroDataMapperServiceProviderService mauroDataMapperServiceProviderService

    CatalogueUserController() {
        super(CatalogueUser)
    }

    @Override
    protected boolean validateResource(CatalogueUser instance, String view) {
        // Need to make sure groups are properly setup, many-to-many relationships don't get set properly during binding
        // This will correctly mark up the groups value if it is actually dirty
        instance.markDirty('groups', instance.groups, instance.getOriginalValue('groups'))
        // This will then make sure the groups actually have a record of the user inside them
        // Which will allow the save to persist the membership
        if (instance.hasChanged('groups')) {
            instance.groups.each {group ->
                if (!group.hasMember(instance)) group.addToGroupMembers(instance)
            }
        }
        return super.validateResource(instance, view)
    }

    @Override
    protected CatalogueUser createResource() {
        CatalogueUser instance = super.createResource() as CatalogueUser
        instance.createdBy = instance.emailAddress
        instance.pending = true
        instance
    }

    @Override
    protected CatalogueUser queryForResource(Serializable id) {
        catalogueUserService.get(id)
    }

    @Override
    protected List<CatalogueUser> listAllReadableResources(Map params) {
        if (params.containsKey('userGroupId')) {
            params.groupsContent = true
            return catalogueUserService.findAllByUserAndUserGroupId(currentUserSecurityPolicyManager, Utils.toUuid(params.userGroupId), params)
        }
        catalogueUserService.findAllByUser(currentUserSecurityPolicyManager, params)
    }

    @Override
    void serviceDeleteResource(CatalogueUser resource) {
        catalogueUserService.delete(resource, params.boolean('permanent') ?: false)
    }

    @Override
    protected CatalogueUser saveResource(CatalogueUser resource) {
        CatalogueUser catalogueUser = super.saveResource(resource)
        groupBasedSecurityPolicyManagerService.refreshAllUserSecurityPolicyManagersBySecurableResource(catalogueUser, getCurrentUser())
        catalogueUser
    }

    @Override
    protected void saveResponse(CatalogueUser instance) {
        emailService.sendEmailToUser(siteUrl, EMAIL_SELF_REGISTER_SUBJECT, EMAIL_SELF_REGISTER_BODY, instance)
        super.saveResponse(instance)
    }

    @Override
    protected void deleteResponse(CatalogueUser instance) {
        if (params.boolean('permanent')) {
            super.deleteResponse(instance)
        } else {
            super.updateResponse(instance)
        }
    }

    @Override
    CatalogueUser getCurrentUser() {
        if (!internalCurrentUser) {
            internalCurrentUser = catalogueUserService.findOrCreateUserFromInterface(currentUserSecurityPolicyManager.getUser())
        }
        internalCurrentUser
    }

    String getSiteUrl() {
        ApiProperty property = apiPropertyService.findByApiPropertyEnum(SITE_URL)
        property ? property.value : "${webRequest.baseUrl}${webRequest.contextPath}"
    }

    @Transactional
    def sendPasswordResetLink() {

        CatalogueUser user = catalogueUserService.findByEmailAddress(params.emailAddress)

        // if no user, don't respond with notfound as this can be used to mine the DB
        // if no pwd set then also return as this means the user has not completed registration or has logged in using an alternative method
        // Only an admin user can reset a users pwd in this situation
        if (!user || !user.password) return done()

        // As this requires calls to database we only want to call it once in the method
        String siteUrlString = siteUrl

        String passwordResetLink = catalogueUserService.generatePasswordResetLink(user, siteUrlString)

        updateResource user

        emailService.sendEmailToUser(siteUrlString, EMAIL_FORGOTTEN_PASSWORD_SUBJECT,
                                     EMAIL_FORGOTTEN_PASSWORD_BODY, user, passwordResetLink)
        // Dont respond with the save response as we dont want to indicate if the user exists
        done()
    }

    def search() {
        SearchParams searchParams = SearchParams.bind(grailsApplication, getRequest())

        if (searchParams.hasErrors()) {
            respond searchParams.errors
            return
        }

        params.offset = params.offset ?: searchParams.offset
        params.max = params.max ?: searchParams.max ?: 10

        List<CatalogueUser> users = catalogueUserService.findAllWhereAnyMatchToTerm(searchParams.searchTerm, params)
        respond users, [status: HttpStatus.OK, view: 'search']
    }

    def userPreferences() {
        CatalogueUser user = queryForResource(params.catalogueUserId)
        if (!user) return notFound(params.catalogueUserId)
        render user.userPreferences ?: ''
    }

    @Transactional
    def updateUserPreferences() {
        CatalogueUser instance = queryForResource(params.catalogueUserId)

        if (!instance) return notFound(params.catalogueUserId)

        try {
            instance.userPreferences = request.inputStream.text
        } catch (Exception ex) {
            throw new ApiBadRequestException('CUCXX', 'Could not read the text', ex)
        }

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    @Transactional
    def resetPassword() {
        // Transactional means we can't use command objects but must bind manually
        ChangePassword changePassword = new ChangePassword()
        bindData changePassword, getObjectToBind()
        internalChangePassword(changePassword)
    }

    @Transactional
    def changePassword() {
        // Transactional means we can't use command objects but must bind manually
        ChangePassword changePassword = new ChangePassword()
        bindData changePassword, getObjectToBind()
        internalChangePassword(changePassword)
    }

    def pending() {
        if (params.all) removePaginationParameters()
        respond catalogueUserService.findAllPendingUsers(params), [model: [userSecurityPolicyManager: currentUserSecurityPolicyManager]]
    }

    def pendingCount() {
        if (params.all) removePaginationParameters()
        respond pendingCount: catalogueUserService.countPendingUsers(params)
    }

    def userExists() {
        respond exists: catalogueUserService.emailAddressExists(params.emailAddress)
    }

    @Transactional
    def createInitialAdminUser() {
        internalCurrentUser = null
        def instance = catalogueUserService.createInitialAdminUser(createResource())
        if (!validateResource(instance, 'create')) return
        saveResource instance
        if (instance.tempPassword) {
            log.warn('Initial user had no password set, sending an email with a temporary password')
            emailService.sendEmailToUser(siteUrl, EMAIL_ADMIN_REGISTER_SUBJECT, EMAIL_ADMIN_REGISTER_BODY, instance)
        }
        super.saveResponse(instance)
    }

    @Transactional
    def adminRegister() {
        internalCurrentUser = null
        if (handleReadOnly()) return

        def instance = catalogueUserService.administratorRegisterNewUser(getCurrentUser(), createResource())

        if (response.isCommitted()) return

        if (!validateResource(instance, 'create')) return

        saveResource instance

        emailService.sendEmailToUser(siteUrl, EMAIL_ADMIN_REGISTER_SUBJECT, EMAIL_ADMIN_REGISTER_BODY, instance)
        super.saveResponse instance
    }

    @Transactional
    def adminPasswordReset() {
        internalCurrentUser = null
        CatalogueUser user = queryForResource(params.catalogueUserId)

        if (!user) return notFound(params.catalogueUserId)

        def instance = catalogueUserService.administratorPasswordReset(getCurrentUser(), user)

        updateResource instance

        emailService.sendEmailToUser(siteUrl, EMAIL_PASSWORD_RESET_SUBJECT, EMAIL_PASSWORD_RESET_BODY, instance)

        updateResponse instance
    }

    @Transactional
    def approveRegistration() {
        internalCurrentUser = null
        CatalogueUser user = queryForResource(params.catalogueUserId)

        if (!user) return notFound(params.catalogueUserId)

        def instance = catalogueUserService.approveUserRegistration(getCurrentUser(), user)

        if (!validateResource(instance, 'update')) return

        updateResource instance

        emailService.sendEmailToUser(siteUrl, EMAIL_ADMIN_CONFIRM_REGISTRATION_SUBJECT, EMAIL_ADMIN_CONFIRM_REGISTRATION_BODY, instance)

        updateResponse instance
    }

    @Transactional
    def rejectRegistration() {
        internalCurrentUser = null
        CatalogueUser user = queryForResource(params.catalogueUserId)

        if (!user) return notFound(params.catalogueUserId)

        def instance = catalogueUserService.rejectUserRegistration(getCurrentUser(), user)

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

    def exportUsers(Integer max) {
        params.max = Math.min(max ?: 10, 10000)
        List<CatalogueUser> users = listAllResources(params)

        //log.info("Exporting all Users using ${exporter.displayName}")
        ByteArrayOutputStream outputStream = catalogueUserService.convertToCsv(users)
        //log.info('Export complete')
        if (!outputStream) {
            return errorResponse(UNPROCESSABLE_ENTITY, 'Users could not be exported')
        }

        render(file: outputStream.toByteArray(), fileName: 'mauroDataMapperUsers.csv', contentType: 'text/csv')
    }

    /**
     * Change the user password using either a not logged in and reset token or logged in and the old password
     *
     * @param changePassword
     * @return
     */
    @Transactional
    private def internalChangePassword(ChangePassword changePassword) {
        internalCurrentUser = null

        changePassword.validate()

        if (changePassword.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond changePassword.errors
            return null
        }

        CatalogueUser instance = queryForResource(params.catalogueUserId)

        if (!instance) return notFound(params.catalogueUserId)

        if (currentUserSecurityPolicyManager.isAuthenticated() && changePassword.oldPassword) {
            catalogueUserService.changeUserPassword(getCurrentUser(), instance, changePassword.oldPassword, changePassword.newPassword)
        } else if (!currentUserSecurityPolicyManager.isAuthenticated() && changePassword.resetToken) {
            catalogueUserService.changeUserPassword(instance, changePassword.resetToken, changePassword.newPassword)
        } else {
            return unauthorised('Invalid credentials')
        }

        if (!validateResource(instance, 'update')) return

        updateResource instance

        updateResponse instance
    }

}
