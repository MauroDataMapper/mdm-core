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
