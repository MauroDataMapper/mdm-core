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
package uk.ac.ox.softeng.maurodatamapper.core.controller


import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.MultiFacetItemAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService

/**
 * @since 25/02/2021
 */
abstract class FacetController<T extends MultiFacetItemAware> extends EditLoggingController<T> {

    FacetController(Class<T> resource) {
        super(resource)
    }

    FacetController(Class<T> resource, boolean readOnly) {
        super(resource, readOnly)
    }

    abstract MultiFacetItemAwareService getFacetService()

    String getOwnerDomainTypeField() {
        'multiFacetAwareItemDomainType'
    }

    String getOwnerIdField() {
        'multiFacetAwareItemId'
    }

    @Override
    protected T queryForResource(Serializable resourceId) {
        getFacetService().findByMultiFacetAwareItemIdAndId(params[getOwnerIdField()], resourceId)
    }

    @Override
    protected List<T> listAllReadableResources(Map params) {
        getFacetService().findAllByMultiFacetAwareItemId(params[getOwnerIdField()] as UUID, params)
    }

    @Override
    protected T createResource() {
        T resource = super.createResource() as T
        resource.clearErrors()
        getFacetService().addFacetToDomain(resource, params[getOwnerDomainTypeField()], params[getOwnerIdField()])
        resource
    }

    @Override
    void serviceDeleteResource(T resource) {
        getFacetService().delete(resource, true)
    }

    @Override
    protected T saveResource(T resource) {
        resource.save flush: true, validate: false
        getFacetService().saveMultiFacetAwareItem(resource)
        getFacetService().addCreatedEditToMultiFacetAwareItem(currentUser, resource, params[getOwnerDomainTypeField()], params[getOwnerIdField()])
    }

    @Override
    protected T updateResource(T resource) {
        List<String> dirtyPropertyNames = resource.getDirtyPropertyNames()
        resource.save flush: true, validate: false
        getFacetService().
            addUpdatedEditToMultiFacetAwareItem(currentUser, resource, params[getOwnerDomainTypeField()], params[getOwnerIdField()],
                                                dirtyPropertyNames)
    }

    @Override
    protected void deleteResource(T resource) {
        serviceDeleteResource(resource)
        getFacetService().addDeletedEditToMultiFacetAwareItem(currentUser, resource, params[getOwnerDomainTypeField()], params[getOwnerIdField()])
    }
}
