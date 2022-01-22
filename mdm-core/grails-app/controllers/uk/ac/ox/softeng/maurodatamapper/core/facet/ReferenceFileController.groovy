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
package uk.ac.ox.softeng.maurodatamapper.core.facet


import uk.ac.ox.softeng.maurodatamapper.core.controller.FacetController
import uk.ac.ox.softeng.maurodatamapper.core.traits.service.MultiFacetItemAwareService

import grails.web.mime.MimeType
import org.grails.web.servlet.mvc.GrailsWebRequest

class ReferenceFileController extends FacetController<ReferenceFile> {
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
        return notFound(params.id)
    }

    @Override
    MultiFacetItemAwareService getFacetService() {
        referenceFileService
    }

    @Override
    protected ReferenceFile createResource() {
        ReferenceFile resource = super.createResource() as ReferenceFile
        resource.determineFileType()
        resource
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
