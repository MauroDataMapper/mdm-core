package uk.ac.ox.softeng.maurodatamapper.core.model


import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.EditHistoryAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.InformationAware
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.PathAware
import uk.ac.ox.softeng.maurodatamapper.security.SecurableResource

/**
 * This is the base trait for any container of models.
 * Currently {@link uk.ac.ox.softeng.maurodatamapper.core.container.Folder} are physical containers and
 * {@link uk.ac.ox.softeng.maurodatamapper.core.container.Classifier} are virtual containers.
 *
 * @since 05/11/2019
 */
trait Container implements PathAware, InformationAware, SecurableResource, EditHistoryAware {

    abstract boolean hasChildren()

    abstract Boolean getDeleted()

}
