package uk.ac.ox.softeng.maurodatamapper.core.traits.controller

/**
 * @since 21/10/2019
 */
trait ResourcelessMdmController extends MdmController {

    Class getResource() {
        null
    }
}
