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
package db.migration.referencedata

import uk.ac.ox.softeng.maurodatamapper.referencedata.ReferenceDataModel

import groovy.util.logging.Slf4j
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

@Slf4j
class V5_0_1__update_paths extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        ReferenceDataModel.withNewTransaction {
            List<ReferenceDataModel> referenceDataModels = ReferenceDataModel.list()
            referenceDataModels.each { dm ->
                dm.checkPath()
                log.debug('Path Migrating {}', dm.path)
                dm.referenceDataElements.each { it.checkPath() }
                dm.referenceDataTypes.each{it.checkPath()}
                dm.referenceDataValues.each{it.checkPath()}
                dm.validate()
                dm.save(flush:true, validate:false)
            }
        }
    }
}
