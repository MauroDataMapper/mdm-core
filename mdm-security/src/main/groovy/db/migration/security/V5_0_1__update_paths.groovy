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
package db.migration.security

import uk.ac.ox.softeng.maurodatamapper.security.role.GroupRole

import groovy.util.logging.Slf4j
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

@Slf4j
class V5_0_1__update_paths extends BaseJavaMigration {

    @Override
    void migrate(Context context) throws Exception {

        GroupRole.withNewTransaction {
            List<GroupRole> roles = GroupRole.findAllByParentIsNull()
            roles.each { r ->
                r.checkPath()
                log.debug('Path Migrating {}', r.path)
                r.children.each { rc -> checkGroupRole(rc) }
                r.validate()
                r.save(flush:true, validate:false)
            }
        }
    }

    void checkGroupRole(GroupRole groupRole) {
        if (groupRole.children) {
            groupRole.children.each { checkGroupRole(it) }
        }
    }
}
