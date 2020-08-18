package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.Validator
import uk.ac.ox.softeng.maurodatamapper.util.Version

class DocumentationVersionValidator implements Validator<Version> {

    final Model model

    DocumentationVersionValidator(Model model) {
        this.model = model
    }

    @Override
    Object isValid(Version documentationVersion) {
        if (model.ident() && model.isDirty('documentationVersion')) {
            return ['model.documentation.version.change.not.allowed']
        }
        true
    }
}
