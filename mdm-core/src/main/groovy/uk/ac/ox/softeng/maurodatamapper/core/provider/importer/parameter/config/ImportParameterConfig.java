package uk.ac.ox.softeng.maurodatamapper.core.provider.importer.parameter.config;

import io.micronaut.core.order.Ordered;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ImportParameterConfig {

    String description() default "";

    String displayName() default "";

    ImportGroupConfig group() default @ImportGroupConfig;

    boolean optional() default false;

    /**
     * Smaller number = higher in order in page
     *
     * @return int position on page
     */
    int order() default Ordered.LOWEST_PRECEDENCE;

    boolean password() default false;
}