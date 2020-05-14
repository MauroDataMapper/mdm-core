package uk.ac.ox.softeng.maurodatamapper.hibernate

import uk.ac.ox.softeng.maurodatamapper.util.Version

import org.hibernate.dialect.Dialect
import org.hibernate.type.AbstractSingleColumnStandardBasicType
import org.hibernate.type.DiscriminatorType
import org.hibernate.type.descriptor.sql.VarcharTypeDescriptor

/**
 * @since 25/01/2018
 */
class VersionUserType extends AbstractSingleColumnStandardBasicType<Version> implements DiscriminatorType<Version> {

    public static final VersionUserType INSTANCE = new VersionUserType()

    VersionUserType() {
        super(VarcharTypeDescriptor.INSTANCE, VersionUserTypeDescriptor.INSTANCE)
    }

    @Override
    Version stringToObject(String xml) throws Exception {
        fromString(xml)
    }

    @Override
    String objectToSQLString(Version value, Dialect dialect) throws Exception {
        toString(value)
    }

    @Override
    String getName() {
        'version'
    }
}
