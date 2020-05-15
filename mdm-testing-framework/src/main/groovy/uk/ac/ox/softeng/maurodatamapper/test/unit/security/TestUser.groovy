package uk.ac.ox.softeng.maurodatamapper.test.unit.security

import uk.ac.ox.softeng.maurodatamapper.security.User

/**
 * @since 10/12/2019
 */
class TestUser implements User {


    String emailAddress
    String firstName
    String lastName
    String tempPassword
    UUID id
    String organisation
    String jobTitle

    UUID ident() {
        id
    }

    @Override
    String getDomainType() {
        TestUser
    }
}