package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

class AnnotationSpec extends CreatorAwareSpec<Annotation> implements DomainUnitTest<Annotation> {

    BasicModel db
    Folder misc

    def setup() {
        mockDomains(Folder, BasicModel)
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        db = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc)
        checkAndSave(misc)
        checkAndSave(db)
    }

    void 'test depth and path'() {
        when:
        setValidDomainValues()
        checkAndSave(domain)
        item = findById()

        then:
        item.depth == 0
        item.path == ''

    }

    void 'test parent child annotations'() {
        given:
        setValidDomainValues()
        Annotation child = new Annotation(description: 'child', createdBy: admin.emailAddress)

        when:
        domain.addToChildAnnotations(child)

        then:
        checkAndSave(domain)

        when:
        item = findById()
        Annotation item2 = Annotation.findByLabel('test [0]')

        then:
        item
        item2

        and:
        item.depth == 0
        item.path == ''

        and:
        item2.depth == 1
        item2.path == "/${item.id}"
        item2.catalogueItemId
        item2.description == 'child'

        when:
        Annotation child2 = new Annotation(description: 'child2', createdBy: admin.emailAddress)
        item2.addToChildAnnotations(child2)

        then:
        child2.depth == 2
        child2.path == "/${item.id}/${item2.id}"

    }

    void 'test no catalogue item'() {
        given:
        domain.label = 'test'
        domain.createdBy = admin

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
        domain.label = 'test'
        domain.setCatalogueItem(db)
    }

    @Override
    void verifyDomainOtherConstraints(Annotation domain) {
        assert domain.label == 'test'
        assert domain.catalogueItemId == db.id
    }
}
