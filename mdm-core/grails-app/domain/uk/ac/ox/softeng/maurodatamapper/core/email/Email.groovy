package uk.ac.ox.softeng.maurodatamapper.core.email

import grails.gorm.DetachedCriteria

import java.time.OffsetDateTime

class Email {

    UUID id
    String sentToEmailAddress
    String subject
    String body
    String emailServiceUsed
    OffsetDateTime dateTimeSent
    Boolean successfullySent
    String failureReason

    static constraints = {
        failureReason nullable: true
    }

    static mapping = {
        body type: 'text'
        subject type: 'text'
        failureReason type: 'text'
    }

    static DetachedCriteria<Email> by() {
        new DetachedCriteria<Email>(Email)
    }

    static DetachedCriteria<Email> withFilter(Map filters) {
        DetachedCriteria<Email> criteria = by()
        if (filters.sentToEmailAddress) criteria = criteria.ilike('sentToEmailAddress', "%${filters.sentToEmailAddress}%")
        if (filters.subject) criteria = criteria.ilike('subject', "%${filters.subject}%")
        if (filters.emailServiceUsed) criteria = criteria.ilike('emailServiceUsed', "%${filters.emailServiceUsed}%")
        if (filters.successfullySent) criteria = criteria.eq('successfullySent', filters.successfullySent)
        criteria
    }
}
