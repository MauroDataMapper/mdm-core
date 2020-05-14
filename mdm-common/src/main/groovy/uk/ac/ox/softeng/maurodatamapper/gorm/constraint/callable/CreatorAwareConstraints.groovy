package uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable

/**
 * @since 17/02/2020
 */
class CreatorAwareConstraints {

    static constraints = {
        createdBy nullable: false, email: true
    }
}
