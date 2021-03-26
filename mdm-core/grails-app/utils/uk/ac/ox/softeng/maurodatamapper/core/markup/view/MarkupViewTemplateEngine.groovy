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
package uk.ac.ox.softeng.maurodatamapper.core.markup.view

import grails.plugin.markup.view.MarkupViewConfiguration
import grails.plugin.markup.view.MarkupViewTemplateEngine as GrailsMarkupViewTemplateEngine
import groovy.text.markup.MarkupTemplateEngine
import groovy.text.markup.TemplateConfiguration
import groovy.text.markup.TemplateResolver

/**
 * @since 25/03/2021
 */
class MarkupViewTemplateEngine extends GrailsMarkupViewTemplateEngine {

    MarkupTemplateEngine.CachingTemplateResolver cachingTemplateResolver

    MarkupViewTemplateEngine(MarkupViewConfiguration config = new MarkupViewConfiguration(),
                             ClassLoader classLoader = Thread.currentThread().contextClassLoader) {
        super(config, classLoader)
        cachingTemplateResolver = new MarkupTemplateEngine.CachingTemplateResolver()
        cachingTemplateResolver.configure(classLoader, config)
        innerEngine = new MarkupTemplateEngine(classLoader, config, new TemplateResolver() {
            @Override
            void configure(ClassLoader templateClassLoader, TemplateConfiguration configuration) {
            }

            @Override
            URL resolveTemplate(String templatePath) throws IOException {
                // Try using the same technique in the original Grails engine
                // This will only work if the files are outside of a jar
                URL template = templateResolver.resolveTemplate(templatePath)
                if (template) return template

                try {
                    // Try using the groovy resolver which uses proper resource loading
                    return cachingTemplateResolver.resolveTemplate(templatePath)
                } catch (IOException ignored) {}

                // If the path starts with / its possible this is throwing the resource search so we remove it and try again
                if (templatePath.startsWith('/')) {
                    String adaptedTemplatePath = templatePath.replaceFirst('/', '')
                    return cachingTemplateResolver.resolveTemplate(adaptedTemplatePath)
                }
                null
            }
        })
    }
}
