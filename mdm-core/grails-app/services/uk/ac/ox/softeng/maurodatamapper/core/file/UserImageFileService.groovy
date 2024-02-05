/*
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package uk.ac.ox.softeng.maurodatamapper.core.file

import uk.ac.ox.softeng.maurodatamapper.core.model.file.CatalogueFileService
import uk.ac.ox.softeng.maurodatamapper.core.security.UserService
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.traits.domain.MdmDomain

import asset.pipeline.grails.AssetResourceLocator
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import static java.awt.RenderingHints.KEY_INTERPOLATION
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC

@Slf4j
@Transactional
class UserImageFileService implements CatalogueFileService<UserImageFile> {

    AssetResourceLocator assetResourceLocator

    @Autowired(required = false)
    UserService userService

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

    boolean userHasImage(UUID userId) {
        UserImageFile.countByUserId(userId)
    }

    /**
     * Find all resources by the defined user security policy manager. If none provided then assume no security policy in place in which case
     * everything is public.
     * @param userSecurityPolicyManager
     * @param pagination
     * @return
     */
    List<UserImageFile> findAllByUser(UserSecurityPolicyManager userSecurityPolicyManager, Map pagination = [:]) {
        if (userSecurityPolicyManager.isApplicationAdministrator()) return UserImageFile.list(pagination)
        [findByUserId(userSecurityPolicyManager.user.id)]
    }

    @Override
    UserImageFile createNewFile(String name, byte[] contents, String type, User user) {
        UserImageFile uif = createNewFileBase(name, contents, type, user.emailAddress)
        uif.userId = user.id
        uif
    }

    UserImageFile resizeImage(UserImageFile imageFile, int size) {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.fileContents))
        if (!image) return imageFile
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        def resize = new BufferedImage(size, size, image.type)
        resize.createGraphics().with {
            setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC)
            drawImage(image, 0, 0, size, size, null)
            dispose()
        }
        ImageIO.write(resize, 'png', outputStream)
        UserImageFile uif = createNewFileBase(imageFile.fileName, outputStream.toByteArray(), 'image/png', (imageFile as MdmDomain).createdBy)
        uif.userId = imageFile.userId
        uif
    }

    UserImageFile getDefaultNoProfileImageForUser(User user) {
        log.info('Loading default profile image')
        Resource resource = assetResourceLocator.findAssetForURI(UserImageFile.NO_PROFILE_IMAGE_FILE_NAME)

        if (resource) {
            return createNewFile(UserImageFile.NO_PROFILE_IMAGE_FILE_NAME, resource.inputStream.readAllBytes(), 'image/png', user)
        }
        null
    }

    UserImageFile addCreatedEditToUser(User creator, UserImageFile domain, UUID userId) {
        if (!userService) return domain
        userService.addImageCreatedEditToUser creator, domain, userId
    }

    UserImageFile addUpdatedEditToUser(User editor, UserImageFile domain, UUID userId, List<String> dirtyPropertyNames) {
        if (!userService) return domain
        userService.addImageUpdatedEditToUser editor, domain, userId, dirtyPropertyNames
    }

    UserImageFile addDeletedEditToUser(User deleter, UserImageFile domain, UUID userId) {
        if (!userService) return domain
        userService.addImageDeletedEditToUser deleter, domain, userId
    }
}