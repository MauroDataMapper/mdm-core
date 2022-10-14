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
package uk.ac.ox.softeng.maurodatamapper.security.role

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiBadRequestException
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MdmDomainService
import uk.ac.ox.softeng.maurodatamapper.security.CatalogueUser
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResourceService
import uk.ac.ox.softeng.maurodatamapper.security.UserGroup

import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import org.springframework.beans.factory.annotation.Autowired

@Transactional
class SecurableResourceGroupRoleService implements MdmDomainService<SecurableResourceGroupRole> {

    @Autowired(required = false)
    List<SecurableResourceService> securableResourceServices

    @Autowired(required = false)
    List<ModelService> modelServices

    SecurableResourceGroupRole get(Serializable id) {
        SecurableResourceGroupRole.get(id)
    }

    @Override
    List<SecurableResourceGroupRole> getAll(Collection<UUID> resourceIds) {
        SecurableResourceGroupRole.getAll(resourceIds)
    }

    List<SecurableResourceGroupRole> list(Map pagination) {
        SecurableResourceGroupRole.list(pagination)
    }

    Long count() {
        SecurableResourceGroupRole.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(SecurableResourceGroupRole securableResourceGroupRole) {
        securableResourceGroupRole.delete(flush: true)
    }

    @Override
    SecurableResourceGroupRole findByParentIdAndPathIdentifier(UUID parentId, String pathIdentifier, Map pathParams = [:]) {
        return null
    }

    void deleteAllForSecurableResource(SecurableResource securableResource) {
        SecurableResourceGroupRole.bySecurableResource(securableResource.domainType, securableResource.resourceId).deleteAll()
    }

    void deleteAllBySecurableResourceDomainTypeAndIds(securableResourceDomainType, Collection<UUID> ids) {
        SecurableResourceGroupRole.bySecurableResourceDomainTypeAndSecurableResourceIdInList(securableResourceDomainType, ids).deleteAll()
    }

    SecurableResourceGroupRole createAndSaveSecurableResourceGroupRole(SecurableResource securableResource, GroupRole groupRole, UserGroup userGroup,
                                                                       CatalogueUser createdBy) {
        SecurableResourceGroupRole securableResourceGroupRole = new SecurableResourceGroupRole(
            securableResource: securableResource,
            createdBy: createdBy.emailAddress
        )
        userGroup.addToSecurableResourceGroupRoles(securableResourceGroupRole)
        updateAndSaveSecurableResourceGroupRole(securableResourceGroupRole, groupRole)
    }

    SecurableResourceGroupRole updateAndSaveSecurableResourceGroupRole(SecurableResourceGroupRole securableResourceGroupRole,
                                                                       GroupRole groupRole) {

        groupRole.addToSecuredResourceGroupRoles(securableResourceGroupRole)

        if (!securableResourceGroupRole.validate()) {
            throw new ValidationException('Could not create new SecurableResourceGroupRole', securableResourceGroupRole.errors)
        }
        securableResourceGroupRole.save(flush: true, validate: false)
    }

    SecurableResourceGroupRole findBySecurableResourceAndId(String securableResourceDomainType, UUID securableResourceId, UUID id) {
        // We have to handle extending where the domaintype of folder may not be the domain type of the actual resource id as its a VF
        SecurableResource sr = securableResourceDomainType == 'Folder' ? findSecurableResource(securableResourceDomainType, securableResourceId) : null
        SecurableResourceGroupRole.bySecurableResourceAndId(sr?.domainType ?: securableResourceDomainType,
                                                            sr?.resourceId ?: securableResourceId, id).get()
    }

    SecurableResourceGroupRole findByUserGroupIdAndId(UUID userGroupId, UUID id) {
        SecurableResourceGroupRole.byUserGroupIdAndId(userGroupId, id).get()
    }

    SecurableResourceGroupRole findBySecurableResourceAndUserGroup(String securableResourceDomainType, UUID securableResourceId, UUID userGroupId) {
        // We have to handle extending where the domaintype of folder may not be the domain type of the actual resource id as its a VF
        SecurableResource sr = securableResourceDomainType == 'Folder' ? findSecurableResource(securableResourceDomainType, securableResourceId) : null
        SecurableResourceGroupRole.bySecurableResourceAndUserGroupId(sr?.domainType ?: securableResourceDomainType,
                                                                     sr?.resourceId ?: securableResourceId, userGroupId).get()
    }

    SecurableResourceGroupRole findBySecurableResourceAndGroupRoleIdAndUserGroupId(String securableResourceDomainType, UUID securableResourceId,
                                                                                   UUID groupRoleId, UUID userGroupId) {
        // We have to handle extending where the domaintype of folder may not be the domain type of the actual resource id as its a VF
        SecurableResource sr = securableResourceDomainType == 'Folder' ? findSecurableResource(securableResourceDomainType, securableResourceId) : null
        SecurableResourceGroupRole.bySecurableResourceAndGroupRoleIdAndUserGroupId(sr?.domainType ?: securableResourceDomainType,
                                                                                   sr?.resourceId ?: securableResourceId,
                                                                                   groupRoleId,
                                                                                   userGroupId).get()
    }

    List<SecurableResourceGroupRole> findAllByUserGroupId(UUID userGroupId, Map pagination = [:]) {
        SecurableResourceGroupRole.byUserGroupId(userGroupId).list(pagination)
    }

    List<SecurableResourceGroupRole> findAllByUserGroupIds(List<UUID> userGroupIds) {
        SecurableResourceGroupRole.byUserGroupIds(userGroupIds).list()
    }

    List<SecurableResourceGroupRole> findAllBySecurableResource(String securableResourceDomainType, UUID securableResourceId, Map pagination = [:]) {
        // We have to handle extending where the domaintype of folder may not be the domain type of the actual resource id as its a VF
        SecurableResource sr = securableResourceDomainType == 'Folder' ? findSecurableResource(securableResourceDomainType, securableResourceId) : null
        SecurableResourceGroupRole.bySecurableResource(sr?.domainType ?: securableResourceDomainType,
                                                       sr?.resourceId ?: securableResourceId).list(pagination)
    }

    List<SecurableResourceGroupRole> findAllBySecurableResourceDomainType(String securableResourceDomainType) {
        SecurableResourceGroupRole.bySecurableResourceDomainType(securableResourceDomainType).list()
    }

    List<SecurableResourceGroupRole> findAllBySecurableResourceAndGroupRoleId(String securableResourceDomainType, UUID securableResourceId,
                                                                              UUID groupRoleId, Map pagination = [:]) {
        // We have to handle extending where the domaintype of folder may not be the domain type of the actual resource id as its a VF
        SecurableResource sr = securableResourceDomainType == 'Folder' ? findSecurableResource(securableResourceDomainType, securableResourceId) : null
        SecurableResourceGroupRole.bySecurableResourceAndGroupRoleId(sr?.domainType ?: securableResourceDomainType,
                                                                     sr?.resourceId ?: securableResourceId,
                                                                     groupRoleId).list(pagination)
    }

    // There are valid situations in which there may be no securable resource service found, so let the caller
    // of this method decide whether an exception should be thrown, or a null value returned
    def <R extends SecurableResource> R findSecurableResource(Class<R> clazz, UUID id, boolean silenceException = false) {
        SecurableResourceService service = securableResourceServices.find { it.handles(clazz) }
        if (!service) {
            if (silenceException) {
                return null
            } else {
                throw new ApiBadRequestException('SRGRS01',
                                             "SecurableResourceGroupRole retrieval for securable resource [${clazz.simpleName}] with no " +
                                             'supporting service')
            }
        }
        service.get(id)
    }

    def <R extends SecurableResource> R findSecurableResource(String domainType, UUID id) {
        SecurableResourceService service = securableResourceServices.find { it.handles(domainType) }
        if (!service) throw new ApiBadRequestException('SRGRS02',
                                                       "SecurableResourceGroupRole retrieval for securable resource [${domainType}] with no " +
                                                       'supporting service')
        service.get(id)
    }
}
