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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.facet


import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec

import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse

import java.util.regex.Pattern

import static io.micronaut.http.HttpStatus.CREATED

/**
 * <pre>
 * Controller: versionLink
 *  |  POST    | /api/${modelDomainType}/${modelId}/versionLinks        | Action: save
 *  |  GET     | /api/${modelDomainType}/${modelId}/versionLinks        | Action: index
 *  |  DELETE  | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: delete
 *  |  PUT     | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: update
 *  |  GET     | /api/${modelDomainType}/${modelId}/versionLinks/${id}  | Action: show
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkController
 */
@Integration
@Slf4j
abstract class ModelVersionLinkFunctionalSpec extends UserAccessFunctionalSpec {

    abstract String getModelDomainType()

    abstract String getModelId()

    abstract String getModelJsonString()

    abstract String getTargetModelId()

    abstract String getTargetModelDomainType()

    abstract String getTargetModelJsonString()

    @Override
    String getResourcePath() {
        "${getModelDomainType()}/${getModelId()}/versionLinks"
    }

    @Override
    String getEditsFullPath(String id) {
        "${getModelDomainType()}/${getModelId()}"
    }

    @Override
    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, getModelId()
    }

    @Override
    void verifyR04UnknownIdResponse(HttpResponse<Map> response, String id) {
        verifyForbidden response
    }

    @Override
    void verifySameValidDataCreationResponse() {
        verifyResponse CREATED, response
    }

    @Override
    void verifyE03ValidResponseBody(HttpResponse<Map> response) {
        assert response.body().id
        assert response.body().linkType == 'New Fork Of'
        assert response.body().domainType == 'VersionLink'
        assert response.body().sourceModel.id == getModelId()
        assert response.body().targetModel.id == getTargetModelId()
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[VersionLink:NEW_FORK_OF:.+?] added to component \[.+?]/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[VersionLink:NEW_DOCUMENTATION_VERSION_OF:.+?] changed properties \[path, linkType]/
    }

    @Override
    Map getValidJson() {
        [
            linkType             : VersionLinkType.NEW_FORK_OF.label,
            targetModelId        : getTargetModelId(),
            targetModelDomainType: getTargetModelDomainType()
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            linkType   : VersionLinkType.NEW_FORK_OF.label,
            targetModel: getModelId().toString(),

        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            linkType: VersionLinkType.NEW_DOCUMENTATION_VERSION_OF.label,
        ]
    }


    @Override
    String getEditorIndexJson() {
        '''{
  "count": 0,
  "items": [
    
  ]
}'''
    }

    @Override
    String getShowJson() {
        '''{
  "id": "${json-unit.matches:id}",
  "linkType": "New Fork Of",
  "domainType": "VersionLink",
  "sourceModel": ''' + getModelJsonString() + ''',
  "targetModel": ''' + getTargetModelJsonString() + '''
}'''
    }

}