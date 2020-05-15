package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

class VersionLinkSpec extends CreatorAwareSpec<VersionLink> implements DomainUnitTest<VersionLink> {

    BasicModel db, db2
    Folder misc

    def setup() {
        mockDomains(Folder, BasicModel)
        misc = new Folder(createdBy: admin.emailAddress, label: 'misc')
        db = new BasicModel(createdBy: admin.emailAddress, label: 'test', folder: misc)
        db2 = new BasicModel(createdBy: admin.emailAddress, label: 'test2', folder: misc)
        checkAndSave(misc)
        checkAndSave(db)
        checkAndSave(db2)
    }

    void 'test saving valid object'() {

        when:
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)
        domain.hasErrors()
        domain.errors.allErrors.size() == 6

        when:
        domain.createdBy = admin.emailAddress
        domain.linkType = VersionLinkType.SUPERSEDED_BY_MODEL
        db.addToVersionLinks(domain)
        domain.setTargetModel(db2)

        then:
        checkAndSave(domain)

        when:
        def d1 = BasicModel.findByLabel('test')
        def d2 = BasicModel.findByLabel('test2')

        then:
        d1
        d2

        and:
        d1.versionLinks.find {it.id == domain.id}

        when:
        VersionLink tsl = VersionLink.byTargetModelId(d2.id).get()

        then:
        tsl
        tsl.id == domain.id
    }

    void 'test saving valid object through an item'() {

        when:
        domain.createdBy = admin.emailAddress
        domain.linkType = VersionLinkType.SUPERSEDED_BY_MODEL
        db.addToVersionLinks(domain)
        domain.setTargetModel(db2)

        then:
        checkAndSave(db)

        when:
        def item = findById()
        def d1 = BasicModel.findByLabel('test')
        def d2 = BasicModel.findByLabel('test2')

        then:
        item
        d1
        d2

        and:
        d1.versionLinks.find {it.id == item.id}

        when:
        VersionLink tsl = VersionLink.byTargetModelId(d2.id).get()

        then:
        tsl
        tsl.id == item.id

    }

    void 'test invalid term link between the same model'() {
        when:
        domain.createdBy = admin.emailAddress
        domain.linkType = VersionLinkType.SUPERSEDED_BY_MODEL
        db.addToVersionLinks(domain)
        domain.setTargetModel(db)
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)

        and:
        domain.hasErrors()

        when:
        def error = domain.errors.getFieldError('catalogueItemId')

        then:
        error.code == 'invalid.same.property.message'
        error.arguments.last() == 'targetModel'
    }

    @Override
    void setValidDomainOtherValues() {
        domain.linkType = VersionLinkType.SUPERSEDED_BY_MODEL
        db.addToVersionLinks(domain)
        domain.setTargetModel(db2)
    }

    @Override
    void verifyDomainOtherConstraints(VersionLink domain) {
        assert domain.linkType == VersionLinkType.SUPERSEDED_BY_MODEL
    }
}
