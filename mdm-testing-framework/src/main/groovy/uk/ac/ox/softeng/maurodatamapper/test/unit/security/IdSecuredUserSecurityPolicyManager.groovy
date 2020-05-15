package uk.ac.ox.softeng.maurodatamapper.test.unit.security

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiNotYetImplementedException
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.basic.AbstractBasicSecurityPolicyManager
import uk.ac.ox.softeng.maurodatamapper.security.basic.UnloggedUser

/**
 * @since 10/02/2020
 */
class IdSecuredUserSecurityPolicyManager extends AbstractBasicSecurityPolicyManager {

    UUID unknownId
    UUID readAccessId
    UUID noAccessId
    UUID writeAccessId
    User user

    IdSecuredUserSecurityPolicyManager(User user, UUID unknownId, UUID noAccessId, UUID readAccessId, UUID writeAccessId) {
        this.unknownId = unknownId
        this.readAccessId = readAccessId
        this.noAccessId = noAccessId
        this.writeAccessId = writeAccessId
        this.user = user
    }

    @Override
    List<UUID> listReadableSecuredResourceIds(Class<? extends SecurableResource> securableResourceClass) {
        throw new ApiNotYetImplementedException('IDUSPXX', 'Listing of readable secured resource ids')
    }

    @Override
    boolean userCanReadResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                  UUID owningSecureResourceId) {
        id in [readAccessId, writeAccessId] || owningSecureResourceId in [readAccessId, writeAccessId]
    }

    @Override
    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        id in [readAccessId, writeAccessId]
    }

    @Override
    boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action) {
        id == writeAccessId
    }

    @Override
    boolean userCanWriteResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                   UUID owningSecureResourceId, String action) {
        id == writeAccessId || owningSecureResourceId == writeAccessId
    }

    @Override
    boolean isApplicationAdministrator() {
        user.emailAddress == 'admin@maurodatamapper.com'
    }

    @Override
    boolean isLoggedIn() {
        UnloggedUser.instance.emailAddress != user.emailAddress && user.emailAddress != 'pending@test.com'
    }
}
