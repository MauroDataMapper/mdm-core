package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.Validator

/**
 *
 * @since 30/01/2018
 */
class ModelLabelValidator implements Validator<String> {

    final Model model

    ModelLabelValidator(Model model) {
        this.model = model
    }

    Class getModelClass() {
        model.getClass()
    }

    @Override
    Object isValid(String value) {
        if (value == null) return ['default.null.message']
        if (!value) return ['default.blank.message']

        List modelsWithTheSameLabel = []
        // Existing models can change label but it must not already be in use
        if (model.ident()) modelsWithTheSameLabel = modelClass.findAllByLabelAndIdNotEqual(value, model.ident())
        else modelsWithTheSameLabel = modelClass.findAllByLabel(value)

        allModelsWithTheSameLabelsHaveDifferentDocumentationVersions(modelsWithTheSameLabel) ?: ['default.not.unique.message']
    }

    boolean allModelsWithTheSameLabelsHaveDifferentDocumentationVersions(List modelsWithTheSameLabel) {
        if (!modelsWithTheSameLabel) return true
        modelsWithTheSameLabel.add(model)
        modelsWithTheSameLabel.groupBy {it.documentationVersion}.every {it.value.size() == 1}
    }
}
