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
package uk.ac.ox.softeng.maurodatamapper.core

import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty
import uk.ac.ox.softeng.maurodatamapper.core.databinding.CsvDataBindingSourceCreator
import uk.ac.ox.softeng.maurodatamapper.core.flyway.MdmFlywayMigationStrategy
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.CoreSchemaMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.AnnotationAwareMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.BreadcrumbTreeMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.CatalogueItemClassifierAwareMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.CatalogueItemMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.ClassifierAwareMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.InformationAwareMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.MdmDomainMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.MetadataAwareMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.ReferenceFileAwareMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.RuleAwareMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.SemanticLinkAwareMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.domain.VersionLinkAwareMappingContext
import uk.ac.ox.softeng.maurodatamapper.core.json.view.JsonViewTemplateEngine
import uk.ac.ox.softeng.maurodatamapper.core.markup.view.MarkupViewTemplateEngine
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperProviderService
import uk.ac.ox.softeng.maurodatamapper.core.rest.render.MdmCsvApiPropertyCollectionRenderer
import uk.ac.ox.softeng.maurodatamapper.core.rest.render.MdmCsvApiPropertyRenderer
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.ClassifierFilterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.ClassifiersFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.CreatedAfterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.CreatedBeforeFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.UpdatedAfterFilter
import uk.ac.ox.softeng.maurodatamapper.core.rest.transport.search.searchparamfilter.UpdatedBeforeFilter
import uk.ac.ox.softeng.maurodatamapper.core.security.basic.DelegatingSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.hibernate.search.mapper.orm.mapping.MdmHibernateSearchMappingConfigurer
import uk.ac.ox.softeng.maurodatamapper.provider.plugin.MauroDataMapperPlugin
import uk.ac.ox.softeng.maurodatamapper.security.basic.NoAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.PublicAccessSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import grails.plugin.markup.view.MarkupViewConfiguration
import grails.plugins.Plugin
import grails.web.mime.MimeType
import groovy.util.logging.Slf4j
import org.grails.web.databinding.bindingsource.DataBindingSourceRegistry
import org.grails.web.servlet.view.CompositeViewResolver
import org.grails.web.servlet.view.SitemeshLayoutViewResolver
import org.hibernate.dialect.PostgreSQL94Dialect
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean
import org.springframework.util.ClassUtils

/**
 * @since 01/11/2017
 */
@Slf4j
class MdmCoreGrailsPlugin extends Plugin {

    static String DEFAULT_USER_SECURITY_POLICY_MANAGER_BEAN_NAME = 'defaultUserSecurityPolicyManager'

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = '5.3.2 > *'
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        'grails-app/views/error.gsp'
    ]

    def title = 'Mauro Data Mapper Core Plugin'
    // Headline display name of the plugin
    def author = 'Oliver Freeman'
    def authorEmail = 'oliver.freeman@bdi.ox.ac.uk'
    def description = '''\
The core domain, services and controllers for the Mauro Data Mapper backend.
This is basically the backend API.
'''

    // URL to the plugin's documentation
    def documentation = ''

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = 'APACHE'

    // Details of company behind the plugin (if there is one)
    def organization = [name: 'Oxford University BRC Informatics', url: 'www.ox.ac.uk']

    // Any additional developers beyond the author specified above.
    def developers = [[name: 'James Welch', email: 'james.welch@bdi.ox.ac.uk']]

    // Location of the plugin's issue tracker.
    def issueManagement = [system: 'YouTrack', url: 'https://maurodatamapper.myjetbrains.com']

    // Online location of the plugin's browseable source code.
    def scm = [url: 'https://github.com/mauroDataMapper/mdm-core']

    def dependsOn = [
        hibernate      : '7.3.0 > *',
        interceptors   : grailsVersion,
        services       : grailsVersion,
        controllers    : grailsVersion,
        assetPipeline  : '3.3.6 > *',
        jsonView       : '2.3.2 > *',
        markupView     : '2.3.2 > *',
        hibernateSearch: '3.0.0-SNAPSHOT > *'
    ]

    Closure doWithSpring() {
        {->

            // Dynamically update the Flyway Schemas
            mdmFlywayMigationStrategy MdmFlywayMigationStrategy

            /*
            Ensure that if using PG9.4+ then the reWriteBatchedInserts property is in the datasource properties
            This is needed to get PG to actually do batch processing of inserts.
            Whilst the hibernate settings get hibernate to perform batching of inserts and updates the batches are sent to PG which doesnt actually then
            perform batch inserts but individual sql inserts. This property gets the PG JDBC driver to rewrite the batches as 1 insert statement with multiple
            sets of values, which is what we want.
             */
            Map<String, Map> dataSources = config.getProperty('dataSources', Map<String, Map>, [:])
            boolean dataSourcesConfig = true
            if (!dataSources) {
                dataSourcesConfig = false
                def defaultDataSource = config.getProperty('dataSource', Map)
                if (defaultDataSource) {
                    dataSources['dataSource'] = defaultDataSource
                }
            }
            if (dataSources) {
                dataSources.each {String k, Map ds ->
                    String dialect = ds.dialect
                    Class dialectClass = ClassUtils.forName(dialect, this.class.classLoader)
                    if (Utils.parentClassIsAssignableFromChild(PostgreSQL94Dialect, dialectClass)) {
                        Map dsProperties = ds.properties ?: [:]
                        String connectionProperties = dsProperties.connectionProperties ?: ''
                        if (!connectionProperties.contains('reWriteBatchedInserts')) {
                            String key = dataSourcesConfig ? "dataSources.${k}.properties.connectionProperties" : 'dataSource.properties.connectionProperties'
                            connectionProperties = connectionProperties ? "${connectionProperties};reWriteBatchedInserts=true" : 'reWriteBatchedInserts=true'
                            config.setAt(key, connectionProperties)
                            dsProperties.connectionProperties = connectionProperties
                            ds['properties'] = dsProperties
                        }
                    }
                }
            }

            boolean rebuildIndexes = config.getProperty('grails.plugins.hibernatesearch.rebuildIndexOnStart', Boolean, false)
            if (rebuildIndexes) log.warn('Rebuilding search indexes')
            /*
             * Load in the HS analysers used by the hibernate search functionality
             */
            hibernateSearchMappingConfigurer(MdmHibernateSearchMappingConfigurer)

            // Ensure the uuid2 generator is used for mapping ids
            config.setAt('grails.gorm.default.mapping', {
                id generator: 'uuid2'
            })

            /*
             * Define the default user security policy manager
             * This would ideally be overridden by a security plugin, or possibly customised to define what the behaviour should be for
             * public access users.
             * The default is either no access, or complete access (and that means complete unfettered access to everything
             * maurodatamapper.security.public property defines what the default is
             */
            boolean publicAccess = config.getProperty('maurodatamapper.security.public', Boolean)
            if (publicAccess) {
                log.warn('Running in public access mode. All actions will be available to any user')
            }
            Class defaultUspmClass = publicAccess ? PublicAccessSecurityPolicyManager : NoAccessSecurityPolicyManager
            "${DEFAULT_USER_SECURITY_POLICY_MANAGER_BEAN_NAME}"(DelegatingSecurityPolicyManager, defaultUspmClass)

            /*
             * Define the mapping context beans
             */
            coreSchemaMappingContext CoreSchemaMappingContext
            informationAwareMappingContext InformationAwareMappingContext
            creatorAwareMappingContext MdmDomainMappingContext
            classifierAwareMappingContext ClassifierAwareMappingContext
            catalogueItemClassifierAwareMappingContext CatalogueItemClassifierAwareMappingContext
            breadcrumbTreeMappingContext BreadcrumbTreeMappingContext
            annotationAwareMappingContext AnnotationAwareMappingContext
            metadataAwareMappingContext MetadataAwareMappingContext
            referenceFileAwareMappingContext ReferenceFileAwareMappingContext
            ruleAwareMappingContext RuleAwareMappingContext
            semanticLinkAwareMappingContext SemanticLinkAwareMappingContext
            versionLinkAwareMappingContext VersionLinkAwareMappingContext
            catalogueItemMappingContext CatalogueItemMappingContext

            /*
             * Define the SearchParamFilter beans
             */
            classifierFilterFilter ClassifierFilterFilter
            classifiersFilter ClassifiersFilter
            createdAfterFilter CreatedAfterFilter
            createdBeforeFilter CreatedBeforeFilter
            updatedAfterFilter UpdatedAfterFilter
            updatedBeforeFilter UpdatedBeforeFilter

            /*
             * Define custom data binding beans
             */
            csvDataBindingSourceCreator(CsvDataBindingSourceCreator)

            /*
             * Get all MDM Plugins to execute their doWithSpring
             */
            MauroDataMapperProviderService.getServices(MauroDataMapperPlugin).each {MauroDataMapperPlugin plugin ->
                if (plugin.doWithSpring()) {
                    log.info("Adding plugin {} beans", plugin.name)
                    def c = plugin.doWithSpring()
                    c.setDelegate(owner.delegate)
                    c.call()
                }
            }

            sessionServiceHttpSessionListener(ServletListenerRegistrationBean) {
                listener = ref('sessionService')
            }

            markupTemplateEngine(MarkupViewTemplateEngine, ref('markupViewConfiguration'), applicationContext.classLoader)

            // If global exclude fields provided then we need to use the custom MDM port of the template engine as this is the only way
            // we can actually add the global exclusion fields to the generator
            if (grailsApplication.config.containsProperty('grails.views.excludeFields')) {
                jsonTemplateEngine(JsonViewTemplateEngine, grailsApplication, ref('jsonViewConfiguration'), applicationContext.classLoader)
            }

            csvApiPropertyRenderer(MdmCsvApiPropertyRenderer, ApiProperty) {
            }

            csvApiPropertyCollectionRenderer(MdmCsvApiPropertyCollectionRenderer, ApiProperty) {
            }

            //Ensure that MarkupViews (rather than the default XML renderer) is used when XML is requested
            markupViewConfiguration(MarkupViewConfiguration) {
                mimeTypes = [MimeType.XML.name, MimeType.HAL_XML.name, MimeType.TEXT_XML.name]
            }
        }

    }

    void doWithApplicationContext() {
        if (config.getProperty('env', String) == 'live') outputRuntimeArgs()
        else Utils.outputRuntimeArgs(MdmCoreGrailsPlugin)

        /*
         * Add custom data binding bean to the data binding registry
         */
        DataBindingSourceRegistry registry = applicationContext.getBean(DataBindingSourceRegistry.BEAN_NAME)
        registry.addDataBindingSourceCreator(applicationContext.getBean(CsvDataBindingSourceCreator))

        /**
         * Remove the SitemeshLayoutViewResolver as this resolves GSP files which we dont need or use
         */
        try {
            SitemeshLayoutViewResolver sitemeshLayoutViewResolver = applicationContext.getBean(SitemeshLayoutViewResolver)
            if (sitemeshLayoutViewResolver) {
                CompositeViewResolver compositeViewResolver = applicationContext.getBean(CompositeViewResolver.BEAN_NAME, CompositeViewResolver)
                compositeViewResolver.viewResolvers.remove(sitemeshLayoutViewResolver)
            }
        } catch (NoSuchBeanDefinitionException ignored) {
        }
    }

    void outputRuntimeArgs() {
        Utils.outputRuntimeArgs(MdmCoreGrailsPlugin)

        log.warn("Running with {} Grails config args", config.size())
        config.findAll {
            String prefix = it.key.indexOf('.') > 0 ? it.key.substring(0, it.key.indexOf('.')) : it.key
            !(it.value instanceof Map) && (prefix in ['database', 'apiUser', 'dataSource', 'env', 'simplejavamail'] ||
                                           it.key.startsWith('grails.cors') ||
                                           it.key.startsWith('hibernate.search') ||
                                           it.key.startsWith('emailService'))

        }.sort().each {k, v ->
            log.warn('{}={}', k, v)
        }
    }
}
