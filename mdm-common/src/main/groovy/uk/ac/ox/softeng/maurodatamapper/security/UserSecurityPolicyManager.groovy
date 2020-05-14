package uk.ac.ox.softeng.maurodatamapper.security

/**
 * @since 18/11/2019
 */
interface UserSecurityPolicyManager {

    User getUser()

    List<UUID> listReadableSecuredResourceIds(Class<? extends SecurableResource> securableResourceClass)

    boolean userCanReadResourceId(Class resourceClass, UUID id,
                                  Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId)

    boolean userCanCreateResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId)

    boolean userCanEditResourceId(Class resourceClass, UUID id,
                                  Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId)

    boolean userCanDeleteResourceId(Class resourceClass, UUID id,
                                    Class<? extends SecurableResource> owningSecureResourceClass, UUID owningSecureResourceId)

    boolean userCanReadSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id)

    boolean userCanCreateSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id)

    boolean userCanEditSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id)

    boolean userCanDeleteSecuredResourceId(Class<? extends SecurableResource> securableResourceClass, UUID id, boolean permanent)

    List<String> userAvailableActions(Class resourceClass, UUID id)

    List<String> userAvailableActions(String domainType, UUID id)

    boolean isApplicationAdministrator()

    boolean isLoggedIn()
}