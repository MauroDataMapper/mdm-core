package uk.ac.ox.softeng.maurodatamapper.core.gorm.constraint.validator

import uk.ac.ox.softeng.maurodatamapper.core.container.Folder
import uk.ac.ox.softeng.maurodatamapper.gorm.constraint.validator.Validator

/**
 *
 * @since 30/01/2018
 */
class FolderLabelValidator implements Validator<String> {

    private static String NOT_UNIQUE_MSG = 'default.not.unique.message'

    final Folder folder

    FolderLabelValidator(Folder folder) {
        this.folder = folder
    }

    @Override
    Object isValid(String value) {
        if (value == null) return ['default.null.message']
        if (!value) return ['default.blank.message']

        if (folder.parentFolder) {
            if (folder.id) {
                if (Folder.countByLabelAndParentFolderAndIdNotEqual(value,
                                                                    folder.parentFolder,
                                                                    folder.id)) return [NOT_UNIQUE_MSG]
            } else if (Folder.countByLabelAndParentFolder(value, folder.parentFolder)) return [NOT_UNIQUE_MSG]
        } else {
            if (folder.id) {
                if (Folder.countByLabelAndParentFolderIsNullAndIdNotEqual(value, folder.id)) return [NOT_UNIQUE_MSG]
            } else if (Folder.countByLabelAndParentFolderIsNull(value)) return [NOT_UNIQUE_MSG]
        }

        true
    }
}
