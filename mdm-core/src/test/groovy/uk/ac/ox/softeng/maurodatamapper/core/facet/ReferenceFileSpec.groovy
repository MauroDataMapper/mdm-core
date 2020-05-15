package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest
import grails.web.mime.MimeType

class ReferenceFileSpec extends CreatorAwareSpec<ReferenceFile> implements DomainUnitTest<ReferenceFile> {

    BasicModel db
    Folder misc

    def setup() {
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        db = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc)
        mockDomains(Folder, BasicModel)
        checkAndSave(misc)
        checkAndSave(db)
    }

    @Override
    void setValidDomainOtherValues() {
        domain.fileName = 'test'
        domain.fileType = MimeType.XML.toString()
        domain.fileContents = 'some content'.bytes
        domain.catalogueItem = db
    }

    @Override
    void verifyDomainOtherConstraints(ReferenceFile domain) {
        domain.fileName == 'test'
        domain.fileType == MimeType.XML.toString()
        new String(domain.fileContents) == 'some content'
        domain.fileSize == 12
        domain.catalogueItemId == db.id
        domain.catalogueItemDomainType = BasicModel.simpleName
    }
}