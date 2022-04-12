package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.Validator
import uk.ac.ox.softeng.maurodatamapper.path.Path
import uk.ac.ox.softeng.maurodatamapper.path.PathNode

/**
 * @since 12/04/2022
 */
class LabelValidator implements Validator<String> {

    @Override
    Object isValid(String value) {
        if (value == null) return ['default.null.message']
        if (!value) return ['default.blank.message']
        if ((value as String).find(/${Path.ESCAPED_PATH_DELIMITER}|${PathNode.ATTRIBUTE_PATH_IDENTIFIER_SEPARATOR}|${PathNode.ESCAPED_MODEL_PATH_IDENTIFIER_SEPARATOR}/)) {
            return ['invalid.label.characters', [Path.PATH_DELIMITER, PathNode.ATTRIBUTE_PATH_IDENTIFIER_SEPARATOR, PathNode.MODEL_PATH_IDENTIFIER_SEPARATOR]]
        }
        true
    }
}
