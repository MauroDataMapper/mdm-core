package uk.ac.ox.softeng.maurodatamapper.core.file


import uk.ac.ox.softeng.maurodatamapper.core.model.file.CatalogueFileService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import asset.pipeline.grails.AssetResourceLocator
import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource

import javax.transaction.Transactional

@Slf4j
@Transactional
class UserImageFileService implements CatalogueFileService<UserImageFile> {

    AssetResourceLocator assetResourceLocator

    UserImageFile get(Serializable id) {
        UserImageFile.get(id)
    }

    List<UserImageFile> list(Map args) {
        UserImageFile.list(args)
    }

    Long count() {
        UserImageFile.count()
    }

    void delete(Serializable id) {
        delete(get(id))
    }

    void delete(UserImageFile userImageFile) {
        userImageFile.delete(flush: true)
    }

    UserImageFile findByUserId(UUID userId) {
        UserImageFile.findByUserId(userId)
    }

    /**
     * Find all resources by the defined user security policy manager. If none provided then assume no security policy in place in which case
     * everything is public.
     * @param userSecurityPolicyManager
     * @param pagination
     * @return
     */
    List<UserImageFile> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        List<UUID> ids = userSecurityPolicyManager.listReadableSecuredResourceIds(UserImageFile)
        ids ? UserImageFile.findAllByIdInList(ids, pagination) : []
    }

    @Override
    UserImageFile createNewFile(String name, byte[] contents, String type, User user) {
        UserImageFile uif = createNewFileBase(name, contents, type, user.emailAddress)
        uif.userId = user.id
        uif
    }

    @Override
    UserImageFile resizeImage(UserImageFile catalogueFile, int size) {
        UserImageFile uif = resizeImageBase(catalogueFile, size)
        uif.userId = catalogueFile.userId
        uif
    }

    UserImageFile getDefaultNoProfileImageForUser(User user) {
        log.info("Loading default profile image")
        Resource resource = assetResourceLocator.findAssetForURI(UserImageFile.NO_PROFILE_IMAGE_FILE_NAME)

        if (resource) {
            return createNewFile(UserImageFile.NO_PROFILE_IMAGE_FILE_NAME, resource.inputStream.bytes, 'image/jpeg', user)
        }
        null
    }
}