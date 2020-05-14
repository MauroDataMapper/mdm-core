package uk.ac.ox.softeng.maurodatamapper.security

/**
 * @since 08/10/2019
 */
trait User extends SecurableResource {

    abstract UUID getId()

    abstract String getEmailAddress()

    abstract void setEmailAddress(String emailAddress)

    abstract String getFirstName()

    abstract void setFirstName(String firstName)

    abstract String getLastName()

    abstract void setLastName(String lastName)

    abstract String getTempPassword()

    abstract void setTempPassword(String tempPassword)

    String toString() {
        getEmailAddress()
    }
}