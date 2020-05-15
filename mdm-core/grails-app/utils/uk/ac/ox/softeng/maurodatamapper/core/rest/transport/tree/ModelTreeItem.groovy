package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree

import uk.ac.ox.softeng.maurodatamapper.core.model.Model
import uk.ac.ox.softeng.maurodatamapper.util.Version
import uk.ac.ox.softeng.maurodatamapper.core.facet.VersionLinkType

/**
 * @since 07/01/2020
 */
class ModelTreeItem extends TreeItem {

    Boolean finalised
    Boolean deleted
    Boolean superseded
    Version documentationVersion
    String modelType
    UUID containerId

    ModelTreeItem(Model model, String containerPropertyName) {
        this(model, containerPropertyName, model.hasChildren(), model.versionLinks.any {
            it.linkType == VersionLinkType.SUPERSEDED_BY_DOCUMENTATION ||
            it.linkType == VersionLinkType.SUPERSEDED_BY_MODEL
        })
    }

    ModelTreeItem(Model model, String containerPropertyName, Boolean childrenExist, Boolean isSuperseded) {
        this(model, model."$containerPropertyName".id, childrenExist, isSuperseded)
    }

    ModelTreeItem(Model model, UUID containerId, Boolean childrenExist, Boolean isSuperseded) {
        super(model, model.id, model.label, model.domainType, childrenExist)
        this.containerId = containerId
        deleted = model.deleted
        finalised = model.finalised
        superseded = isSuperseded
        documentationVersion = model.documentationVersion
        modelType = model.modelType
    }

    @Override
    int compareTo(TreeItem that) {
        def res = super.compareTo(that)
        if (res == 0 && that instanceof ModelTreeItem) res = this.documentationVersion <=> that.documentationVersion
        res
    }
}
