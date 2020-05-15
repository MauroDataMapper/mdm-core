package uk.ac.ox.softeng.maurodatamapper.core.provider.email

import uk.ac.ox.softeng.maurodatamapper.core.provider.ProviderType
import uk.ac.ox.softeng.maurodatamapper.provider.MauroDataMapperService

trait EmailProviderService extends MauroDataMapperService {

    abstract boolean configure(Map props)

    abstract def sendEmail(String fromName,
                           String fromAddress,
                           Map<String, String> to,
                           Map<String, String> cc,
                           String subject,
                           String messageBody)

    @Override
    String getProviderType() {
        ProviderType.EMAIL.name
    }
}
