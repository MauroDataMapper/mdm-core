package uk.ac.ox.softeng.maurodatamapper.core.traits.controller

import uk.ac.ox.softeng.maurodatamapper.core.MdmCoreGrailsPlugin
import uk.ac.ox.softeng.maurodatamapper.security.User
import uk.ac.ox.softeng.maurodatamapper.security.UserSecurityPolicyManager

import static org.springframework.http.HttpStatus.UNAUTHORIZED

/**
 * @since 25/11/2019
 */
//@SelfType([ResponseRenderer, ServletAttributes])
//@CompileStatic
trait UserSecurityPolicyManagerAware {

    User getCurrentUser() {
        currentUserSecurityPolicyManager.getUser()
    }

    Boolean isAuthenticated() {
        currentUserSecurityPolicyManager.isLoggedIn()
    }

    UserSecurityPolicyManager getCurrentUserSecurityPolicyManager() {
        // If no uspm stored in the params then return the default uspm bean
        params.currentUserSecurityPolicyManager ?:
        applicationContext.getBean(MdmCoreGrailsPlugin.DEFAULT_USER_SECURITY_POLICY_MANAGER_BEAN_NAME) as UserSecurityPolicyManager
    }

    boolean unauthorised() {
        render status: UNAUTHORIZED
        false
    }

    void setCurrentUserSecurityPolicyManager(UserSecurityPolicyManager userSecurityPolicyManager) {
        params.currentUserSecurityPolicyManager = userSecurityPolicyManager
    }
}
