/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.federation.referencedata

import uk.ac.ox.softeng.maurodatamapper.core.model.ModelService
import uk.ac.ox.softeng.maurodatamapper.federation.test.BaseSubscribedModelServiceIntegrationSpec
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel
import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModelService

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.util.logging.Slf4j

@Slf4j
@Integration
@Rollback
class ReferenceDataModelSubscribedModelServiceIntegrationSpec extends BaseSubscribedModelServiceIntegrationSpec<ReferenceDataModel> {

    ReferenceDataModelService referenceDataModelService

    ModelService getModelService() {
        referenceDataModelService
    }

    String getModelType() {
        'ReferenceDataModel'
    }

    String getRemoteModelVersion1Json() {
        '''{
            "referenceDataModel": {
                "id": "c8023de6-5329-4b8b-8a1b-27c2abeaffcd",
                "label": "Remote Model",
                "lastUpdated": "2021-02-10T17:43:53.2Z",
                "author": "Remote Author",
                "organisation": "Remote Organisation",
                "documentationVersion": "1.0.0",
                "finalised": true,
                "dateFinalised": "2021-02-10T17:43:53.15Z",
                "modelVersion": "1.0.0",
                "authority": {
                    "id": "82429f5a-c3f9-45f2-8ed5-0426f5b0030d",
                    "url": "http://remotehost",
                    "label": "Remote Mauro Data Mapper"
                }
            },
            "exportMetadata": {
                "exportedBy": "Admin User",
                "exportedOn": "2021-02-14T12:32:37.522Z",
                "exporter": {
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.reference.provider.exporter",
                    "name": "ReferenceDataModelJsonExporterService",
                    "version": "3.0"
                }
            }
        }'''
    }

    String getRemoteModelVersion2Json() {
        '''{
            "referenceDataModel": {
                "id": "d8023de6-5329-4b8b-8a1b-27c2abeaffcd",
                "label": "Remote Model",
                "lastUpdated": "2021-02-11T17:43:53.2Z",
                "author": "Remote Author",
                "organisation": "Remote Organisation",
                "documentationVersion": "1.0.0",
                "finalised": true,
                "dateFinalised": "2021-02-10T17:43:53.15Z",
                "modelVersion": "2.0.0",
                "authority": {
                    "id": "82429f5a-c3f9-45f2-8ed5-0426f5b0030d",
                    "url": "http://remotehost",
                    "label": "Remote Mauro Data Mapper"
                }
            },
            "exportMetadata": {
                "exportedBy": "Admin User",
                "exportedOn": "2021-02-14T18:32:37.522Z",
                "exporter": {
                    "namespace": "uk.ac.ox.softeng.maurodatamapper.reference.provider.exporter",
                    "name": "ReferenceDataModelJsonExporterService",
                    "version": "3.0"
                }
            }
        }'''
    }    

    //Version link between Model versions 2 and 1
    String getRemoteModelVersion2VersionLinks() {
        '''{
            "count": 1,
            "items": [
                {
                    "id": "13746c1b-4f3b-4bac-9ca5-3a42445f4d22",
                    "linkType": "New Model Version Of",
                    "domainType": "VersionLink",
                    "sourceModel": {
                        "id": "d8023de6-5329-4b8b-8a1b-27c2abeaffcd",
                        "domainType": "ReferenceDataModel",
                        "label": "Remote Model"
                    },
                    "targetModel": {
                        "id": "c8023de6-5329-4b8b-8a1b-27c2abeaffcd",
                        "domainType": "ReferenceDataModel",
                        "label": "Remote Model"
                    }
                }
            ]
        }'''
    }

}

