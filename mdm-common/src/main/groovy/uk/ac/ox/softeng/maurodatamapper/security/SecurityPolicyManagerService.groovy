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
package uk.ac.ox.softeng.maurodatamapper.security

interface SecurityPolicyManagerService {

    UserSecurityPolicyManager addSecurityForSecurableResource(SecurableResource securableResource, User creator, String resourceName)

    UserSecurityPolicyManager removeSecurityForSecurableResource(SecurableResource securableResource, User actor)

    void removeSecurityForSecurableResourceIds(String securableResourceDomainType, Collection<UUID> ids)

    UserSecurityPolicyManager updateSecurityForSecurableResource(SecurableResource securableResource, Set<String> changedProperties,
                                                                 User currentUser)

    UserSecurityPolicyManager retrieveUserSecurityPolicyManager(String userEmailAddress)

    UserSecurityPolicyManager reloadUserSecurityPolicyManager(String userEmailAddress)

    UserSecurityPolicyManager refreshAllUserSecurityPolicyManagersBySecurableResource(SecurableResource securableResource, User currentUser)

    void removeUserSecurityPolicyManager(String emailAddress)

}