package uk.ac.ox.softeng.maurodatamapper.core.provider.exporter

import java.time.OffsetDateTime

class ExportMetadata {
    String namespace
    String name
    String version
    OffsetDateTime exportDate
    String exportedBy

    ExportMetadata() {
    }

    ExportMetadata(ExporterProviderService exporterProviderService, String firstName, String lastName) {
        this(exporterProviderService, firstName + " " + lastName)
    }

    ExportMetadata(ExporterProviderService exporterProviderService, String exportedBy) {
        this(exporterProviderService, exportedBy, OffsetDateTime.now())
    }

    ExportMetadata(ExporterProviderService exporterProviderService, String exportedBy, OffsetDateTime exportDate) {
        this.namespace = exporterProviderService.getNamespace()
        this.name = exporterProviderService.getName()
        this.version = exporterProviderService.getVersion()
        this.exportDate = exportDate
        this.exportedBy = exportedBy
    }
}