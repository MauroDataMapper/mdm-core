package uk.ac.ox.softeng.maurodatamapper.security.basic

import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource
import uk.ac.ox.softeng.maurodatamapper.security.User

import grails.gorm.DetachedCriteria

/**
 * @since 20/11/2019
 */
class PublicAccessSecurityPolicyManager extends AbstractBasicSecurityPolicyManager {

    private final UnloggedUser user

    private PublicAccessSecurityPolicyManager() {
        user = UnloggedUser.instance
    }

    @Override
    User getUser() {
        user
    }

    @Override
    List<UUID> listReadableSecuredResourceIds(Class<? extends SecurableResource> securableResourceClass) {
        new DetachedCriteria(securableResourceClass).id().list()
    }

    @Override
    boolean userCanReadResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                  UUID owningSecureResourceId) {
        true
    }

    @Override
    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id) {
        true
    }

    @Override
    boolean userCanWriteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, String action) {
        true
    }

    @Override
    boolean userCanWriteResourceId(Class resourceClass, UUID id, Class<? extends SecurableResource> owningSecureResourceClass,
                                   UUID owningSecureResourceId, String action) {
        true
    }

    @Override
    boolean isApplicationAdministrator() {
        true
    }

    @Override
    boolean isLoggedIn() {
        true
    }

    static getInstance() {
        new PublicAccessSecurityPolicyManager()
    }
}
