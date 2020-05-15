package uk.ac.ox.softeng.maurodatamapper.core.admin

import uk.ac.ox.softeng.maurodatamapper.core.ApiPropertyService
import uk.ac.ox.softeng.maurodatamapper.core.BootStrap
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import asset.pipeline.grails.AssetResourceLocator
import com.google.common.base.Strings
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import org.springframework.core.io.InputStreamResource

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AdminServiceSpec extends BaseUnitSpec implements ServiceUnitTest<AdminService>, GrailsWebUnitTest {

    // Given api properties are loaded on startup, which they are as part of bootstrap
    def setup() {
        String tmpDir = System.getProperty("java.io.tmpdir")
        tmpDir = Strings.isNullOrEmpty(tmpDir) ? "/tmp" : tmpDir

        mockDomain(ApiProperty)
        mockArtefact(ApiPropertyService)

        ApiPropertyService apiPropertyService = applicationContext.getBean(ApiPropertyService)
        apiPropertyService.assetResourceLocator = Mock(AssetResourceLocator) {
            findAssetForURI('defaults.properties') >> {
                Path path = Paths.get('grails-app/assets/api/defaults.properties')
                assert Files.exists(path)
                new InputStreamResource(Files.newInputStream(path))
            }
        }

        BootStrap bootStrap = new BootStrap()
        bootStrap.grailsApplication = grailsApplication
        bootStrap.apiPropertyService = apiPropertyService
        bootStrap.loadApiProperties(tmpDir)
    }

    void 'test getting the api properties'() {
        when:
        List<ApiProperty> apiProperties = service.getApiProperties()

        then:
        apiProperties

        and:
        for (ApiPropertyEnum property : ApiPropertyEnum.values()) {
            if (property != ApiPropertyEnum.SITE_URL)
                assert apiProperties.find {property}
        }
    }

    void 'test updating api properties'() {
        when:
        Map<String, String> updates = [:]
        updates."${ApiPropertyEnum.EMAIL_FROM_ADDRESS.toString()}" = admin.emailAddress

        List<ApiProperty> apiProperties = service.getAndUpdateApiProperties(admin, updates)

        then:
        apiProperties.find {it.key == ApiPropertyEnum.EMAIL_FROM_ADDRESS.key}

        when:
        String updated = apiProperties.find {it.key == ApiPropertyEnum.EMAIL_FROM_ADDRESS.key}?.value

        then:
        updated == admin.emailAddress
    }
}
