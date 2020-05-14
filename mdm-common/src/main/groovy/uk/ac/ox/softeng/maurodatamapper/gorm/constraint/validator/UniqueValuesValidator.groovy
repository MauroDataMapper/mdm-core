package uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

/**
 * @since 13/02/2018
 */
@CompileStatic
class UniqueValuesValidator implements Validator<Collection> {

    String uniqueProperty
    String uniquePropertyPath

    UniqueValuesValidator(String uniqueProperty) {
        this(uniqueProperty, uniqueProperty)
    }

    UniqueValuesValidator(String uniqueProperty, String uniquePropertyPath) {
        this.uniqueProperty = uniqueProperty
        this.uniquePropertyPath = uniquePropertyPath
    }

    @Override
    Object isValid(Collection value) {
        if (!value) return true
        Map<String, List> allGrouped = groupCollection(value)
        isValid(allGrouped)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    Map<String, List> groupCollection(Collection value) {
        value.groupBy {it."${uniqueProperty}"} as Map<String, List>
    }

    Object isValid(Map<String, List> allGrouped) {
        List<String> nonUnique = allGrouped.findAll {it.value.size() != 1}.collect {it.key}
        if (nonUnique) return ['invalid.unique.values.message', nonUnique.sort().join(', '), uniquePropertyPath]
        true
    }
}
