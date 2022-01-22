/*
 * Copyright 2020-2022 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package db.migration.datamodel


import uk.ac.ox.softeng.maurodatamapper.datamodel.DataModel
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.datamodel.item.datatype.EnumerationType

import groovy.util.logging.Slf4j
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

@Slf4j
class V5_0_1__update_paths extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        DataModel.withNewTransaction {
            List<DataModel> dataModels = DataModel.list()
            dataModels.each {dm ->
                dm.checkPath()
                dm.breadcrumbTree.update(dm)
                dm.skipValidation(true)
                log.debug('Path Migrating {}', dm.path)
                dm.fullSortOfChildren(dm.dataTypes)
                dm.fullSortOfChildren(dm.childDataClasses)
                dm.childDataClasses.each {dc -> checkDataClass(dc)}
                dm.dataTypes.each {
                    it.checkPath()
                    it.breadcrumbTree.update(it)
                    it.skipValidation(true)
                    if (it instanceof EnumerationType) {
                        it.fullSortOfChildren(it.enumerationValues)
                        it.enumerationValues.each {ev ->
                            ev.checkPath()
                            ev.breadcrumbTree.update(ev)
                            ev.skipValidation(true)
                        }
                    }
                }
                dm.save(flush: true, validate: false)

            }
        }
    }

    void checkDataClass(DataClass dataClass) {
        dataClass.checkPath()
        dataClass.skipValidation(true)
        dataClass.breadcrumbTree.update(dataClass)
        if (dataClass.dataElements) {
            dataClass.dataElements.each {
                it.checkPath()
                it.breadcrumbTree.update(it)
                it.skipValidation(true)
            }
            dataClass.fullSortOfChildren(dataClass.dataElements)
        }
        if (dataClass.dataClasses) {
            dataClass.fullSortOfChildren(dataClass.dataClasses)
            dataClass.dataClasses.each {
                checkDataClass(it)
                it.breadcrumbTree.update(it)
                it.skipValidation(true)
            }
        }
    }
}
