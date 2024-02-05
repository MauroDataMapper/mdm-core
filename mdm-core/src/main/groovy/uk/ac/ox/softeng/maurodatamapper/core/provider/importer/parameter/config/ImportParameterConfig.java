/**
 * Copyright 2020-2024 University of Oxford and NHS England
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
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

    String[] description() default {};

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

    String descriptionJoinDelimiter() default "\n";

    /**
     * If set to true will hide this parameter from the list of possible parameters they are described
     *
     * @return boolean
     */
    boolean hidden() default false;
}