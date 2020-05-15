package uk.ac.ox.softeng.maurodatamapper.core.admin


import uk.ac.ox.softeng.maurodatamapper.test.unit.CreatorAwareSpec

import grails.testing.gorm.DomainUnitTest

class ApiPropertySpec extends CreatorAwareSpec<ApiProperty> implements DomainUnitTest<ApiProperty> {

    @Override
    void setValidDomainOtherValues() {
        domain.key = ApiPropertyEnum.EMAIL_FROM_NAME.key
        domain.value = 'Unit Test'
        domain.lastUpdatedBy = admin.emailAddress
        domain
    }

    @Override
    void verifyDomainOtherConstraints(ApiProperty domain) {
        assert domain.key == ApiPropertyEnum.EMAIL_FROM_NAME.key
        assert domain.value == 'Unit Test'
        assert domain.lastUpdatedBy == admin.emailAddress
    }
}
