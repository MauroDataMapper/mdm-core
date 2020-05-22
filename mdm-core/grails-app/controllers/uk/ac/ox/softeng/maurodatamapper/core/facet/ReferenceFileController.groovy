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
package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.controller.EditLoggingController

import grails.web.mime.MimeType
import org.grails.web.servlet.mvc.GrailsWebRequest

class ReferenceFileController extends EditLoggingController<ReferenceFile> {
    static responseFormats = ['json', 'xml']

    ReferenceFileService referenceFileService

    ReferenceFileController() {
        super(ReferenceFile)
    }

    @Override
    def show() {
        def resource = queryForResource(params.id)
        if (resource) {
            return render(file: resource.fileContents, fileName: resource.fileName, contentType: resource.contentType)
        }
        return notFound()
    }

    @Override
    protected ReferenceFile queryForResource(Serializable resourceId) {
        return referenceFileService.findByCatalogueItemIdAndId(params.catalogueItemId, resourceId)
    }

    @Override
    protected List<ReferenceFile> listAllReadableResources(Map params) {
        return referenceFileService.findAllByCatalogueItemId(params.catalogueItemId, params)
    }

    @Override
    protected ReferenceFile createResource() {
        ReferenceFile resource = super.createResource() as ReferenceFile
        resource.determineFileType()
        resource.catalogueItem = referenceFileService.findCatalogueItemByDomainTypeAndId(params.catalogueItemDomainType, params.catalogueItemId)
        resource
    }

    @Override
    void serviceDeleteResource(ReferenceFile resource) {
        referenceFileService.delete(resource)
    }

    @Override
    protected ReferenceFile saveResource(ReferenceFile resource) {
        resource.save flush: true, validate: false
        referenceFileService.addCreatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected ReferenceFile updateResource(ReferenceFile resource) {
        resource.save flush: true, validate: false
        referenceFileService.addUpdatedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }

    @Override
    protected void deleteResource(ReferenceFile resource) {
        serviceDeleteResource(resource)
        referenceFileService.addDeletedEditToCatalogueItem(currentUser, resource, params.catalogueItemDomainType, params.catalogueItemId)
    }


    @Override
    protected Object getObjectToBind() {
        if (request.contentType.startsWith(MimeType.MULTIPART_FORM.name)) {
            GrailsWebRequest grailsWebRequest = GrailsWebRequest.lookup(request)
            Map object = grailsWebRequest.params
            return object
        }
        request
    }
}
