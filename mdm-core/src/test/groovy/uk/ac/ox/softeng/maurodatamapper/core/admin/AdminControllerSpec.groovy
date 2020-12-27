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
package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.core.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.BootStrap
import uk.ac.ox.softeng.maurodatamapper.core.hibernate.search.LuceneIndexingService
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.LuceneIndexParameters
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import asset.pipeline.grails.AssetResourceLocator
import com.google.common.base.Strings
import grails.testing.web.controllers.ControllerUnitTest
import grails.util.BuildSettings
import io.micronaut.http.HttpStatus
import org.springframework.core.io.InputStreamResource

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AdminControllerSpec extends BaseUnitSpec implements ControllerUnitTest<AdminController> {

    def currentVersion

    def setup() {
        String tmpDir = System.getProperty("java.io.tmpdir")
        tmpDir = Strings.isNullOrEmpty(tmpDir) ? "/tmp" : tmpDir

        mockDomain(ApiProperty)

        ApiPropertyService apiPropertyService = new ApiPropertyService()

        apiPropertyService.assetResourceLocator = Mock(AssetResourceLocator) {
            findAssetForURI('defaults.properties') >> {
                Path path = Paths.get('grails-app/assets/api/defaults.properties')
                assert Files.exists(path)
                new InputStreamResource(Files.newInputStream(path))
            }
        }

        AdminService adminService = Spy(AdminService)
        adminService.apiPropertyService = apiPropertyService
        controller.adminService = adminService

        controller.luceneIndexingService = Stub(LuceneIndexingService) {
            rebuildLuceneIndexes(_) >> {sleep(1000)}
        }

        BootStrap bootStrap = new BootStrap()
        bootStrap.grailsApplication = grailsApplication
        bootStrap.apiPropertyService = apiPropertyService
        bootStrap.grailsApplication.config.simplejavamail.smtp.username = 'Unit Test Controller'
        bootStrap.loadApiProperties(tmpDir)

        Path gradleProperties = Paths.get(BuildSettings.BASE_DIR.absolutePath, 'gradle.properties')
        assert Files.exists(gradleProperties)
        currentVersion = Files.readAllLines(gradleProperties).find {it.startsWith('version')}.find(/version=(.+)/) {it[1]}
    }

    void 'test apiProperties'() {
        when:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        controller.apiProperties()

        then:
        model.apiPropertyList
        ApiPropertyEnum.values().findAll { it != ApiPropertyEnum.SITE_URL }.every { ape -> model.apiPropertyList.any { it.key == ape.key } }
    }

    void 'test editProperties'() {
        when:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        request.method = 'POST'
        request.contentType = JSON_CONTENT_TYPE
        request.setJson("{\"${ApiPropertyEnum.EMAIL_FROM_ADDRESS}\":\"${admin.emailAddress}\"}".toString())
        controller.editApiProperties()

        then:
        status == HttpStatus.OK.code

        and: 'response has updated properties'
        model.apiPropertyList
        model.apiPropertyList.find { it.key == ApiPropertyEnum.EMAIL_FROM_ADDRESS.key }.value == admin.emailAddress

        and: 'properties loaded from servlet context has the updated property'
        ApiProperty.findByKey(ApiPropertyEnum.EMAIL_FROM_ADDRESS.key).value == admin.emailAddress

    }

    void 'test rebuild lucene indexes from the UI'() {
        given:
        LuceneIndexParameters indexParameters = new LuceneIndexParameters()

        when:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        controller.rebuildLuceneIndexes(indexParameters)

        then:
        status == HttpStatus.OK.code
        model.user == 'unlogged_user@mdm-core.com'
        model.indexed
        model.timeTakenMilliseconds
        model.timeTaken
    }
}