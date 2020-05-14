package uk.ac.ox.softeng.maurodatamapper.security.basic

import uk.ac.ox.softeng.maurodatamapper.security.User

/**
 * @since 21/10/2019
 */
class UnloggedUser implements User {

    public static final String UNLOGGED_EMAIL_ADDRESS = 'unlogged_user@mdm-core.com'

    private UnloggedUser() {
    }

    String emailAddress = UNLOGGED_EMAIL_ADDRESS
    String firstName = 'Unlogged'
    String lastName = 'User'
    String tempPassword

    @Override
    UUID getId() {
        UUID.randomUUID()
    }

    static getInstance() {
        new UnloggedUser()
    }

    UUID ident() {
        id
    }

    @Override
    String getDomainType() {
        UnloggedUser
    }
}
