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
package uk.ac.ox.softeng.maurodatamapper.search.bridge.spi


import uk.ac.ox.softeng.maurodatamapper.search.bridge.OffsetDateTimeBridge
import uk.ac.ox.softeng.maurodatamapper.util.Utils

import org.hibernate.search.bridge.FieldBridge
import org.hibernate.search.bridge.spi.BridgeProvider
import org.hibernate.search.bridge.util.impl.String2FieldBridgeAdaptor

import java.time.OffsetDateTime

/**
 * @since 13/09/2021
 */
class OffsetDateTimeBridgeProvider implements BridgeProvider {
    @Override
    FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext) {
        if (
        bridgeProviderContext.returnType.simpleName == OffsetDateTime.simpleName && Utils.parentClassIsAssignableFromChild(OffsetDateTime, bridgeProviderContext.returnType)) {
            return new String2FieldBridgeAdaptor(new OffsetDateTimeBridge())
        }
        null
    }
}
