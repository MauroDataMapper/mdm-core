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
        response.json
        ApiPropertyEnum.values().findAll {it != ApiPropertyEnum.SITE_URL}.every {response.json."${it}"}
    }

    void 'test editProperties'() {
        when:
        params.currentUserSecurityPolicyManager = PublicAccessSecurityPolicyManager.instance
        request.method = 'POST'
        request.contentType = JSON_CONTENT_TYPE
        request.setJson("{\"${ApiPropertyEnum.EMAIL_FROM_ADDRESS}\":\"${admin.emailAddress}\"}".toString())
        controller.editApiProperties()

        then:
        response.status == HttpStatus.OK.code

        and: 'response has updated properties'
        response.json
        response.json."${ApiPropertyEnum.EMAIL_FROM_ADDRESS}" == admin.emailAddress

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
        response.status == HttpStatus.I_AM_A_TEAPOT.code
        response.json.user == 'unlogged_user@mdm-core.com'
        response.json.indexed
        response.json.timeTakenMilliseconds
        response.json.timeTaken
    }

    /*
        void 'test rebuild lucene indexes from restful api'() {
            given:
            controller.adminService = Mock(AdminService) {
                rebuildLuceneIndexes(_) >> {sleep(1000)}
            }
            LuceneIndexParameters indexParameters = new LuceneIndexParameters()

            when: 'no log in details provided'
            controller.rebuildLuceneIndexes(indexParameters)

            then:
            response.status == HttpStatus.UNAUTHORIZED.code

            when: 'logged in as non-admin'
            response.reset()
            indexParameters.username = editor.emailAddress
            indexParameters.password = 'password'
            controller.rebuildLuceneIndexes(indexParameters)

            then:
            response.status == HttpStatus.UNAUTHORIZED.code

            when: 'logged in as admin'
            response.reset()
            indexParameters.username = admin.emailAddress
            indexParameters.password = 'password'
            controller.rebuildLuceneIndexes(indexParameters)

            then:
            response.status == HttpStatus.I_AM_A_TEAPOT.code
            response.json.user == 'admin@maurodatamapper.com'
            response.json.indexed
            response.json.timeTakenMilliseconds
            response.json.timeTaken
        }
    */
}