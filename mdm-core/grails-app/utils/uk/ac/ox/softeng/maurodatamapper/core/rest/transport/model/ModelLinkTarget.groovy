package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType

class ModelLinkTarget {
    String modelId
    String description

    ModelLinkTarget(UUID targetId, VersionLinkType versionLinkType) {
        modelId = targetId.toString()
        this.description = versionLinkType.label
    }
}
