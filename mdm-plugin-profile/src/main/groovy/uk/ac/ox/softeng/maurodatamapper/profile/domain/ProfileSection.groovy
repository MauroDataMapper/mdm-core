package uk.ac.ox.softeng.maurodatamapper.profile.domain

import groovy.transform.AutoClone

@AutoClone
class ProfileSection {

    String sectionName
    String sectionDescription
    List<ProfileField> fields = []

}
