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
package uk.ac.ox.softeng.maurodatamapper.core.file

import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.test.functional.ResourceFunctionalSpec

import grails.testing.mixin.integration.Integration
import grails.testing.spock.OnceBefore
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import spock.lang.Ignore

import java.nio.file.Files
import java.nio.file.Paths

/**
 * @see uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFileController* TODO
 */
@Integration
@Slf4j
@Ignore('Needs to be tested in the security module')
class UserImageFileFunctionalSpec extends ResourceFunctionalSpec<UserImageFile> {

    String getResourcePath() {
        'userImageFiles'
    }

    @Override
    Map getValidJson() {
        [image: Files.readString(Paths.get('src/integration-test/resources/image_data_file.txt')),
         type : 'image/png;charset=utf-8']
    }

    @Override
    Map getInvalidJson() {
        [type: '']
    }

    @Override
    String getExpectedShowJson() {
        null
    }

    @OnceBefore
    void checkImageDataFileExists() {
        assert Files.exists(Paths.get('src/integration-test/resources/image_data_file.txt'))
    }

    @Override
    void verifyR5ShowResponse() {
        verifyResponse(HttpStatus.OK, jsonCapableResponse)
        ((String) jsonCapableResponse.body()).size() == 76682
    }
}