package uk.ac.ox.softeng.maurodatamapper.core.file

import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest
import grails.web.mime.MimeType

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class UserImageFileSpec extends CreatorAwareSpec<UserImageFile> implements DomainUnitTest<UserImageFile> {

    void 'test creating actual user image'() {
        given:
        Path p = Paths.get('src/test/resources/userimagefile_string_content.txt')
        assert Files.exists(p)
        String image = Files.readString(p)

        when:
        domain.setImage(image)
        domain.setType('png')
        domain.setCreatedBy(editor.emailAddress)
        domain.setUserId(editor.id)
        checkAndSave(domain)

        then:
        noExceptionThrown()
    }

    @Override
    void setValidDomainOtherValues() {
        domain.fileName = 'test'
        domain.fileType = MimeType.XML.toString()
        domain.fileContents = 'some content'.bytes
        domain.userId = UUID.randomUUID()
    }

    @Override
    void verifyDomainOtherConstraints(UserImageFile domain) {
        domain.fileName == 'test'
        domain.fileType == MimeType.XML.toString()
        new String(domain.fileContents) == 'some content'
        domain.fileSize == 12
    }
}
