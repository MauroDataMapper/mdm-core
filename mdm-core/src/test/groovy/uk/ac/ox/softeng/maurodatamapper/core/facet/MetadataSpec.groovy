package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

class MetadataSpec extends CreatorAwareSpec<Metadata> implements DomainUnitTest<Metadata> {

    BasicModel db
    Folder misc

    def setup() {
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        db = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc)
        mockDomains(Folder, BasicModel)
        checkAndSave(misc)
        checkAndSave(db)
    }

    void 'test no catalogue item'() {
        given:
        domain.namespace = "http://test.com/valid"
        domain.key = 'test_key'
        domain.value = 'a value'
        domain.createdBy = admin.emailAddress

        when:
        check(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.getFieldError('catalogueItemId')
        domain.errors.getFieldError('catalogueItemDomainType')
    }

    @Override
    void setValidDomainOtherValues() {
        domain.namespace = "http://test.com/valid"
        domain.key = 'test_key'
        domain.value = 'a value'
        domain.catalogueItem = db
    }

    @Override
    void verifyDomainOtherConstraints(Metadata domain) {
        assert domain.namespace == "http://test.com/valid"
        assert domain.key == 'test_key'
        assert domain.value == 'a value'
        assert domain.catalogueItem.id
        assert domain.catalogueItemDomainType == BasicModel.simpleName
        assert domain.catalogueItem.id == db.id
        assert domain.createdBy == admin.emailAddress
    }
}
