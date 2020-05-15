package uk.ac.ox.softeng.maurodatamapper.core.rest.transport.tree

import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem

/**
 * @since 07/01/2020
 */
class ModelItemTreeItem extends TreeItem {

    int order

    ModelItemTreeItem(ModelItem modelItem, Boolean childrenExist) {
        super(modelItem, modelItem.id, modelItem.label, modelItem.domainType, childrenExist)
        order = modelItem.getOrder()
    }

    @Override
    int compareTo(TreeItem that) {
        def res = this.domainTypeIndex <=> that.domainTypeIndex
        if (res != 0) return res

        ModelItemTreeItem other = that as ModelItemTreeItem
        res = this.hasChildren() <=> other.hasChildren()
        if (res == 0) res = this.order <=> other.order
        if (res == 0) res = this.label <=> other.label
        res
    }
}
