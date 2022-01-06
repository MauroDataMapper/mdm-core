import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty

import grails.gorm.PagedResultList

model {
    Iterable<ApiProperty> apiPropertyList
}

Iterable<ApiProperty> apl = apiPropertyList as Iterable<ApiProperty>

xmlDeclaration()

apiProperties {
    count(apl instanceof PagedResultList ? ((PagedResultList) apl).getTotalCount() : apl?.size() ?: 0)
    items {
        apl.each {ap ->
            layout 'apiProperty/_apiProperty.gml', apiProperty: ap
        }
    }
}
