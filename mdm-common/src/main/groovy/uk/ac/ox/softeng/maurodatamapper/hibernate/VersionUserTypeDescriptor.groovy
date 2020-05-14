package uk.ac.ox.softeng.maurodatamapper.hibernate

import uk.ac.ox.softeng.maurodatamapper.util.Version

import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor

/**
 * @since 25/01/2018
 */
class VersionUserTypeDescriptor extends AbstractTypeDescriptor<Version> {

    public static final VersionUserTypeDescriptor INSTANCE = new VersionUserTypeDescriptor()

    VersionUserTypeDescriptor() {
        super(Version)
    }

    @Override
    String toString(Version value) {
        value.toString()
    }

    @Override
    Version fromString(String string) {
        Version.from(string)
    }

    @Override
    <X> X unwrap(Version value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null
        }
        if (Version.class.isAssignableFrom(type)) {
            return (X) value
        }
        if (String.class.isAssignableFrom(type)) {
            return (X) toString(value)
        }
        throw unknownUnwrap(type)
    }

    @Override
    <X> Version wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null
        }
        if (String.class.isInstance(value)) {
            return fromString((String) value)
        }
        if (Version.class.isInstance(value)) {
            return (Version) value
        }
        throw unknownWrap(value.getClass())
    }
}
