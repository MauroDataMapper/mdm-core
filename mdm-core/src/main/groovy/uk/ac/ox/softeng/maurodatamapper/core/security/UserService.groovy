package uk.ac.ox.softeng.maurodatamapper.core.security

import uk.ac.ox.softeng.maurodatamapper.core.file.UserImageFile
import uk.ac.ox.softeng.maurodatamapper.security.User

interface UserService {

    UserImageFile addImageCreatedEditToUser(User creator, UserImageFile domain, UUID userId)

    UserImageFile addImageUpdatedEditToUser(User editor, UserImageFile domain, UUID userId, List<String> dirtyPropertyNames)

    UserImageFile addImageDeletedEditToUser(User deleter, UserImageFile domain, UUID userId)

}