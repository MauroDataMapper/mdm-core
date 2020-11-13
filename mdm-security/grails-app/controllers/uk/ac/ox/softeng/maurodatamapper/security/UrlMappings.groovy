/*
 * Copyright 2020 University of Oxford
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


import static uk.ac.ox.softeng.maurodatamapper.core.web.mapping.UrlMappingActions.DEFAULT_EXCLUDES

import static org.grails.web.mapping.DefaultUrlMappingEvaluator.ACTION_DELETE
import static org.grails.web.mapping.DefaultUrlMappingEvaluator.ACTION_INDEX
import static org.grails.web.mapping.DefaultUrlMappingEvaluator.ACTION_SAVE

class UrlMappings {

    static mappings = {

        group '/api', {
            '/userGroups'(resources: 'userGroup', excludes: DEFAULT_EXCLUDES) {
                get '/catalogueUsers'(controller: 'catalogueUser', action: 'index')
                put "/catalogueUsers/$catalogueUserId"(controller: 'userGroup', action: 'alterMembers')
                delete "/catalogueUsers/$catalogueUserId"(controller: 'userGroup', action: 'alterMembers')

                "/securableResourceGroupRoles"(resources: 'securableResourceGroupRole', excludes: DEFAULT_EXCLUDES)
            }

            '/catalogueUsers'(resources: 'catalogueUser', excludes: DEFAULT_EXCLUDES) {
                '/image'(single: 'userImageFile', excludes: DEFAULT_EXCLUDES)
                get '/userPreferences'(controller: 'catalogueUser', action: 'userPreferences')
                put '/userPreferences'(controller: 'catalogueUser', action: 'updateUserPreferences')
                put '/changePassword'(controller: 'catalogueUser', action: 'changePassword')
                put '/resetPassword'(controller: 'catalogueUser', action: 'resetPassword') // New URL
                '/apiKeys'(resources: 'apiKey', includes: [ACTION_SAVE, ACTION_DELETE, ACTION_INDEX]) {
                    put "/refresh/$expiresInDays"(controller: 'apiKey', action: 'refreshApiKey')
                    put '/disable'(controller: 'apiKey', action: 'disableApiKey')
                    put '/enable'(controller: 'apiKey', action: 'enableApiKey')
                }
            }

            group '/catalogueUsers', {
                post '/search'(controller: 'catalogueUser', action: 'search') // Only available if logged in
                get '/search'(controller: 'catalogueUser', action: 'search') // Only available if logged in
                get "/resetPasswordLink/$emailAddress"(controller: 'catalogueUser', action: 'sendPasswordResetLink') // New URL
            }

            group '/admin', {
                group '/catalogueUsers', {
                    get '/pending'(controller: 'catalogueUser', action: 'pending') // New URL
                    get '/pendingCount'(controller: 'catalogueUser', action: 'pendingCount') // New URL
                    get "/userExists/$emailAddress"(controller: 'catalogueUser', action: 'userExists') // New URL, new JSON
                    post '/adminRegister'(controller: 'catalogueUser', action: 'adminRegister') // New URL

                    group "/$catalogueUserId", {
                        put '/adminPasswordReset'(controller: 'catalogueUser', action: 'adminPasswordReset') // New URL
                        put '/approveRegistration'(controller: 'catalogueUser', action: 'approveRegistration') // New URL
                        put '/rejectRegistration'(controller: 'catalogueUser', action: 'rejectRegistration') // New URL

                    }
                }
                post '/activeSessions'(controller: 'authenticating', action: 'activeSessionsWithCredentials')

                '/groupRoles'(resources: 'groupRole', excludes: DEFAULT_EXCLUDES)
                group '/applicationGroupRoles', {
                    get '/'(controller: 'groupRole', action: 'listApplicationGroupRoles')
                    group "/$applicationGroupRoleId", {
                        get '/userGroups'(controller: 'userGroup', action: 'index')
                        put "/userGroups/$userGroupId"(controller: 'userGroup', action: 'updateApplicationGroupRole')
                        delete "/userGroups/$userGroupId"(controller: 'userGroup', action: 'updateApplicationGroupRole')
                    }
                }
                get '/availableApplicationAccess'(controller: 'groupRole', action: 'listApplicationAccess')
            }
            group "/$securableResourceDomainType/$securableResourceId", {
                group '/groupRoles', {
                    get "/"(controller: "groupRole", action: "listGroupRolesAvailableToSecurableResource")

                    group "/$groupRoleId", {

                        get "/"(controller: 'securableResourceGroupRole', action: 'index')

                        get "/userGroups"(controller: 'userGroup', action: 'index')
                        post "/userGroups/$userGroupId"(controller: 'securableResourceGroupRole', action: 'save')
                        delete "/userGroups/$userGroupId"(controller: 'securableResourceGroupRole', action: 'delete')
                    }
                }
                '/securableResourceGroupRoles'(resources: 'securableResourceGroupRole', excludes: DEFAULT_EXCLUDES)
                get '/permissions'(controller: 'permissions', action: 'permissions')
            }
            group "/$containerDomainType/$containerId", {
                '/userGroups'(resources: 'userGroup', excludes: DEFAULT_EXCLUDES) {
                    get '/catalogueUsers'(controller: 'catalogueUser', action: 'index')
                    put "/catalogueUsers/$catalogueUserId"(controller: 'userGroup', action: 'alterMembers')
                    delete "/catalogueUsers/$catalogueUserId"(controller: 'userGroup', action: 'alterMembers')
                }
            }

            group '/authentication', {
                post '/login'(controller: 'authenticating', action: 'login')
                '/logout'(controller: 'authenticating', action: 'logout')
            }
        }
    }
}
