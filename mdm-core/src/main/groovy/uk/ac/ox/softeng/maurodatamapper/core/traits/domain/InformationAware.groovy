package uk.ac.ox.softeng.maurodatamapper.core.traits.domain

import groovy.transform.CompileStatic
import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

/**
 * @since 20/09/2017
 */
@SelfType(GormEntity)
@CompileStatic
trait InformationAware {

    String description
    String label

    String toString() {
        "${getClass().getName()} (${label})[${ident() ?: '(unsaved)'}]"
    }
}