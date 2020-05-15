package uk.ac.ox.softeng.maurodatamapper.core.model

import uk.ac.ox.softeng.maurodatamapper.api.exception.ApiInternalException
import uk.ac.ox.softeng.maurodatamapper.core.diff.Diffable
import uk.ac.ox.softeng.maurodatamapper.core.facet.BreadcrumbTree
import uk.ac.ox.softeng.maurodatamapper.core.model.facet.Breadcrumb
import uk.ac.ox.softeng.maurodatamapper.core.traits.domain.PathAware

import grails.databinding.BindUsing
import grails.gorm.DetachedCriteria
import org.hibernate.search.annotations.Field
import org.springframework.core.Ordered

/**
 * Base class for all items which are contained inside a model. These items are securable by the model they are contained in.
 * D is always the class extending ModelItem, however due to compilation issues we have to use Diffable as the constraint
 * @since 04/11/2019
 */
trait ModelItem<D extends Diffable, T extends Model> extends CatalogueItem<D> implements PathAware, Ordered, Comparable<D> {

    abstract T getModel()

    abstract Boolean hasChildren()

    Integer idx

    int getOrder() {
        idx != null ? idx : Ordered.LOWEST_PRECEDENCE
    }

    void setIndex(int index) {
        idx = index
        markDirty('idx')
        if (ident()) updateIndices(index)
    }

    void updateIndices(int index) {
        // No-op
    }

    List<Breadcrumb> getBreadcrumbs() {
        breadcrumbTree.getBreadcrumbs()
    }

    @Override
    String buildPath() {
        String path = super.buildPath()
        if (!breadcrumbTree) {
            breadcrumbTree = new BreadcrumbTree(this)
        } else {
            if (!breadcrumbTree.matchesPath(path)) {
                breadcrumbTree.update(this)
            }
        }
        path
    }

    def beforeValidateModelItem() {
        if (idx == null) idx = Ordered.LOWEST_PRECEDENCE
        buildPath()
        beforeValidateCatalogueItem()
    }

    @Field
    String getModelType() {
        model.modelType
    }

    @Override
    int compareTo(D that) {
        if (!(that instanceof ModelItem)) throw new ApiInternalException('MI01', 'Cannot compare non-ModelItem')
        int res = this.order <=> ((ModelItem) that).order
        res == 0 ? this.label <=> ((ModelItem) that).label : res
    }
}
