/*
 * Copyright 2020 University of Oxford and Health and Social Care Information Centre, also known as NHS Digital
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
package uk.ac.ox.softeng.maurodatamapper.traits.domain

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import grails.compiler.GrailsCompileStatic
import groovy.transform.SelfType
import org.grails.orm.hibernate.proxy.HibernateProxyHandler

/**
 * Technically this need to be typed, to prevent grails from messing up mapping on getPathParent,
 * however that adds an extra level of possibly unnecessary complexity, when we can just make sure getPathParent is
 * typed to the actual class rather than PathAware
 * @since 18/09/2017
 */
@SuppressFBWarnings('BC_IMPOSSIBLE_INSTANCEOF')
@SelfType(MdmDomain)
@GrailsCompileStatic
trait PathAware {

    public static final String UNSET = 'UNSET'

    String path
    //    Integer depth

    private HibernateProxyHandler proxyHandler = new HibernateProxyHandler()

    abstract MdmDomain getPathParent()

    String buildPath() {
        MdmDomain ge = getPathParent()
        if (ge) {
            if (ge.instanceOf(PathAware)) {
                // Ensure proxies are unwrapped
                PathAware parent =proxyHandler.unwrapIfProxy(ge) as PathAware
                //                depth = parent.depth + 1
                path = "${parent.getPath()}/${parent.getId() ?: UNSET}"
            } else {
                //                depth = 1
                path = "/${ge.ident()?.toString() ?: UNSET}"
            }
        } else {
            //            depth = 0
            path = ''
        }
        path
    }

    String getPath() {
        if (!path || path.contains(UNSET)) buildPath()
        path
    }

    //    Integer getDepth() {
    //        if (!depth) buildPath()
    //        depth
    //    }

    // This does NOT cascade to associations, so this must be implemented in each class if they contain any pathaware associations
    abstract def beforeValidate()

    abstract def beforeInsert()

    abstract def beforeUpdate()
}