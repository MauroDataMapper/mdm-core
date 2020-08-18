package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.callable.ModelConstraints
import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.Validator
import uk.ac.ox.softeng.maurodatamapper.util.Version

class ModelVersionValidator implements Validator<Version> {

    final Model model

    ModelVersionValidator(Model model) {
        this.model = model
    }

    @Override
    Object isValid(Version modelVersion) {
        if (model.ident() && model.isDirty('modelVersion') && model.getOriginalValue('modelVersion')) {
            return ['model.model.version.change.not.allowed']
        }
        if (modelVersion && model.branchName != ModelConstraints.DEFAULT_BRANCH_NAME) {
            return ['model.model.version.cannot.be.set.on.branch']
        }
        if (modelVersion && !model.finalised) {
            return ['model.model.version.can.only.set.on.finalised.model']
        }
        if (!modelVersion && model.finalised) {
            return ['model.model.version.must.be.set.on.finalised.model']
        }
        true
    }
}
