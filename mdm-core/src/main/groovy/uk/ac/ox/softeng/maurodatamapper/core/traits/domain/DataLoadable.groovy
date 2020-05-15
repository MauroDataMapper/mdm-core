package uk.ac.ox.softeng.maurodatamapper.core.traits.domain


import uk.ac.ox.softeng.maurodatamapper.core.container.Classifier
import uk.ac.ox.softeng.maurodatamapper.core.model.container.ClassifierAware
import uk.ac.ox.softeng.maurodatamapper.util.Version

import org.grails.datastore.gorm.GormEntityApi
import org.grails.datastore.gorm.GormValidateable
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable

trait DataLoadable implements GormEntityApi, GormValidateable, DirtyCheckable, InformationAware {

    abstract Version getDocumentationVersion()

    abstract ClassifierAware addToClassifiers(Classifier classifier)

    abstract void setDocumentationVersion(Version version)

}