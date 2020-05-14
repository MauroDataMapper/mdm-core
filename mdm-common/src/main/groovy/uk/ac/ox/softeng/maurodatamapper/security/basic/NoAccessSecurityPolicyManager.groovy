package uk.ac.ox.softeng.maurodatamapper.security.basic

import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.User

/**
 * @since 19/11/2019
 */
class NoAccessSecurityPolicyManager extends AbstractBasicSecurityPolicyManager {

    private final UnloggedUser user

    private NoAccessSecurityPolicyManager() {
        user = UnloggedUser.instance
    }

    @Override
    User getUser() {
        UnloggedUser.instance
    }

    @Override
    List<UUID> listReadableSecuredResourceIds(Class<? extends SecurableResource> securableResourceClass) {
        []
    }

    @Override
    boolean userCanReadResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                  UUID owningSecureResourceId) {
        false
    }

    @Override
    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        false
    }

    @Override
    boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action) {
        false
    }

    @Override
    boolean userCanWriteResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                   UUID owningSecureResourceId, String action) {
        false
    }

    @Override
    boolean isApplicationAdministrator() {
        false
    }

    @Override
    boolean isLoggedIn() {
        false
    }

    static getInstance() {
        new NoAccessSecurityPolicyManager()
    }
}
