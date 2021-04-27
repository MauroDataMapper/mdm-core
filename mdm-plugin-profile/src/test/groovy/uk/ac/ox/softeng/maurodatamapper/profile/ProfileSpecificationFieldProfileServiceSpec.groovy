package uk.ac.ox.softeng.maurodatamapper.profile

import uk.ac.ox.softeng.maurodatamapper.core.facet.MetadataService
import uk.ac.ox.softeng.maurodatamapper.core.provider.MauroDataMapperServiceProviderService
import uk.ac.ox.softeng.maurodatamapper.profile.object.JsonProfile
import uk.ac.ox.softeng.maurodatamapper.profile.provider.EmptyJsonProfileFactory
import uk.ac.ox.softeng.maurodatamapper.test.unit.BaseUnitSpec

import grails.testing.services.ServiceUnitTest

class ProfileSpecificationFieldProfileServiceSpec extends BaseUnitSpec implements ServiceUnitTest<ProfileSpecificationFieldProfileService>{

    def setup() {
        mockArtefact(MetadataService)
        mockArtefact(MauroDataMapperServiceProviderService)
    }


    def cleanup() {
    }

    void "Profile correctly loaded"() {
        JsonProfile jsonProfile = EmptyJsonProfileFactory.instance.getEmptyProfile(service)
        expect: "2 fields in the profile specification profile"
            service.profileApplicableForDomains() == ["DataElement"]

            jsonProfile.sections.size() == 1
            jsonProfile.sections[0].fields.size() == 4
    }
}
