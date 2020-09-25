package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.model

import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType
import uk.ac.ox.softeng.maurodatamapper.core.model.Model

class VersionTreeModel {
    String modelId
    String label
    String branchName
    Boolean newDocumentationVersion
    Boolean newBranchModelVersion
    Boolean newFork
    List<String> targets

    VersionTreeModel(Model model, VersionLinkType versionLinkType) {
        modelId = model.id.toString()
        label = model.label
        branchName = model.branchName
        newDocumentationVersion = versionLinkType == VersionLinkType.NEW_DOCUMENTATION_VERSION_OF
        newBranchModelVersion = versionLinkType == VersionLinkType.NEW_MODEL_VERSION_OF
        newFork = versionLinkType == VersionLinkType.NEW_FORK_OF
        targets = []
    }

    void addTarget(UUID targetId){
        targets.add(targetId.toString())
    }
}
