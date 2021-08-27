import uk.ac.ox.softeng.maurodatamapper.datamodel.item.DataClass
import uk.ac.ox.softeng.maurodatamapper.path.Path


DataClass dc = dataClass as DataClass

'mdm:dataClass' {
    'mdm:label' dc.label
    'mdm:path' Path.toPathPrefix(dc, 'dm')
}
