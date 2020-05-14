package uk.ac.ox.softeng.maurodatamapper.provider.plugin

/**
 * @since 17/08/2017
 */
abstract class AbstractMauroDataMapperPlugin implements MauroDataMapperPlugin, Comparable<MauroDataMapperPlugin> {

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'unknown'
    }

    @Override
    Closure doWithSpring() {
        null
    }

    @Override
    String toString() {
        "${name} : ${version}"
    }

    @Override
    int compareTo(MauroDataMapperPlugin that) {
        int res = this.order <=> that.order
        if (res == 0) {
            if ((this.name.startsWith('Plugin') && that.name.startsWith('Plugin')) ||
                (this.name.startsWith('DataLoader') && that.name.startsWith('DataLoader'))) res = this.name <=> that.name
            else if ((this.name.startsWith('Plugin') && that.name.startsWith('DataLoader')) ||
                     (that.name.startsWith('DataLoader') || that.name.startsWith('Plugin'))) res = -1
            else if (this.name.startsWith('DataLoader') && that.name.startsWith('Plugin')) res = 1
            else res = this.name <=> that.name
        }
        res
    }

    @Override
    int getOrder() {
        HIGHEST_PRECEDENCE
    }
}
