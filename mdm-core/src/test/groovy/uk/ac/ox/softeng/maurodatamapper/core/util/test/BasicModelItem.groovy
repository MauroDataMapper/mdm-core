package uk.ac.ox.softeng.maurodatamapper.core.util.test

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelItemConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints

import grails.gorm.DetachedCriteria
import grails.gorm.annotation.Entity
import org.grails.datastore.gorm.GormEntity

/**
 * @since 10/12/2019
 */
@Entity
class BasicModelItem implements ModelItem<BasicModelItem, BasicModel>, GormEntity<BasicModelItem> {

    UUID id
    BasicModel model
    BasicModelItem parent

    static constraints = {
        CallableConstraints.call(ModelItemConstraints, delegate)
        parent nullable: true
    }

    static belongsTo = [BasicModel, BasicModelItem]

    static hasMany = [
        classifiers    : Classifier,
        childModelItems: BasicModelItem,
        metadata       : Metadata,
        annotations    : Annotation,
        semanticLinks  : SemanticLink,
        referenceFiles : ReferenceFile
    ]

    static mappedBy = [
        childModelItems: 'parent'
    ]

    static mapping = {
        breadcrumbTree fetch: 'join'
    }

    BasicModelItem() {
        id = UUID.randomUUID()
    }

    @Override
    String getDomainType() {
        BasicModelItem.simpleName
    }


    @Override
    String getEditLabel() {
        null
    }

    @Override
    def beforeValidate() {
        idx = 0
        if (!model) this.model = parent?.model
        beforeValidateModelItem()
    }

    @Override
    def beforeInsert() {
        buildPath()
    }

    @Override
    def beforeUpdate() {
        buildPath()
    }

    @Override
    GormEntity getPathParent() {
        parent ?: model
    }

    BasicModelItem addToChildModelItems(Map map) {
        addToChildModelItems new BasicModelItem(map)
    }

    BasicModelItem addToChildModelItems(BasicModelItem basicModelItem) {
        basicModelItem.parent = this
        basicModelItem.model = this.model
        addTo('childModelItems', basicModelItem)
    }

    Boolean hasChildren() {
        childModelItems == null ? false : !childModelItems.isEmpty()
    }

    Set<BasicModelItem> getAllModelItems() {
        (childModelItems ?: []) + childModelItems.collect {it.getAllModelItems()}.flatten()
    }

    static BasicModel findByIdJoinClassifiers(UUID id) {
        new DetachedCriteria<BasicModel>(BasicModel).idEq(id).join('classifiers').get()
    }

    ObjectDiff<BasicModelItem> diff(BasicModelItem obj) {
        catalogueItemDiffBuilder(BasicModelItem, this, obj)
    }
}
