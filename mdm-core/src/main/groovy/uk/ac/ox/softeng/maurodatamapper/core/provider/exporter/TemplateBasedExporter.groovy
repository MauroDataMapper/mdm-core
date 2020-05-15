package uk.ac.ox.softeng.maurodatamapper.core.provider.exporter

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException

import grails.views.ResolvableGroovyTemplateEngine
import groovy.text.Template
import groovy.util.logging.Slf4j

/**
 * @since 16/11/2017
 */
@Slf4j
trait TemplateBasedExporter {

    abstract ResolvableGroovyTemplateEngine getTemplateEngine()

    String getExportViewPath() {
        '/exportModel/export'
    }

    ByteArrayOutputStream exportModel(ExportModel exportModel, String format) {
        Template template = templateEngine.resolveTemplate(exportViewPath)

        if (!template) {
            log.error('Could not find template for format {} at path {}', format, exportViewPath)
            throw new ApiInternalException('TBE02', "Could not find template for format ${format} at path ${exportViewPath}")
        }

        def writable = template.make(exportModel: exportModel)
        def sw = new StringWriter()
        writable.writeTo(sw)
        ByteArrayOutputStream os = new ByteArrayOutputStream()
        os.write(sw.toString().bytes)
        os
    }
}
