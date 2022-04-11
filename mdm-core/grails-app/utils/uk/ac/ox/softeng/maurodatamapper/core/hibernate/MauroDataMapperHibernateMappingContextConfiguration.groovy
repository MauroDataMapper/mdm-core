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
package uk.ac.ox.softeng.maurodatamapper.core.hibernate

import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.DynamicHibernateMappingContext

import grails.config.Config
import grails.core.GrailsApplication
import grails.util.Holders
import groovy.util.logging.Slf4j
import org.grails.config.PropertySourcesConfig
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.orm.hibernate.cfg.HibernateMappingContextConfiguration
import org.hibernate.HibernateException
import org.hibernate.SessionFactory
import org.hibernate.cfg.AvailableSettings
import org.hibernate.cfg.Configuration
import org.hibernate.service.ServiceRegistry
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.PropertySource
import org.springframework.orm.hibernate5.SpringBeanContainer

import static uk.ac.ox.softeng.maurodatamapper.util.Utils.parentClassIsAssignableFromChild

/**
 * Reconfigures the mapping context after the persistent entities have been loaded.
 *
 * The primary purpose of this confirguration is to augment the domain mappings, it will apply all
 * {@link DynamicHibernateMappingContext} spring beans to every {@link PersistentEntity} which has been loaded into the session factory.
 *
 * Anyone wishing to implement their own {@link HibernateMappingContextConfiguration} must override this class instead otherwise the MDC will not
 * work
 *
 * Note that with grails in dev mode there is a restart functionality which creates a new classloader, this means using the call
 * DynamicHibernateMappingContext.isAssignableFrom(beanClass) will not work as the default class DynamicHibernateMappingContext belongs to a different
 * classloader. Therefore you need to obtain the current classloader to compare object classes.
 * @since 31/10/2019
 */
@SuppressWarnings('CatchNullPointerException')
@Slf4j
class MauroDataMapperHibernateMappingContextConfiguration extends HibernateMappingContextConfiguration {

    final List dynamicHibernateMappingContexts = []

    MauroDataMapperHibernateMappingContextConfiguration() {
        GrailsApplication grailsApplication = Holders.grailsApplication
        ConfigurableApplicationContext applicationContext = grailsApplication.parentContext as ConfigurableApplicationContext

        checkHibernateSearchConfig(grailsApplication.config)
        checkBeanProvider(grailsApplication.config, applicationContext)

        log.debug('Instantiating Mauro Data Mapper Hibernate mapping')

        List beans = applicationContext.beanDefinitionNames.collect {name ->
            try {
                Class beanClass = applicationContext.getType(name)
                if (parentClassIsAssignableFromChild(DynamicHibernateMappingContext, beanClass)) {
                    return applicationContext.getBean(name)
                }
            } catch (NullPointerException | ClassNotFoundException ignored) {}
            null
        }.findAll()
        dynamicHibernateMappingContexts.addAll(beans)
        log.debug('Loaded {} dynamic mapping contexts', dynamicHibernateMappingContexts.size())
    }

    @Override
    SessionFactory buildSessionFactory() throws HibernateException {
        configureMappingContext()
        super.buildSessionFactory()
    }

    @Override
    SessionFactory buildSessionFactory(ServiceRegistry serviceRegistry) throws HibernateException {
        configureMappingContext()
        super.buildSessionFactory(serviceRegistry)
    }

    protected void configureMappingContext() {
        Collection<PersistentEntity> persistentEntities = hibernateMappingContext.persistentEntities
        log.debug('Dynamically configuring mapping context before session factory is built for {} persistent entities', persistentEntities.size())
        for (PersistentEntity entity : persistentEntities) {
            dynamicHibernateMappingContexts.findAll {context ->
                context.handlesDomainClass(entity.javaClass)
            }.each {context ->
                context.updateDomainMapping(entity)
            }
        }
    }

    void checkBeanProvider(Config config, ConfigurableApplicationContext applicationContext) {
        if (config.getProperty(AvailableSettings.BEAN_CONTAINER)) return
        SpringBeanContainer beanContainer = new SpringBeanContainer(applicationContext.getBeanFactory())
        config.setAt(AvailableSettings.BEAN_CONTAINER, beanContainer)
        (this as Configuration).getProperties()[AvailableSettings.BEAN_CONTAINER] = beanContainer
    }

    void checkHibernateSearchConfig(Config config) {
        log.debug('Checking hibernate search v5 to v6 configuration')
        checkConfig config, 'hibernate.search.lucene_version', 'hibernate.search.backend.lucene_version'
        checkConfig config, 'hibernate.search.default.indexmanager', null
        checkConfig config, 'hibernate.search.default.directory_provider', null
        checkConfig config, 'hibernate.search.default.optimizer.operation_limit.max', null
        checkConfig config, 'hibernate.search.default.optimizer.transaction_limit.max', null

        checkConfig config, 'hibernate.search.default.indexBase', 'hibernate.search.backend.directory.root'

        String hsDir = config.getProperty('hibernate.search.backend.directory.root', String)
        log.info('Using hibernate search index directory of: {}', hsDir)
    }

    void checkConfig(Config config, String oldProp, String newProp) {
        if (config.containsProperty(oldProp)) {
            def oldValue = config.remove(oldProp)
            String title = 'Configuration'
            if (config instanceof PropertySourcesConfig) {
                PropertySource propertySource = config.getPropertySources().find { it.containsProperty(oldProp) } as PropertySource
                title = "[${propertySource.name}] configuration"
            }
            if (newProp) {
                log.warn('DEPRECATED : {} property [{}] has moved to [{}]', title, oldProp, newProp)
                config.setAt(newProp, oldValue)
                (this as Configuration).getProperties()[newProp] = oldValue
            } else {
                log.warn('DEPRECATED : {} property [{}] has been removed', title, oldProp)
            }
        }
    }
}