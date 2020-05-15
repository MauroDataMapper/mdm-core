package uk.ac.ox.softeng.maurodatamapper.core.file

import uk.ac.ox.softeng.maurodatamapper.core.traits.controller.MdmInterceptor
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.springframework.core.Ordered

class UserImageFileInterceptor implements MdmInterceptor {

    UserImageFileInterceptor() {
        order = Ordered.LOWEST_PRECEDENCE
    }

    @Override
    boolean before() {

        // Interception should be done by the user providing plugin
        if (params.userId) {
            return true
        }

        isShow()
    }
}
