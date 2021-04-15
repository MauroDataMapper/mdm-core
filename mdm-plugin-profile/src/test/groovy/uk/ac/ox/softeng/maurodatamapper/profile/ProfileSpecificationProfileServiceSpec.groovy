package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile

import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class ProfileSpecificationProfileServiceSpec extends Specification implements ServiceUnitTest<ProfileSpecificationProfileService>{

    MetadataService metadataService

    def setup() {
    }

    def cleanup() {
    }

    void "Profile correctly loaded"() {
        JsonProfile jsonProfile = service.getNewProfile()
        expect: "2 fields in the profile specification profile"
            service.profileApplicableForDomains() == ["DataModel"]

            jsonProfile.sections.size() == 1
            jsonProfile.sections[0].fields.size() == 1
    }
}
