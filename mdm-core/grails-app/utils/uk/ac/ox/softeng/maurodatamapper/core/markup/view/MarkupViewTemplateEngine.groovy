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
