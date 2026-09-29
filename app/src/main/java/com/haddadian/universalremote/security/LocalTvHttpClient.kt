package com.haddadian.universalremote.security

import com.haddadian.universalremote.connection.TvTransportPolicy
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * HTTP is required by several legacy TV protocols. Cleartext is therefore
 * enabled at the Android transport layer, but this interceptor prevents HTTP
 * requests to anything except RFC1918 private IPv4 addresses.
 *
 * v29 fix: this used to re-implement its own private-IPv4 check, duplicating
 * the (previously unused) one in connection.TvTransportPolicy. Consolidated
 * into a single implementation so a future fix to the private-range logic
 * only has to happen once.
 */
class LocalTvHttpInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        require(TvTransportPolicy.isPrivateIpv4Host(request.url.host)) {
            "Local TV traffic is restricted to private IPv4 addresses"
        }
        return chain.proceed(request)
    }
}

object LocalTvHttpClient {
    fun create(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(LocalTvHttpInterceptor())
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()
}
