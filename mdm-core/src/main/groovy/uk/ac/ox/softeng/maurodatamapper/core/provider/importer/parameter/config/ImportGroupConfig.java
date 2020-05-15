package uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config;

import io.micronaut.core.order.Ordered;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @since 11/07/2018
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ImportGroupConfig {

    String name() default "Miscellaneous";

    int order() default Ordered.LOWEST_PRECEDENCE;
}
