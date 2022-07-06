import uk.ac.ox.softeng.maurodatamapper.core.model.ModelItem

model {
    ModelItem modelItem
    Boolean addContents
}
ModelItem export = modelItem as ModelItem
Boolean add = addContents == null ? true : addContents

layout '/catalogueItem/_export.gml', catalogueItem: export, addContents: add
if (export.idx != null) 'mdm:index' export.order