/*
 * Copyright 2020-2023 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.profile.object

import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileField
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileFieldDataType
import uk.ac.ox.softeng.maurodatamapper.profile.domain.ProfileSection
import uk.ac.ox.softeng.maurodatamapper.test.integration.BaseIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.util.GormUtils

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration

/**
 * @since 16/08/2021
 */
@Integration
@Rollback
class JsonProfileSpec extends BaseIntegrationSpec {

    void '1 test validation when no sections'() {

        when:
        Profile profile = new JsonProfile(id: UUID.randomUUID(), domainType: 'BasicModel', label: 'Test')

        then:
        !profile.validate()
        GormUtils.outputDomainErrors(messageSource, profile)
        profile.errors.hasFieldErrors('sections')

        and:
        profile.clearErrors()
        profile.validateCurrentValues()
    }

    void '2 test validation when section with no fields'() {

        when:
        Profile profile = new JsonProfile(id: UUID.randomUUID(), domainType: 'BasicModel', label: 'Test', sections: [
            new ProfileSection(name: 'section 1')
        ])

        then:
        !profile.validate()
        GormUtils.outputDomainErrors(messageSource, profile)
        profile.errors.hasFieldErrors('sections[0].fields')

        and:
        profile.clearErrors()
        profile.sections.each {
            it.clearErrors()
        }
        profile.validateCurrentValues()
    }

    void '3 test validation when section with mandatory fields'() {

        when:
        Profile profile = new JsonProfile(id: UUID.randomUUID(), domainType: 'BasicModel', label: 'Test', sections: [
            new ProfileSection(name: 'section 1', fields: [
                new ProfileField(fieldName: 'field1', metadataPropertyName: 'field1', minMultiplicity: 0, maxMultiplicity: 1, dataType: ProfileFieldDataType.STRING),
                new ProfileField(fieldName: 'field2', metadataPropertyName: 'field2', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.STRING),
            ])
        ])

        then:
        !profile.validate()
        GormUtils.outputDomainErrors(messageSource, profile)
        profile.errors.hasFieldErrors('sections[0].fields[1].currentValue')
        profile.errors.getFieldError('sections[0].fields[1].currentValue').code == 'null.message'

        then:
        profile.clearErrors()
        profile.sections.each {
            it.clearErrors()
            it.each {it.clearErrors()}
        }
        !profile.validateCurrentValues()
        GormUtils.outputDomainErrors(messageSource, profile)
        profile.errors.hasFieldErrors('sections[0].fields[1].currentValue')
        profile.errors.getFieldError('sections[0].fields[1].currentValue').code == 'null.message'
    }

    void '4 test validation when section with field in list'() {

        when:
        Profile profile = new JsonProfile(id: UUID.randomUUID(), domainType: 'BasicModel', label: 'Test', sections: [
            new ProfileSection(name: 'section 1', fields: [
                new ProfileField(fieldName: 'field1', metadataPropertyName: 'field1', minMultiplicity: 0, maxMultiplicity: 1, dataType: ProfileFieldDataType.STRING,
                                 allowedValues: ['a', 'b']),
                new ProfileField(fieldName: 'field2', metadataPropertyName: 'field2', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.STRING,
                                 allowedValues: ['a', 'b']),
                new ProfileField(fieldName: 'field3', metadataPropertyName: 'field3', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.STRING,
                                 allowedValues: ['a', 'b'], currentValue: 'c'),
                new ProfileField(fieldName: 'field4', metadataPropertyName: 'field4', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.STRING,
                                 allowedValues: ['a', 'b'], currentValue: 'a')
            ])
        ])

        then:
        !profile.validate()
        GormUtils.outputDomainErrors(messageSource, profile)
        profile.errors.hasFieldErrors('sections[0].fields[1].currentValue')
        profile.errors.getFieldError('sections[0].fields[1].currentValue').code == 'null.message'

        profile.errors.hasFieldErrors('sections[0].fields[2].currentValue')
        profile.errors.getFieldError('sections[0].fields[2].currentValue').code == 'not.inlist.message'

        and:
        profile.clearErrors()
        profile.sections.each {
            it.clearErrors()
            it.each {it.clearErrors()}
        }
        !profile.validateCurrentValues()
        GormUtils.outputDomainErrors(messageSource, profile)
        profile.errors.hasFieldErrors('sections[0].fields[1].currentValue')
        profile.errors.getFieldError('sections[0].fields[1].currentValue').code == 'null.message'

        profile.errors.hasFieldErrors('sections[0].fields[2].currentValue')
        profile.errors.getFieldError('sections[0].fields[2].currentValue').code == 'not.inlist.message'
    }

    void '5 test validation when section with field regex'() {

        when:
        Profile profile = new JsonProfile(id: UUID.randomUUID(), domainType: 'BasicModel', label: 'Test', sections: [
            new ProfileSection(name: 'section 1', fields: [
                new ProfileField(fieldName: 'field1', metadataPropertyName: 'field1', minMultiplicity: 0, maxMultiplicity: 1, dataType: ProfileFieldDataType.INT,
                                 regularExpression: '\\d{2}'),
                new ProfileField(fieldName: 'field2', metadataPropertyName: 'field2', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.INT,
                                 regularExpression: '\\d{2}'),
                new ProfileField(fieldName: 'field3', metadataPropertyName: 'field3', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.INT,
                                 regularExpression: '\\d{2}', currentValue: 'c'),
                new ProfileField(fieldName: 'field4', metadataPropertyName: 'field4', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.INT,
                                 regularExpression: '\\d{2}', currentValue: '11'),
            ])
        ])

        then:
        !profile.validate()
        GormUtils.outputDomainErrors(messageSource, profile)
        profile.errors.hasFieldErrors('sections[0].fields[1].currentValue')
        profile.errors.getFieldError('sections[0].fields[1].currentValue').code == 'null.message'

        profile.errors.hasFieldErrors('sections[0].fields[2].currentValue')
        profile.errors.getFieldError('sections[0].fields[2].currentValue').code == 'doesnt.match.message'

        and:
        profile.clearErrors()
        profile.sections.each {
            it.clearErrors()
            it.each {it.clearErrors()}
        }
        !profile.validateCurrentValues()
        GormUtils.outputDomainErrors(messageSource, profile)
        profile.errors.hasFieldErrors('sections[0].fields[1].currentValue')
        profile.errors.getFieldError('sections[0].fields[1].currentValue').code == 'null.message'

        profile.errors.hasFieldErrors('sections[0].fields[2].currentValue')
        profile.errors.getFieldError('sections[0].fields[2].currentValue').code == 'doesnt.match.message'
    }

    void '5 test validation when section with field typing'() {

        when:
        Profile profile = new JsonProfile(id: UUID.randomUUID(), domainType: 'BasicModel', label: 'Test', sections: [
            new ProfileSection(name: 'section 1', fields: [
                new ProfileField(fieldName: 'field1', metadataPropertyName: 'field1', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.INT,
                                 currentValue: 'c'),
                new ProfileField(fieldName: 'field2', metadataPropertyName: 'field2', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.STRING,
                                 currentValue: 'hello'),
                new ProfileField(fieldName: 'field3', metadataPropertyName: 'field3', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.BOOLEAN,
                                 currentValue: 'blob'),
                new ProfileField(fieldName: 'field4', metadataPropertyName: 'field4', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.INT,
                                 currentValue: '11'),
                new ProfileField(fieldName: 'field5', metadataPropertyName: 'field5', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.DATE,
                                 currentValue: '31/12/1999'),
                new ProfileField(fieldName: 'field5', metadataPropertyName: 'field5', minMultiplicity: 1, maxMultiplicity: 1, dataType: ProfileFieldDataType.DATE,
                                 currentValue: '2000-01-01'),
                new ProfileField(fieldName: 'field7', metadataPropertyName: 'field7', minMultiplicity: 1, maxMultiplicity: 1, dataType: 'Custom Type',
                                 currentValue: 'custom123')
            ])
        ])

        then:
        !profile.validate()
        GormUtils.outputDomainErrors(messageSource, profile)
        profile.errors.hasFieldErrors('sections[0].fields[0].currentValue')
        profile.errors.getFieldError('sections[0].fields[0].currentValue').code == 'typeMismatch'

        profile.errors.hasFieldErrors('sections[0].fields[2].currentValue')
        profile.errors.getFieldError('sections[0].fields[2].currentValue').code == 'typeMismatch'

        profile.errors.errorCount == 2

        and:
        profile.clearErrors()
        profile.sections.each {
            it.clearErrors()
            it.each {it.clearErrors()}
        }
        !profile.validateCurrentValues()
        GormUtils.outputDomainErrors(messageSource, profile)
        profile.errors.hasFieldErrors('sections[0].fields[0].currentValue')
        profile.errors.getFieldError('sections[0].fields[0].currentValue').code == 'typeMismatch'

        profile.errors.hasFieldErrors('sections[0].fields[2].currentValue')
        profile.errors.getFieldError('sections[0].fields[2].currentValue').code == 'typeMismatch'

        profile.errors.errorCount == 2
    }


    @Override
    void setupDomainData() {

    }
}
