package uk.ac.ox.softeng.maurodatamapper.security

interface SecurityPolicyManagerService {

    UserSecurityPolicyManager addSecurityForSecurableResource(SecurableResource securableResource, User creator, String resourceName)

    UserSecurityPolicyManager removeSecurityForSecurableResource(SecurableResource securableResource, User actor)

    UserSecurityPolicyManager updateSecurityForSecurableResource(SecurableResource securableResource, Set<String> changedProperties,
                                                                 User currentUser)

    UserSecurityPolicyManager retrieveUserSecurityPolicyManager(String userEmailAddress)

    UserSecurityPolicyManager reloadUserSecurityPolicyManager(String userEmailAddress)

    UserSecurityPolicyManager refreshAllUserSecurityPolicyManagersBySecurableResource(SecurableResource securableResource, User currentUser)

    void removeUserSecurityPolicyManager(String emailAddress)

}