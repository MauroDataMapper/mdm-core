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
package uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer

import uk.ac.ox.softeng.maurodatamapper.core.container.ClassifierService
import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.core.provider.importer.ImporterProviderService
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlow
import uk.ac.ox.softeng.maurodatamapper.dataflow.DataFlowService
import uk.ac.ox.softeng.maurodatamapper.dataflow.provider.importer.parameter.DataFlowImporterProviderServiceParameters
import uk.ac.ox.softeng.maurodatamapper.security.User

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

/**
 * @since 11/01/2021
 */
@CompileStatic
abstract class DataFlowImporterProviderService<T extends DataFlowImporterProviderServiceParameters>
    extends ImporterProviderService<DataFlow, T> {

    @Autowired
    DataFlowService dataFlowService

    @Autowired
    ClassifierService classifierService

    abstract DataFlow importDataFlow(User currentUser, T params)

    abstract List<DataFlow> importDataFlows(User currentUser, T params)

    @CompileDynamic
    @Override
    DataFlow importDomain(User currentUser, T params) {
        DataFlow dataFlow = importDataFlow(currentUser, params)
        if (!dataFlow) return null
        if (params.modelName) dataFlow.label = params.modelName
        checkImport(currentUser, dataFlow)
    }

    @CompileDynamic
    @Override
    List<DataFlow> importDomains(User currentUser, T params) {
        List<DataFlow> dataFlows = importDataFlows(currentUser, params)
        dataFlows?.collect { checkImport(currentUser, it) }
    }

    @Override
    String getProviderType() {
        "DataFlow${ProviderType.IMPORTER.name}"
    }

    DataFlow checkImport(User currentUser, DataFlow dataFlow) {
        classifierService.checkClassifiers(currentUser, dataFlow)

        dataFlow.dataClassComponents?.each { dcc ->
            classifierService.checkClassifiers(currentUser, dcc)

            dcc.dataElementComponents?.each { dec ->
                classifierService.checkClassifiers(currentUser, dec)
            }
        }

        dataFlow
    }
}
