/*
 * Copyright 2020-2024 University of Oxford and NHS England
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

import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.MdmDomainConstraints
import uk.ac.ox.softeng.maurodatamapper.security.authentication.ApiKey
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecureRandomStringGenerator
import uk.ac.ox.softeng.maurodatamapper.security.utils.SecurityUtils
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import grails.rest.Resource

import java.security.Principal
import java.time.OffsetDateTime

@Resource(readOnly = false, formats = ['json', 'xml'])
class CatalogueUser implements Principal, MdmDomain, EditHistoryAware, User {

    UUID id
    String emailAddress
    String firstName
    String lastName
    OffsetDateTime lastLogin
    String organisation

    @BindUsing({obj, source ->
        SecurityUtils.getHash(source['password'] as String, obj.salt)
    })
    byte[] password
    UserImageFile profilePicture
    String jobTitle
    byte[] salt
    String tempPassword
    Boolean pending
    String userPreferences
    Boolean disabled
    UUID resetToken
    String creationMethod

    static belongsTo = UserGroup

    static hasMany = [
        groups : UserGroup,
        apiKeys: ApiKey
    ]

    static constraints = {
        CallableConstraints.call(MdmDomainConstraints, delegate)
        emailAddress email: true, unique: true, blank: false
        pending nullable: false
        firstName blank: false
        lastName blank: false
        profilePicture nullable: true
        organisation nullable: true
        jobTitle nullable: true
        userPreferences nullable: true
        password nullable: true
        tempPassword nullable: true
        lastLogin nullable: true
        disabled nullable: false
        resetToken nullable: true
        apiKeys minSize: 0
    }

    static mapping = {
        groups joinTable: 'join_catalogue_user_to_user_group', index: 'jcutug_catalogue_user_idx'
        profilePicture lazy: true, index: 'catalogue_user_profile_picture_idx', cascade: 'all-delete-orphan'
        userPreferences type: 'text'
    }

    CatalogueUser() {
        salt = SecureRandomStringGenerator.generateSalt()
    }

    void setEmailAddress(String emailAddress) {
        this.emailAddress = SecurityUtils.normaliseEmailAddress(emailAddress)
    }

    void encryptAndSetPassword(String password) {
        this.resetToken = null
        this.tempPassword = null
        this.password = SecurityUtils.getHash(password, salt)
        markDirty('resetToken')
        markDirty('password')
        markDirty('tempPassword')
    }

    def beforeValidate() {
        checkPath()
        if (!creationMethod) creationMethod = 'Standard'
        if (pending == null) pending = false
        if (disabled == null) disabled = false
    }

    Boolean ownsItem(MdmDomain item) {
        item.createdBy == emailAddress
    }

    Boolean isInGroup(UserGroup group) {
        groups.any {it.name == group.name}
    }

    String toString() {
        "${getClass().getName()} (${emailAddress})[${id ?: '(unsaved)'}]"
    }

    @Override
    String getEditLabel() {
        "CatalogueUser:${id}"
    }

    @Override
    String getName() {
        emailAddress
    }

    @Override
    String getDomainType() {
        CatalogueUser.simpleName
    }

    @Override
    String getPathPrefix() {
        'cu'
    }

    @Override
    String getPathIdentifier() {
        emailAddress
    }

    boolean isDisabled() {
        disabled
    }

    boolean isPending() {
        pending
    }

    boolean getNeedsToResetPassword() {
        tempPassword
    }

    static CatalogueUser findByEmailAddress(String emailAddress) {
        by().eq('emailAddress', SecurityUtils.normaliseEmailAddress(emailAddress)).get()
    }

    static DetachedCriteria<CatalogueUser> whereAnyMap(Map<String, Object> searchParams) {
        by().or {
            searchParams.findAll {k, v -> v}.each {
                ilike(it.key, "%${it.value}%")
            }
        }
    }

    static DetachedCriteria<CatalogueUser> whereMap(Map<String, Object> searchParams) {
        by().and {searchParams.findAll {k, v -> v}.each {ilike(it.key, "%${it.value}%")}}
    }

    static DetachedCriteria<CatalogueUser> whereAnyMatchToTerm(String searchTerm) {
        whereAnyMap([
            emailAddress: searchTerm,
            lastName    : searchTerm,
            firstName   : searchTerm,
            organisation: searchTerm,
            jobTitle    : searchTerm
        ])
    }

    static DetachedCriteria<CatalogueUser> by() {
        new DetachedCriteria<CatalogueUser>(CatalogueUser)
    }

    //    static DetachedCriteria<CatalogueUser> byAdministratorOrId(Serializable id) {
    //        by().or {
    //            eq('userRole', UserRole.ADMINISTRATOR)
    //            idEq(toUuid(id))
    //        }
    //    }

    static DetachedCriteria<CatalogueUser> byUserGroupId(UUID userGroupId) {
        where {
            groups {
                eq 'id', userGroupId
            }
        }

    }

    static DetachedCriteria<CatalogueUser> byUserGroupIdAndNotDisabled(UUID userGroupId) {
        byUserGroupId(userGroupId).eq('disabled', false)
    }

    static DetachedCriteria<CatalogueUser> byPending() {
        by().eq('pending', true)
    }

    static DetachedCriteria<CatalogueUser> byIdInList(List<UUID> ids) {
        by().inList('id', ids)
    }

    static DetachedCriteria<CatalogueUser> withFilter(Map filters, DetachedCriteria<CatalogueUser> criteria = by()) {
        if (filters.emailAddress) criteria = criteria.ilike('emailAddress', "%${filters.emailAddress}%")
        if (filters.firstName) criteria = criteria.ilike('firstName', "%${filters.firstName}%")
        if (filters.lastName) criteria = criteria.ilike('lastName', "%${filters.lastName}%")
        if (filters.organisation) criteria = criteria.ilike('organisation', "%${filters.organisation}%")
        if (filters.jobTitle) criteria = criteria.ilike('jobTitle', "%${filters.jobTitle}%")
        if (filters.disabled) criteria = criteria.eq('disabled', filters.disabled)
        criteria
    }

    static CatalogueUser getAdminUser() {
        findByEmailAddress('admin@maurodatamapper.com')
    }

    static CatalogueUser fromInterface(User user) {
        new CatalogueUser().with {
            emailAddress = user.emailAddress
            firstName = user.firstName
            lastName = user.lastName
            tempPassword = user.tempPassword
            createdBy = user.emailAddress
            if (validate()) save()
            it
        }
    }
}