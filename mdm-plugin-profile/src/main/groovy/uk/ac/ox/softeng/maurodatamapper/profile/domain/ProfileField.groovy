package uk.ac.ox.softeng.maurodatamapper.profile.domain

import groovy.transform.AutoClone

@AutoClone
class ProfileField {

    String fieldName
    String description
    Integer maxMultiplicity
    Integer minMultiplicity
    String dataType
    String currentValue
}
