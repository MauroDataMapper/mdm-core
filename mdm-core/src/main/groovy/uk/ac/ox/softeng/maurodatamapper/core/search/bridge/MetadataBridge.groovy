package uk.ac.ox.softeng.maurodatamapper.core.search.bridge

import uk.ac.ox.softeng.maurodatamapper.core.facet.Metadata
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.apache.lucene.document.Document
import org.grails.datastore.gorm.GormEntity
import org.hibernate.search.bridge.FieldBridge
import org.hibernate.search.bridge.LuceneOptions

class MetadataBridge implements FieldBridge {

    @Override
    void set(String name, Object value, Document document, LuceneOptions luceneOptions) {
        if (value &&
            value instanceof Collection &&
            Utils.parentClassIsAssignableFromChild(GormEntity, value.first().getClass()) &&
            ((GormEntity) value.first()).instanceOf(Metadata)) {
            Collection<Metadata> mds = value as Collection<Metadata>
            mds.each {metadata ->
                String fieldName = metadata.namespace + ' | ' + metadata.key
                String fieldValue = metadata.value
                luceneOptions.addFieldToDocument(fieldName, fieldValue, document)

            }
        }
    }
}