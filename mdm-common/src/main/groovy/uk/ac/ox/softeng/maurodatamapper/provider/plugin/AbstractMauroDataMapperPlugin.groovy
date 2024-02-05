/*
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
package uk.ac.ox.softeng.maurodatamapper.provider.plugin

/**
 * @since 17/08/2017
 */
abstract class AbstractMauroDataMapperPlugin implements MauroDataMapperPlugin, Comparable<MauroDataMapperPlugin> {

    @Override
    String getVersion() {
        getClass().getPackage().getSpecificationVersion() ?: 'SNAPSHOT'
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
