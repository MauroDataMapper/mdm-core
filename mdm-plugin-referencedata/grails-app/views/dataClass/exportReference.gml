import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass

DataClass dc = dataClass as DataClass

layout '/catalogueItem/_export.gml', catalogueItem: dc, addContents: false
'mdm:dataClassPath' DataClass.buildLabelPath(dc)