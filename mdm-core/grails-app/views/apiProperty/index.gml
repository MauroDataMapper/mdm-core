import uk.ac.ox.softeng.maurodatamapper.core.admin.ApiProperty

import grails.gorm.PagedResultList

Iterable<ApiProperty> apl = apiPropertyList as Iterable<ApiProperty>

xmlDeclaration()
index{
    count (apl instanceof PagedResultList ? ((PagedResultList) apl).getTotalCount() : apl?.size() ?: 0)
    apiProperties {
        apl.each {ap ->
            layout '_apiProperty.gml', apiProperty: ap
        }
    }
}