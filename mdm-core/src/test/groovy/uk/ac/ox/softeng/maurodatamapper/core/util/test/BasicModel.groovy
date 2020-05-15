package uk.ac.ox.softeng.maurodatamapper.core.util.test

import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.diff.ObjectDiff
import uk.ac.ox.softeng.maurodatamapper.core.facet.Annotation
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.ReferenceFile
import uk.ac.ox.softeng.maurodatamapper.core.facet.SemanticLink
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLink
import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.util.Version

import grails.gorm.DetachedCriteria
import grails.gorm.annotation.Entity
import org.grails.datastore.gorm.GormEntity

/**
 * @since 10/12/2019
 */
@Entity
class BasicModel implements Model<BasicModel>, GormEntity<BasicModel> {

    UUID id

    static hasMany = [
        classifiers   : Classifier,
        modelItems    : BasicModelItem,
        metadata      : Metadata,
        annotations   : Annotation,
        semanticLinks : SemanticLink,
        versionLinks  : VersionLink,
        referenceFiles: ReferenceFile
    ]

    static constraints = {
        CallableConstraints.call(ModelConstraints, delegate)
    }

    BasicModel() {
        modelType = BasicModel.simpleName
        deleted = false
        finalised = false
        documentationVersion = Version.from('1.0.0')
        id = UUID.randomUUID()
        breadcrumbTree = new BreadcrumbTree(this)
    }

    @Override
    String getDomainType() {
        BasicModel.simpleName
    }

    Set<BasicModelItem> getAllModelItems() {
        (modelItems ?: []) + modelItems.collect {it.getAllModelItems()}.flatten()
    }

    @Override
    Boolean hasChildren() {
        modelItems == null ? false : !modelItems.isEmpty()
    }

    @Override
    String getEditLabel() {
        "${domainType}:${label}"
    }

    def beforeValidate() {
        beforeValidateCatalogueItem()
    }

    BasicModel addToModelItems(Map map) {
        addToModelItems new BasicModelItem(map)
    }

    BasicModel addToModelItems(BasicModelItem basicModelItem) {
        basicModelItem.model = this
        addTo('modelItems', basicModelItem)
    }

    ObjectDiff<BasicModel> diff(BasicModel obj) {
        modelDiffBuilder(BasicModel, this, obj)
    }

    static BasicModel findByIdJoinClassifiers(UUID id) {
        new DetachedCriteria<BasicModel>(BasicModel).idEq(id).join('classifiers').get()
    }


}
