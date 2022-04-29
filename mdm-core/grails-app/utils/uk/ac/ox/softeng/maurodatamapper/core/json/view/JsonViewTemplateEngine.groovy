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
package uk.ac.ox.softeng.maurodatamapper.core.json.view

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import grails.core.GrailsApplication
import grails.plugin.json.builder.JsonGenerator
import grails.plugin.json.converters.InstantJsonConverter
import grails.plugin.json.converters.LocalDateJsonConverter
import grails.plugin.json.converters.LocalDateTimeJsonConverter
import grails.plugin.json.converters.LocalTimeJsonConverter
import grails.plugin.json.converters.OffsetDateTimeJsonConverter
import grails.plugin.json.converters.OffsetTimeJsonConverter
import grails.plugin.json.converters.PeriodJsonConverter
import grails.plugin.json.converters.ZonedDateTimeJsonConverter
import grails.plugin.json.view.JsonViewConfiguration
import grails.plugin.json.view.JsonViewGeneratorConfiguration
import grails.plugin.json.view.template.JsonViewTemplate
import grails.views.ViewConfiguration
import grails.views.WritableScriptTemplate
import grails.views.api.GrailsView
import groovy.text.Template
import groovy.util.logging.Slf4j
import org.springframework.core.OrderComparator

/**
 * A template engine for parsing JSON views.
 * Complete port of the GrailsViews JsonViewTemplateEngine as we need to add options to exclude fields based on
 * configuration and this can't be done by extending the original.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@Slf4j
@SuppressFBWarnings('NM_SAME_SIMPLE_NAME_AS_SUPERCLASS')
class JsonViewTemplateEngine extends grails.plugin.json.view.JsonViewTemplateEngine {

    final JsonGenerator mdmGenerator

    /**
     * Constructs a JsonTemplateEngine with the default configuration
     */
    JsonViewTemplateEngine() {
        this(null, new JsonViewConfiguration(), Thread.currentThread().contextClassLoader)
    }

    /**
     * Constructs a JsonTemplateEngine with the default configuration
     */
    JsonViewTemplateEngine(ClassLoader classLoader) {
        this(null, new JsonViewConfiguration(), classLoader)
    }

    /**
     * Constructs a JsonTemplateEngine with a custom base class
     *
     * @param baseClassName The name of the base class
     */
    JsonViewTemplateEngine(GrailsApplication grailsApplication, ViewConfiguration configuration, ClassLoader classLoader) {
        super(configuration, classLoader)
        log.info('Using MDM custom json view engine')

        JsonGenerator.Options options = new JsonGenerator.Options()
        JsonViewGeneratorConfiguration config = ((JsonViewConfiguration) configuration).generator

        if (!config.escapeUnicode) {
            options.disableUnicodeEscaping()
        }
        Locale locale
        String[] localeData = config.locale.split('/')
        if (localeData.length > 1) {
            locale = new Locale(localeData[0], localeData[1])
        } else {
            locale = new Locale(localeData[0])
        }

        options.dateFormat(config.dateFormat, locale)
        options.timezone(config.timeZone)

        ServiceLoader<JsonGenerator.Converter> loader = ServiceLoader.load(JsonGenerator.Converter.class)
        List<JsonGenerator.Converter> converters = []
        for (JsonGenerator.Converter converter : loader) {
            converters.add(converter)
        }
        converters.add(new InstantJsonConverter() as JsonGenerator.Converter)
        converters.add(new LocalDateJsonConverter() as JsonGenerator.Converter)
        converters.add(new LocalDateTimeJsonConverter() as JsonGenerator.Converter)
        converters.add(new LocalTimeJsonConverter() as JsonGenerator.Converter)
        converters.add(new OffsetDateTimeJsonConverter() as JsonGenerator.Converter)
        converters.add(new OffsetTimeJsonConverter() as JsonGenerator.Converter)
        converters.add(new PeriodJsonConverter() as JsonGenerator.Converter)
        converters.add(new ZonedDateTimeJsonConverter() as JsonGenerator.Converter)
        OrderComparator.sort(converters)
        converters.each {
            options.addConverter(it)
        }

        if (grailsApplication) {
            def exclusions = grailsApplication.config.getProperty('grails.views.excludeFields')
            if (exclusions) {
                if (exclusions instanceof String) {
                    options.excludeFieldsByName(exclusions)
                } else if (exclusions instanceof Collection) {
                    options.excludeFieldsByName(exclusions)
                } else {
                    log.warn('Unknown type for field exclusions [{}]', exclusions.class)
                }
            }
        }

        this.mdmGenerator = options.build()
    }

    @Override
    protected WritableScriptTemplate createTemplate(Class<? extends Template> cls, File sourceFile) {
        def template = new JsonViewTemplate((Class<? extends GrailsView>) cls, sourceFile)
        template.generator = mdmGenerator
        template.jsonApiIdRenderStrategy = this.jsonApiIdRenderStrategy
        return initializeTemplate(template, sourceFile)
    }
}
