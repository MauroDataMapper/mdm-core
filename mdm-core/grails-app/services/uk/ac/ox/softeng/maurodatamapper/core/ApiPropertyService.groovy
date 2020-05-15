package uk.ac.ox.softeng.maurodatamapper.core

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiPropertyEnum
import uk.ac.ox.softeng.maurodatamapper.security.User

import asset.pipeline.grails.AssetResourceLocator
import grails.gorm.transactions.Transactional
import groovy.util.logging.Slf4j
import org.springframework.core.io.Resource

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Transactional
@Slf4j
class ApiPropertyService {

    AssetResourceLocator assetResourceLocator

    List<ApiProperty> list() {
        ApiProperty.list()
    }

    Long count() {
        ApiProperty.count()
    }

    ApiProperty findByKey(String key) {
        ApiProperty.findByKey(key)
    }

    ApiProperty findByApiPropertyEnum(ApiPropertyEnum key) {
        findByKey(key.key)
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
        findAndUpdateByKey(key.key, updateValue, updatedBy)
    }

    ApiProperty findAndUpdateByKey(String key, updateValue, User updatedBy) {
        if (updateValue) {
            ApiProperty property = findByKey(key)
            if (property) {
                if (property.value != updateValue.toString()) {
                    property.value = updateValue.toString()
                }
            } else {
                property = new ApiProperty(key: key, value: updateValue.toString(), createdBy: updatedBy.emailAddress)
            }
            return save(property, updatedBy)
        }
        null
    }

    ApiProperty save(String key, value, User updatedBy) {
        value ? save(new ApiProperty(key: key, value: value.toString, createdBy: updatedBy.emailAddress), updatedBy) : null
    }

    ApiProperty save(ApiPropertyEnum apiProperty, value, User updatedBy) {
        save(apiProperty.key, value, updatedBy)
    }

    ApiProperty save(ApiProperty apiProperty, User updatedBy) {
        apiProperty.lastUpdatedBy = updatedBy.emailAddress
        apiProperty.save(flush: true)
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
                                        createdBy: createdBy.emailAddress)
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
                                                createdBy: createdBy.emailAddress)
                            }
                        log.debug('Found {} new default Api properties', newDefaultApiProperties.size())
                        if (newDefaultApiProperties) ApiProperty.saveAll(newDefaultApiProperties)
                    }
                } catch (FileNotFoundException ignored) {
                    log.warn('URL loading of API defaults file failed as asset not found')
                }
            }
        } catch (IOException e) {
            log.error('Something went wrong trying to load default API Properties', e)
        }
    }
}
