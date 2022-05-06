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
package uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.orm.mapping

import uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.pojo.bridge.DomainClassBridge
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.pojo.bridge.OffsetDateTimeBridge

import grails.plugins.hibernate.search.mapper.orm.mapping.GrailsHibernateSearchMappingConfigurer
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.GormEntity
import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext

import java.time.OffsetDateTime

/**
 * @since 12/10/2021
 */
@Slf4j
class MdmHibernateSearchMappingConfigurer extends GrailsHibernateSearchMappingConfigurer {
    @Override
    void configure(HibernateOrmMappingConfigurationContext context) {
        super.configure(context)

        log.info('Configuring Mauro HibernateSearch Mapping')
        context.bridges().exactType(GormEntity).valueBridge(new DomainClassBridge())
        context.bridges().exactType(OffsetDateTime).valueBridge(new OffsetDateTimeBridge())
    }
}
