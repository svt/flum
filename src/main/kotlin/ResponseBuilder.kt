// SPDX-FileCopyrightText: 2022 Sveriges Television AB
//
// SPDX-License-Identifier: Apache-2.0

package se.svt.oss.flum

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.io.InputStream

class ResponseBuilder(private val requestVerifyer: RequestVerifyer) {

    private val responseModifiers = ArrayList<ResponseModifier>()

    internal var responseProducer: ResponseProducer = { MockResponse() }

    internal val actions = ArrayList<RequestAction>()

    @Synchronized
    fun withStatus(status: Int): ResponseBuilder =
        apply {
            this.responseModifiers.add {
                it.setResponseCode(status)
            }
        }

    @Synchronized
    fun withHeader(header: String, value: String): ResponseBuilder =
        apply {
            this.responseModifiers.add {
                it.setHeader(header, value)
            }
        }

    @Synchronized
    fun withBody(body: String): ResponseBuilder =
        apply {
            this.responseModifiers.add {
                it.setBody(body)
            }
        }

    @Synchronized
    fun withBody(body: InputStream): ResponseBuilder =
        apply {
            this.responseModifiers.add {
                it.setBody(Buffer().readFrom(body))
            }
        }

    @Synchronized
    fun withJsonBody(body: String): ResponseBuilder =
        apply {
            this.responseModifiers.add {
                it.setHeader(CONTENT_TYPE, APPLICATION_JSON)
                it.setBody(body)
            }
        }

    @Synchronized
    fun afterwardsVerifyRequest(): RequestVerifyer =
        requestVerifyer

    @Synchronized
    internal fun response(request: RecordedRequest): MockResponse {
        actions.forEach { it.invoke(request) }
        return responseModifiers.fold(
            responseProducer.invoke(request),
            { response, modifier -> modifier.invoke(response) }
        )
    }
}
