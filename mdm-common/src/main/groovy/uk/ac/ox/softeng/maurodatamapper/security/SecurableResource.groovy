package uk.ac.ox.softeng.maurodatamapper.security

import uk.ac.ox.softeng.maurodatamapper.util.Utils

/**
 * @since 09/10/2019
 */
trait SecurableResource {

    UUID getResourceId() {
        Utils.toUuid(ident())
    }

    abstract String getDomainType()
}
