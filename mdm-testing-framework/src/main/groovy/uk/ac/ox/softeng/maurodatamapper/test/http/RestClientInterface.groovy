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
package uk.ac.ox.softeng.maurodatamapper.test.http

import io.micronaut.core.type.Argument
import io.micronaut.http.HttpResponse
import io.micronaut.http.netty.cookies.NettyCookie

interface RestClientInterface {

    public static final Argument<Map> MAP_ARG = Argument.of(Map)
    public static final Argument<String> STRING_ARG = Argument.of(String)

    HttpResponse<Map> getResponse()

    NettyCookie getCurrentCookie()

    void setCurrentCookie(NettyCookie currentCookie)

    def <O> HttpResponse<O> GET(String resourceEndpoint)

    def <O> HttpResponse<O> POST(String resourceEndpoint, Map body)

    def <O> HttpResponse<O> PUT(String resourceEndpoint, Map body)

    def <O> HttpResponse<O> DELETE(String resourceEndpoint)

    def <O> HttpResponse<O> DELETE(String resourceEndpoint, Map body)

    def <O> HttpResponse<O> GET(String resourceEndpoint, Argument<O> bodyType)

    def <O> HttpResponse<O> POST(String resourceEndpoint, Map body, Argument<O> bodyType)

    def <O> HttpResponse<O> PUT(String resourceEndpoint, Map body, Argument<O> bodyType)

    def <O> HttpResponse<O> DELETE(String resourceEndpoint, Argument<O> bodyType)

    def <O> HttpResponse<O> GET(String resourceEndpoint, Argument<O> bodyType, boolean cleanEndpoint)

    def <O> HttpResponse<O> POST(String resourceEndpoint, Map body, Argument<O> bodyType, boolean cleanEndpoint)

    def <O> HttpResponse<O> PUT(String resourceEndpoint, Map body, Argument<O> bodyType, boolean cleanEndpoint)

    def <O> HttpResponse<O> DELETE(String resourceEndpoint, Argument<O> bodyType, boolean cleanEndpoint)

    def <O> HttpResponse<O> DELETE(String resourceEndpoint, Map body, Argument<O> bodyType)
}
