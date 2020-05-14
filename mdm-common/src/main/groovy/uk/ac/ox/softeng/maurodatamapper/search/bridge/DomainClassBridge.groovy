package uk.ac.ox.softeng.maurodatamapper.search.bridge

import org.grails.datastore.gorm.GormEntity
import org.hibernate.search.bridge.StringBridge
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @since 20/07/2018
 */
class DomainClassBridge implements StringBridge {

    Logger logger = LoggerFactory.getLogger(DomainClassBridge)

    @Override
    String objectToString(Object object) {
        if (object instanceof GormEntity) {
            return object.ident().toString()
        }
        logger.error('Bridge set up to convert object of type {} but it is not a GormEntity', object.getClass())
        return null
    }
}
