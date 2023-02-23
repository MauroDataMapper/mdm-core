/*
 * Copyright 2020-2023 University of Oxford and NHS England
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
package uk.ac.ox.softeng.maurodatamapper.core.admin

// import asset.pipeline.AssetPipelineConfigHolder
import uk.ac.ox.softeng.maurodatamapper.security.User

import asset.pipeline.grails.AssetResourceLocator
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.grails.web.mapping.DefaultLinkGenerator
import org.springframework.core.io.Resource

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Transactional
@Slf4j
class ApiPropertyService {

    AssetResourceLocator assetResourceLocator

    // This is autowired does not need the annotation
    DefaultLinkGenerator grailsLinkGenerator

    List<ApiProperty> list(Map pagination = [:]) {
        ApiProperty.by().list(pagination)
    }

    Long count() {
        ApiProperty.count()
    }

    void delete(ApiProperty apiProperty) {
        apiProperty.delete(flush: true)
    }

    static ApiProperty findByKey(String key) {
        ApiProperty.findByKey(key)
    }

    static ApiProperty findByApiPropertyEnum(ApiPropertyEnum key) {
        findByKey(key.key)
    }

    List<ApiProperty> findAllByPubliclyVisible(Map pagination) {
        ApiProperty.byPubliclyVisible().list(pagination)
    }

    List<ApiProperty> listAndUpdateApiProperties(Map<String, String> updateValues, User updatedBy) {
        List<ApiProperty> existing = list()
        updateValues.each {update ->
            if (update.value) {
                ApiProperty property = findByKey(update.key)
                if (property) {
                    if (property.value != update.value) {
                        property.value = update.value
                    }
                } else {
                    existing.add(new ApiProperty(key: update.key, value: update.value, createdBy: updatedBy.emailAddress))
                }
            }
        }
        ApiProperty.saveAll(existing)
        existing
    }

    ApiProperty findAndUpdateByApiPropertyEnum(ApiPropertyEnum key, updateValue, User updatedBy) {
        findAndUpdateByKey(key.key, updateValue, updatedBy, ApiProperty.extractDefaultCategoryFromKey(key.key))
    }

    ApiProperty findAndUpdateByKey(String key, updateValue, User updatedBy, String category = null) {
        if (updateValue) {
            ApiProperty property = findByKey(key)
            if (property) {
                if (property.value != updateValue.toString()) {
                    property.value = updateValue.toString()
                }
            } else {
                property = new ApiProperty(key: key, value: updateValue.toString(), createdBy: updatedBy.emailAddress, category: category)
            }
            return save(property, updatedBy)
        }
        null
    }

    ApiProperty save(String key, value, User updatedBy, String category = null) {
        value ? save(new ApiProperty(key: key, value: value.toString, createdBy: updatedBy.emailAddress, category: category), updatedBy) : null
    }

    ApiProperty save(ApiPropertyEnum apiProperty, value, User updatedBy, String category = null) {
        save(apiProperty.key, value, updatedBy, category)
    }

    ApiProperty save(ApiProperty apiProperty, User updatedBy) {
        apiProperty.lastUpdatedBy = updatedBy.emailAddress
        apiProperty.save(flush: true)
    }

    void updateLinkGeneratorWithSiteUrl(ApiProperty apiProperty) {
        if (!apiProperty) return
        if (apiProperty.key != ApiPropertyEnum.SITE_URL.key) return
        grailsLinkGenerator.setConfiguredServerBaseURL(apiProperty.value)
    }

    void checkAndSetSiteUrl(String configServerUrl, String configContextPath, User user) {
        ApiProperty siteUrlProperty = findByApiPropertyEnum(ApiPropertyEnum.SITE_URL)
        // If no site url property but a config server url then create a new property
        if (!siteUrlProperty && configServerUrl) {
            String contextPathPart = configContextPath ? "/$configContextPath" : ''
            siteUrlProperty = new ApiProperty(key: ApiPropertyEnum.SITE_URL.key,
                                              value: "${configServerUrl}${contextPathPart}",
                                              createdBy: user.emailAddress,
                                              category: ApiProperty.extractDefaultCategoryFromKey(ApiPropertyEnum.SITE_URL.key)).save()
        }
        // If a site url property exists either from db or now created then update the link generator
        if (siteUrlProperty) {
            updateLinkGeneratorWithSiteUrl(siteUrlProperty)
        }
    }

    void loadLegacyPropertiesFromDefaultsFileIntoDatabase(String path, User createdBy) {
        try {
            // Override with any values saved from runtime
            Path savedDefaultsPath = Paths.get(path, 'savedDefaults.properties')
            if (Files.exists(savedDefaultsPath)) {
                Properties legacyProperties = new Properties()
                log.info('Loading previously saved API properties')
                legacyProperties.load(Files.newInputStream(savedDefaultsPath))
                if (legacyProperties) {
                    List<ApiProperty> legacyApiProperties = legacyProperties.findAll {it.value}.collect {
                        new ApiProperty(key: it.key, value: it.value,
                                        createdBy: createdBy.emailAddress,
                                        category: ApiProperty.extractDefaultCategoryFromKey(it.key))
                    }
                    List<ApiProperty> existingApiProperties = list()

                    legacyApiProperties.each {legacy ->

                        ApiProperty existing = existingApiProperties.find {it.key == legacy.key && it.value != legacy.value}
                        if (existing) {
                            log.warn('Updating {} from the default value to the legacy stored value', existing.key)
                            existing.value = legacy.value
                            existing.lastUpdatedBy = createdBy.emailAddress
                        } else {
                            log.warn('Adding {} from the legacy values as {}', legacy.key)
                            existingApiProperties.add(legacy)
                        }
                    }
                    log.debug('Saving all api properties')
                    ApiProperty.saveAll(existingApiProperties)
                }
                log.warn('Removing legacy defaults file')
                Files.delete(savedDefaultsPath)
            } else {
                log.debug('No legacy file exists')
            }
        } catch (IOException e) {
            log.error('Something went wrong trying to load legacy API Properties', e)
        }
    }

    void loadDefaultPropertiesIntoDatabase(User createdBy) {
        log.info('Loading default API properties')


        // TODO: Once we're happy that this issue has been solved, the extra logging can be removed
/*        log.info("Class Search Directories: ")
        assetResourceLocator.classSearchDirectories.each {
            log.info(it)
        }
        log.info("Resource Search Directories: ")
        assetResourceLocator.resourceSearchDirectories.each {
            log.info(it)
        }
        log.info("Plugin List: ")
        assetResourceLocator.pluginManager.pluginList.each {
            log.info(it.toString())
        }
        log.info("Manifest properties found: ")
        log.info(AssetPipelineConfigHolder.manifest.toString())

        log.info("Is war: " + assetResourceLocator.warDeployed)
        log.info(assetResourceLocator.defaultResourceLoader.toString())
*/
        Resource resource = assetResourceLocator.findAssetForURI('defaults.properties')
        try {
            if (resource?.exists()) {
                try {
                    Properties defaultProperties = new Properties()
                    defaultProperties.load(resource.inputStream)

                    if (defaultProperties) {
                        List<String> existingApiPropertyKeys = list()*.key
                        List<ApiProperty> newDefaultApiProperties = defaultProperties
                            .findAll {!(it.key in existingApiPropertyKeys)}
                            .collect {
                                new ApiProperty(key: it.key, value: it.value,
                                                createdBy: createdBy.emailAddress,
                                                category: ApiProperty.extractDefaultCategoryFromKey(it.key))
                            }
                        log.debug('Found {} new default Api properties', newDefaultApiProperties.size())
                        if (newDefaultApiProperties) ApiProperty.saveAll(newDefaultApiProperties)
                    }
                } catch (FileNotFoundException ignored) {
                    log.warn('URL loading of API defaults file failed as asset not found')
                }
            } else {
                log.error('Cannot find the defaults.properties file')
            }
        } catch (IOException e) {
            log.error('Something went wrong trying to load default API Properties', e)
        }
    }
}
