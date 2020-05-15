package uk.ac.ox.softeng.maurodatamapper.core.facet

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLinkType
import uk.ac.ox.softeng.maurodatamapper.core.util.test.BasicModel
import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest
import org.spockframework.util.InternalSpockError

class SemanticLinkSpec extends CreatorAwareSpec<SemanticLink> implements DomainUnitTest<SemanticLink> {

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
        domain.linkType = SemanticLinkType.DOES_NOT_REFINE
        db.addToSemanticLinks(domain)
        domain.setTargetCatalogueItem(db2)

        then:
        checkAndSave(domain)

        when:
        def d1 = BasicModel.findByLabel('test')
        def d2 = BasicModel.findByLabel('test2')

        then:
        d1
        d2

        and:
        d1.semanticLinks.find {it.id == domain.id}

        when:
        SemanticLink tsl = SemanticLink.byTargetCatalogueItemId(d2.id).get()

        then:
        tsl
        tsl.id == domain.id
    }

    void 'test saving valid object through an item'() {

        when:
        domain.createdBy = admin.emailAddress
        domain.linkType = SemanticLinkType.DOES_NOT_REFINE
        db.addToSemanticLinks(domain)
        domain.setTargetCatalogueItem(db2)

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
        d1.semanticLinks.find {it.id == item.id}

        when:
        SemanticLink tsl = SemanticLink.byTargetCatalogueItemId(d2.id).get()

        then:
        tsl
        tsl.id == item.id

    }

    void 'test invalid term link between the same item'() {
        when:
        domain.createdBy = admin.emailAddress
        domain.linkType = SemanticLinkType.DOES_NOT_REFINE
        db.addToSemanticLinks(domain)
        domain.setTargetCatalogueItem(db)
        checkAndSave(domain)

        then:
        thrown(InternalSpockError)

        and:
        domain.hasErrors()

        when:
        def error = domain.errors.getFieldError('catalogueItemId')

        then:
        error.code == 'invalid.same.property.message'
        error.arguments.last() == 'targetCatalogueItem'
    }

    @Override
    void setValidDomainOtherValues() {
        domain.linkType = SemanticLinkType.REFINES
        db.addToSemanticLinks(domain)
        domain.setTargetCatalogueItem(db2)
    }

    @Override
    void verifyDomainOtherConstraints(SemanticLink domain) {
        assert domain.linkType == SemanticLinkType.REFINES
    }
}
