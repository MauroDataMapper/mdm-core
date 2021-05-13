package uk.ac.ox.softeng.maurodatamapper.profile.provider

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.model.CatalogueItem
import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile

import java.nio.charset.StandardCharsets

/**
 * @since 28/04/2021
 */
class DynamicJsonProfileProviderService extends JsonProfileProviderService{

    UUID dataModelId
    String dataModelLabel
    String dataModelDescription
    String dataModelVersion

    DynamicJsonProfileProviderService(MetadataService metadataService, DataModel dataModel){
        this.metadataService = metadataService
        this. dataModelId = dataModel.id
        this. dataModelLabel = dataModel.label
        this. dataModelDescription = dataModel.description
        this. dataModelVersion = dataModel.version
    }

    @Override
    JsonProfile createProfileFromEntity(CatalogueItem entity) {
        JsonProfile jsonProfile = new JsonProfile(getSections())
        jsonProfile.catalogueItemId = entity.id
        jsonProfile.catalogueItemDomainType = entity.domainType
        jsonProfile.catalogueItemLabel = entity.label
        List<Metadata> metadataList = metadataService.findAllByMultiFacetAwareItemIdAndNamespace(entity.id, this.getMetadataNamespace())

        jsonProfile.sections.each {section ->
            section.fields.each { field ->
                Metadata matchingField = metadataList.find {
                    it.key == field.metadataPropertyName || it.key == "${section.sectionName}/${field.fieldName}"
                }
                if(matchingField) {
                    field.currentValue = matchingField.value
                } else {
                    field.currentValue = ""
                }
                field.validate()
            }
        }
        jsonProfile
    }

    @Override
    Set<String> getKnownMetadataKeys() {
        Set<String> knownProperties = []
        getSections().each {section ->
            section.fields.each {field ->
                if(field.metadataPropertyName) {
                    knownProperties.add(field.metadataPropertyName)

                } else {
                    knownProperties.add("${section.sectionName}/${field.fieldName}")
                }
            }
        }
        return knownProperties
    }


    @Override
    void storeProfileInEntity(CatalogueItem entity, JsonProfile profile, String userEmailAddress) {
        JsonProfile emptyJsonProfile = new JsonProfile(getSections())

        emptyJsonProfile.sections.each {section ->
            ProfileSection submittedSection = profile.sections.find{it.sectionName == section.sectionName }
            if(submittedSection) {
                section.fields.each {field ->
                    ProfileField submittedField = submittedSection.fields.find {it.fieldName == field.fieldName }
                    if(submittedField) {
                        if(submittedField.currentValue  && submittedField.metadataPropertyName) {
                            entity.addToMetadata(metadataNamespace, field.metadataPropertyName, submittedField.currentValue,
                                                 userEmailAddress)
                        } else if(!field.metadataPropertyName) {
                            log.info("No metadataPropertyName set for field: " + field.fieldName)
                            String metadataPropertyName = "${section.sectionName}/${submittedField.fieldName}"
                            entity.addToMetadata(metadataNamespace, metadataPropertyName, submittedField.currentValue,
                                                 userEmailAddress)
                        } else if(!submittedField.currentValue) {
                            Metadata md = entity.metadata.find{
                                it.namespace == metadataNamespace && it.key == field.metadataPropertyName
                            }
                            if(md) {
                                entity.metadata.remove(md)
                                metadataService.delete(md)
                            }
                        }
                    }
                }
            }
        }
        //entity.addToMetadata(metadataNamespace, '_profiled', 'Yes', userEmailAddress)
        Metadata.saveAll(entity.metadata)
    }

    @Override
    String getJsonResourceFile() {
        return null
    }

    @Override
    String getMetadataNamespace() {
        if(!getProfileDataModel()) {
            return null
        }
        Metadata md = getProfileDataModel().metadata.find {md ->
            md.namespace == "uk.ac.ox.softeng.maurodatamapper.profile" &&
            md.key == "metadataNamespace"}
        if(md) {
            return md.value
        } else {
            log.error("Invalid namespace!!")
            return "invalid.namespace"
        }
    }

    @Override
    String getDisplayName() {
        return dataModelLabel
    }

    @Override
    String getName() {
        return URLEncoder.encode(dataModelLabel, StandardCharsets.UTF_8);
    }

    @Override
    String getVersion() {
        return dataModelVersion
    }

    @Override
    List<String> profileApplicableForDomains() {
        Metadata md = getProfileDataModel().metadata.find {md ->
            md.namespace == "uk.ac.ox.softeng.maurodatamapper.profile" &&
            md.key == "domainsApplicable"}
        if(md) {
            return md.value.tokenize(";")
        } else {
            return []
        }
    }

    DataModel getProfileDataModel() {
        DataModel.findById(dataModelId)
    }

    @Override
    UUID getDefiningDataModel() {
        return dataModelId
    }

    @Override
    String getDefiningDataModelLabel() {
        return dataModelLabel
    }

    @Override
    String getDefiningDataModelDescription() {
        return dataModelDescription
    }


    List<ProfileSection> getSections() {
        DataModel dm = getProfileDataModel()
        dm.dataClasses.sort { it.order }.collect() { dataClass ->
            new ProfileSection(
                sectionName: dataClass.label,
                sectionDescription: dataClass.description,
                fields: dataClass.dataElements.sort { it.order }.collect { dataElement ->
                    new ProfileField(
                        fieldName: dataElement.label,
                        description: dataElement.description,
                        metadataPropertyName: dataElement.metadata.find {
                            it.namespace == "uk.ac.ox.softeng.maurodatamapper.profile.dataelement" &&
                            it.key == "metadataPropertyName"
                        }?.value,
                        maxMultiplicity: dataElement.maxMultiplicity,
                        minMultiplicity: dataElement.minMultiplicity,
                        dataType: (dataElement.dataType instanceof EnumerationType) ? 'enumeration' : dataElement.dataType.label,
                        regularExpression: dataElement.metadata.find {
                            it.namespace == "uk.ac.ox.softeng.maurodatamapper.profile.dataelement" &&
                            it.key == "regularExpression"
                        }?.value,
                        allowedValues: (dataElement.dataType instanceof EnumerationType) ?
                                       ((EnumerationType) dataElement.dataType).enumerationValues.collect { it.key } : [],
                        currentValue: ""
                    )
                }
            )
        }
    }
}

/*


        ProfileProviderService<JsonProfile, CatalogueItem>() {

            MetadataService localMetadataService = grailsApplication.mainContext.getBean('metadataService')


            @Override
            void storeProfileInEntity(CatalogueItem entity, JsonProfile profile, String userEmailAddress) {
                JsonProfile emptyJsonProfile = new JsonProfile(getSections())

                emptyJsonProfile.sections.each {section ->
                    ProfileSection submittedSection = profile.sections.find{it.sectionName == section.sectionName }
                    if(submittedSection) {
                        section.fields.each {field ->
                            ProfileField submittedField = submittedSection.fields.find {it.fieldName == field.fieldName }
                            if(submittedField) {
                                if(submittedField.currentValue  && submittedField.metadataPropertyName) {
                                    entity.addToMetadata(metadataNamespace, field.metadataPropertyName, submittedField.currentValue,
                                            userEmailAddress)
                                } else if(!field.metadataPropertyName) {
                                    log.error("No metadataPropertyName set for field: " + field.fieldName)
                                } else if(!submittedField.currentValue) {
                                    Metadata md = entity.metadata.find{
                                        it.namespace == metadataNamespace && it.key == field.metadataPropertyName
                                    }
                                    if(md) {
                                        entity.metadata.remove(md)
                                        localMetadataService.delete(md)
                                    }
                                }
                            }
                        }
                    }
                }
                //entity.addToMetadata(metadataNamespace, '_profiled', 'Yes', userEmailAddress)
                Metadata.saveAll(entity.metadata)
            }

            @Override
            JsonProfile createProfileFromEntity(CatalogueItem entity) {
                JsonProfile jsonProfile = new JsonProfile(getSections())
                jsonProfile.catalogueItemId = entity.id
                jsonProfile.catalogueItemDomainType = entity.domainType
                jsonProfile.catalogueItemLabel = entity.label
                List<Metadata> metadataList = localMetadataService.findAllByMultiFacetAwareItemIdAndNamespace(entity.id, this.getMetadataNamespace())

                metadataList.each {}
                jsonProfile.sections.each {section ->
                    section.fields.each { field ->
                        Metadata matchingField = metadataList.find {it.key == field.metadataPropertyName }
                        if(matchingField) {
                            field.currentValue = matchingField.value
                        } else {
                            field.currentValue = ""
                        }
                        field.validate()
                    }
                }
                jsonProfile
            }

            @Override
            String getMetadataNamespace() {
                if(!getProfileDataModel()) {
                    return null
                }
                Metadata md = getProfileDataModel().metadata.find {md ->
                    md.namespace == "uk.ac.ox.softeng.maurodatamapper.profile" &&
                        md.key == "metadataNamespace"}
                if(md) {
                    return md.value
                } else {
                    log.error("Invalid namespace!!")
                    return "invalid.namespace"
                }
            }

            @Override
            String getDisplayName() {
                return dataModelLabel
            }

            @Override
            String getName() {
                return URLEncoder.encode(dataModelLabel, StandardCharsets.UTF_8);
            }

            @Override
            String getVersion() {
                return dataModelVersion
            }

            @Override
            List<String> profileApplicableForDomains() {
                Metadata md = getProfileDataModel().metadata.find {md ->
                    md.namespace == "uk.ac.ox.softeng.maurodatamapper.profile" &&
                            md.key == "domainsApplicable"}
                if(md) {
                    return md.value.tokenize(";")
                } else {
                    return []
                }
            }

            DataModel getProfileDataModel() {
                DataModel.findById(dataModelId)
            }

            @Override
            UUID getDefiningDataModel() {
                return dataModelId
            }

            @Override
            String getDefiningDataModelLabel() {
                return dataModelLabel
            }

            @Override
            String getDefiningDataModelDescription() {
                return dataModelDescription
            }


            List<ProfileSection> getSections() {
                DataModel dm = getProfileDataModel()
                dm.dataClasses.sort { it.order }.collect() { dataClass ->
                    new ProfileSection(
                            sectionName: dataClass.label,
                            sectionDescription: dataClass.description,
                            fields: dataClass.dataElements.sort { it.order }.collect { dataElement ->
                                new ProfileField(
                                        fieldName: dataElement.label,
                                        description: dataElement.description,
                                        metadataPropertyName: dataElement.metadata.find {
                                            it.namespace == "uk.ac.ox.softeng.maurodatamapper.profile.dataelement" &&
                                                    it.key == "metadataPropertyName"
                                        }?.value,
                                        maxMultiplicity: dataElement.maxMultiplicity,
                                        minMultiplicity: dataElement.minMultiplicity,
                                        dataType: (dataElement.dataType instanceof EnumerationType) ? 'enumeration' : dataElement.dataType.label,
                                        regularExpression: dataElement.metadata.find {
                                            it.namespace == "uk.ac.ox.softeng.maurodatamapper.profile.dataelement" &&
                                                    it.key == "regularExpression"
                                        }?.value,
                                        allowedValues: (dataElement.dataType instanceof EnumerationType) ?
                                                ((EnumerationType) dataElement.dataType).enumerationValues.collect { it.key } : [],
                                        currentValue: ""
                                )
                            }
                    )
                }
            }
        }
 */
