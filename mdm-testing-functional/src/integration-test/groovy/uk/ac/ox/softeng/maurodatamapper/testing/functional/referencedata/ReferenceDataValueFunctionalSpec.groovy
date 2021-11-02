/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.testing.functional.referencedata

import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.bootstrap.BootstrapModels
import uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataElement
import uk.ac.ox.softeng.maurodatamapper.testing.functional.UserAccessFunctionalSpec

import grails.gorm.transactions.Transactional
import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import grails.util.BuildSettings
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpResponse
import spock.lang.Shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * <pre>
 * Controller: referenceDataValue
 * | GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues                            | Action: index
 * | POST   | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues                            | Action: save
 * | GET    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${referenceDataValueId}    | Action: show
 * | PUT    | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${referenceDataValueId}    | Action: update
 * | DELETE | /api/referenceDataModels/${referenceDataModelId}/referenceDataValues/${referenceDataValueId}    | Action: delete
 * </pre>
 * @see uk.ac.ox.softeng.maurodatamapper.referencedata.item.ReferenceDataValueController
 */
@Integration
@Slf4j
class ReferenceDataValueFunctionalSpec extends UserAccessFunctionalSpec {

    @Shared
    Path resourcesPath

    @OnceBefore
    void setupResourcesPath() {
        resourcesPath = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'src', 'integration-test', 'resources', 'referencedata').toAbsolutePath()
    }

    private byte[] loadJsonFile(String filename) {
        Path jsonFilePath = resourcesPath.resolve("${filename}.json").toAbsolutePath()
        assert Files.exists(jsonFilePath)
        Files.readAllBytes(jsonFilePath)
    }

    @Override
    String getResourcePath() {
        "referenceDataModels/$simpleReferenceDataModelId/referenceDataValues"
    }

    @Override
    String getEditsFullPath(String id) {
        "referenceDataElements/$referenceDataElementId"
    }

    @Override
    Pattern getExpectedCreatedEditRegex() {
        ~/\[ReferenceDataValue:.+?] added to component \[ReferenceDataElement:.+?]/
    }

    @Override
    Pattern getExpectedUpdateEditRegex() {
        ~/\[ReferenceDataValue:.+?] changed properties \[value]/
    }

    @Transactional
    String getSimpleReferenceDataModelId() {
        ReferenceDataModel.findByLabel(BootstrapModels.SIMPLE_REFERENCE_MODEL_NAME).id.toString()
    }

    @Transactional
    String getReferenceDataElementId() {
        ReferenceDataElement.findByLabel('Organisation code').id.toString()
    }

    @Override
    Map getValidJson() {
        [
            rowNumber           : 1,
            value               : 'Functional Test ReferenceDataValue',
            referenceDataModel  : simpleReferenceDataModelId,
            referenceDataElement: referenceDataElementId
        ]
    }

    @Override
    Map getInvalidJson() {
        [
            rowNumber           : -1,
            value               : null,
            referenceDataModel  : null,
            referenceDataElement: null,
        ]
    }

    @Override
    Map getValidUpdateJson() {
        [
            value: 'Updated ReferenceDataValue'
        ]
    }

    @Override
    Boolean readerPermissionIsInherited() {
        true
    }

    @Override
    void verifyL03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, simpleReferenceDataModelId
    }

    @Override
    void verifyL03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, simpleReferenceDataModelId
    }

    @Override
    void verifyL03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, simpleReferenceDataModelId
    }

    @Override
    void verifyN03NoContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, simpleReferenceDataModelId
    }

    @Override
    void verifyN03InvalidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, simpleReferenceDataModelId
    }

    @Override
    void verifyN03ValidContentResponse(HttpResponse<Map> response) {
        verifyNotFound response, simpleReferenceDataModelId
    }

    @Override
    void verifyE03ValidResponseBody(HttpResponse<Map> response) {
        assert response.body().id
    }

    @Override
    void verifyR04UnknownIdResponse(HttpResponse<Map> response, String id) {
        verifyForbidden response
    }

    @Override
    String getShowJson() {
        null
    }

    @Override
    String getEditorIndexJson() {
        new String(loadJsonFile('expectedEditorIndex'))
    }
}
