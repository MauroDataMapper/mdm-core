package uk.ac.ox.softeng.maurodatamapper.traits.domain

import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.SelfType
import org.grails.datastore.gorm.GormEntity

import java.time.OffsetDateTime

/**
 * This is the base domain trait which all domain classes must extend/implement in the MDC.
 * @since 25/09/2017
 */
@SelfType(GormEntity)
trait CreatorAware {

    OffsetDateTime dateCreated
    OffsetDateTime lastUpdated
    String createdBy

    Boolean isOwnedBy(User user) {
        createdBy == user?.emailAddress
    }

    abstract UUID getId()

    abstract String getDomainType()

    void setCreatedByUser(User user) {
        this.createdBy = user?.emailAddress
    }
}