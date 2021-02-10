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
package uk.ac.ox.softeng.maurodatamapper.core.admin


import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import asset.pipeline.grails.AssetResourceLocator
import com.google.common.base.Strings
import grails.testing.services.ServiceUnitTest
import org.springframework.core.io.InputStreamResource

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ApiPropertyServiceSpec extends BaseUnitSpec implements ServiceUnitTest<ApiPropertyService> {

    String tmpDir
    Path savedDefaultsPath

    def setup() {
        tmpDir = System.getProperty("java.io.tmpdir")
        tmpDir = Strings.isNullOrEmpty(tmpDir) ? "/tmp" : tmpDir

        savedDefaultsPath = Paths.get(tmpDir).resolve('savedDefaults.properties')
        Properties legacy = new Properties()
        legacy.put(ApiPropertyEnum.EMAIL_FROM_NAME.key, 'Unit Test')
        try {
            OutputStream outputStream = new FileOutputStream(savedDefaultsPath.toFile())
            legacy.store(outputStream, 'Unit Test')
        } catch (IOException e) {
            log.error("Something went wrong saving ApiProperties file", e)
        }
        assert Files.exists(savedDefaultsPath)
        mockDomain(ApiProperty)

        service.assetResourceLocator = Mock(AssetResourceLocator) {
            findAssetForURI('defaults.properties') >> {
                Path path = Paths.get('grails-app/assets/api/defaults.properties')
                assert Files.exists(path)
                new InputStreamResource(Files.newInputStream(path))
            }
        }


    }

    def cleanup() {
        if (Files.exists(savedDefaultsPath)) {
            Files.delete(savedDefaultsPath)
        }
        assert !Files.exists(savedDefaultsPath)
    }

    void 'test list with nothing loaded'() {
        expect:
        !service.list()

        and:
        !service.count()
    }

    void 'test loading defaults'() {
        when:
        service.loadDefaultPropertiesIntoDatabase(admin)
        List<ApiProperty> loaded = service.list()

        then:
        loaded.size() == 15

        and:
        for (ApiPropertyEnum property : ApiPropertyEnum.values()) {
            assert loaded.find {property}
        }
    }

    void 'test loading legacy defaults'() {
        when:
        service.loadDefaultPropertiesIntoDatabase(admin)
        service.loadLegacyPropertiesFromDefaultsFileIntoDatabase(tmpDir, editor)

        List<ApiProperty> loaded = service.list()

        then:
        loaded.size() == 15

        when:
        ApiProperty updated = service.findByApiPropertyEnum(ApiPropertyEnum.EMAIL_FROM_NAME)

        then:
        updated.value == 'Unit Test'
        updated.lastUpdatedBy == editor.emailAddress
    }
}
