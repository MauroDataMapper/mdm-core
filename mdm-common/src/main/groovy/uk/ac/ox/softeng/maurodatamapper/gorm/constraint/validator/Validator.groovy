package uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator

/**
 * @since 14/02/2020
 */
interface Validator<T> {

    Object isValid(T value);
}
