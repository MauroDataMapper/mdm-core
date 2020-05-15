package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable


import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.callable.CreatorAwareConstraints

/**
 * @since 17/02/2020
 */
class CatalogueFileConstraints extends CreatorAwareConstraints {

    static constraints = {
        fileContents maxSize: 200000000
        fileName blank: false
        fileType blank: false
    }
}
