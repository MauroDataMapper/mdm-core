package uk.ac.ox.softeng.maurodatamapper.core.container

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.InformationAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Container
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CallableConstraints
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints
import uk.ac.ox.softeng.maurodatamapper.search.PathTokenizerAnalyzer
import uk.ac.ox.softeng.maurodatamapper.search.bridge.OffsetDateTimeBridge
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.DetachedCriteria
import grails.plugins.hibernate.search.HibernateSearchApi
import grails.rest.Resource

@Resource(readOnly = false, formats = ['json', 'xml'])
class Classifier implements Container {

    public final static Integer BATCH_SIZE = 1000

    UUID id

    Classifier parentClassifier

    static hasMany = [
        childClassifiers: Classifier,
    ]

    static belongsTo = [
        Classifier
    ]

    static constraints = {
        CallableConstraints.call(CreatorAwareConstraints, delegate)
        CallableConstraints.call(InformationAwareConstraints, delegate)
        label unique: true
        parentClassifier nullable: true
    }

    static mapping = {
        childClassifiers cascade: 'all-delete-orphan'
        parentClassifier index: 'classifier_parent_classifier_idx', cascade: 'none'
    }

    static mappedBy = [
        childClassifiers: 'parentClassifier',
    ]

    static search = {
        label index: 'yes', analyzer: 'wordDelimiter'
        path index: 'yes', analyzer: PathTokenizerAnalyzer
        description termVector: 'with_positions'
        lastUpdated index: 'yes', bridge: ['class': OffsetDateTimeBridge]
        dateCreated index: 'yes', bridge: ['class': OffsetDateTimeBridge]
    }

    Classifier() {
    }

    @Override
    String getDomainType() {
        Classifier.simpleName
    }


    @Override
    Classifier getPathParent() {
        parentClassifier
    }

    @Override
    def beforeValidate() {
        buildPath()
        childClassifiers.each {it.beforeValidate()}
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
    void addCreatedEdit(User creator) {
        String description = parentClassifier ? "${editLabel} as child of [${parentClassifier.editLabel}]" : "${editLabel} added"
        addToEditsTransactionally creator, description
    }

    @Override
    String getEditLabel() {
        "Classifier:${label}"
    }

    @Override
    boolean hasChildren() {
        !childClassifiers.isEmpty()
    }

    @Override
    Boolean getDeleted() {
        false
    }

    static DetachedCriteria<Classifier> by() {
        new DetachedCriteria<Classifier>(Classifier)
    }

    static DetachedCriteria<Classifier> byLabel(String label) {
        by().eq('label', label)
    }

    static List<Classifier> luceneList(@DelegatesTo(HibernateSearchApi) Closure closure) {
        Classifier.search().list closure
    }

    static List<Classifier> luceneTreeLabelSearch(List<String> allowedIds, String searchTerm) {
        luceneList {
            keyword 'label', searchTerm
            filter name: 'idSecured', params: [allowedIds: allowedIds]
        }
    }

    static List<Classifier> findAllContainedInClassifierId(UUID classifierId) {
        luceneList {
            should {
                keyword 'path', classifierId.toString()
            }
        }
    }
}