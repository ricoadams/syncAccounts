package com.vbs

import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.DefaultHttpClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by radams on 9/9/2014.
 */
class SyncAccountRESTProxy {
    private static Logger LOGGER = LoggerFactory.getLogger(SyncAccountRESTProxy.class)

    private static final String VONAGE_BUSINESS_ADDRESS = "https://my.vocalocity.com/appserver/rest/zuoraSynchAccounts"

    private static void populateAuthorizationHeader(
            final String login, final String password, final HttpRequestBase request) {
        request.setHeader("login", login)
        request.setHeader("password", password)
    }

    public static HttpResponse callService(final String login, final String password, final Integer accountId) {
        HttpResponse response = null

        try {
            HttpGet request = new HttpGet(VONAGE_BUSINESS_ADDRESS + "?accountId=${accountId}")
            populateAuthorizationHeader(login, password, request)
            HttpClient client = new DefaultHttpClient()
            response = client.execute(request)
            client.connectionManager.shutdown()
        } catch (Throwable ex) {
            LOGGER.error("System unable call service", ex)
        }

        return response
    }
}
