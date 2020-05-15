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