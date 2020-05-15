package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree

import uk.ac.ox.softeng.maurodatamapper.core.model.Container

/**
 * @since 07/01/2020
 */
class ContainerTreeItem extends TreeItem {

    Boolean deleted
    String containerType
    UUID containerId

    ContainerTreeItem(Container container) {
        super(container, container.id, container.label, container.domainType, null)
        containerId = container.parentId
        deleted = container.deleted
        containerType = container.domainType
        renderChildren = true
    }

    boolean isEmptyContainerTree() {
        if (childSet.isEmpty()) return true
        if (any {!(it instanceof ContainerTreeItem)}) return false
        findAll {it instanceof ContainerTreeItem}.every {((ContainerTreeItem) it).isEmptyContainerTree()}
    }

    void recursivelyRemoveEmptyChildContainers() {
        childSet.removeIf {it instanceof ContainerTreeItem && ((ContainerTreeItem) it).isEmptyContainerTree()}
        findAllChildContainerTrees().each {it.recursivelyRemoveEmptyChildContainers()}
    }

    Set<ContainerTreeItem> findAllChildContainerTrees() {
        findAll {it instanceof ContainerTreeItem} as Set<ContainerTreeItem>
    }
}
