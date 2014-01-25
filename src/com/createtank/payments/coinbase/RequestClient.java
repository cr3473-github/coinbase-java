/*
Copyright 2013 createTank L.L.C.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package com.createtank.payments.coinbase;

import com.createtank.payments.coinbase.exceptions.UnsupportedRequestVerbException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Singleton for managing web service calls
 * Based HEAVILY on the com.coinbase.api.RpcManager class from the coinbase-android project
 */
public class RequestClient {

    private static final String BASE_URL = "https://coinbase.com:443/api/v1/";

    private static enum RequestVerb {
        GET,
        POST,
        PUT,
        DELETE
    }

    private static JsonObject call(CoinbaseApi api, String method, RequestVerb verb, Map<String,
            String> params, String accessToken) throws IOException {
        return call(api, method, verb, params, true, accessToken);
    }

    private static JsonObject call(CoinbaseApi api, String method, RequestVerb verb, JsonObject json,
                                   String accessToken) throws IOException, UnsupportedRequestVerbException {
        return call(api, method, verb, json,true, accessToken);
    }

    private static JsonObject call(CoinbaseApi api, String method, RequestVerb verb, JsonObject json,
                                   boolean retry, String accessToken) throws IOException, UnsupportedRequestVerbException {

        if (verb == RequestVerb.DELETE || verb == RequestVerb.GET) {
            throw new UnsupportedRequestVerbException();
        }
        if (api.allowSecure()) {

            HttpClient client = HttpClientBuilder.create().build();
            String url = BASE_URL + method;
            HttpUriRequest request;

            switch (verb) {
                case POST:
                    request = new HttpPost(url);
                    break;
                case PUT:
                    request = new HttpPut(url);
                    break;
                default:
                    throw new RuntimeException("RequestVerb not implemented: " + verb);
            }

            ((HttpEntityEnclosingRequestBase) request).setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

            if (accessToken != null)
                request.addHeader("Authorization", String.format("Bearer %s", accessToken));

            request.addHeader("Content-Type", "application/json");
            HttpResponse response = client.execute(request);
            int code = response.getStatusLine().getStatusCode();

            if (code == 401) {
                if (retry) {
                    api.refreshAccessToken();
                    call(api, method, verb, json, false, api.getAccessToken());
                } else {
                    throw new IOException("Account is no longer valid");
                }
            } else if (code != 200) {
                throw new IOException("HTTP response " + code + " to request " + method);
            }

            String responseString = EntityUtils.toString(response.getEntity());
            if(responseString.startsWith("[")) {
                // Is an array
                responseString = "{response:" + responseString + "}";
            }

            JsonParser parser = new JsonParser();
            JsonObject resp = (JsonObject) parser.parse(responseString);
            System.out.println(resp.toString());
            return resp;
        }

        String url = BASE_URL + method;

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(verb.name());
        conn.addRequestProperty("Content-Type", "application/json");

        if (accessToken != null)
            conn.setRequestProperty("Authorization", String.format("Bearer %s", accessToken));


        conn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write(json.toString());
        writer.flush();
        writer.close();

        int code = conn.getResponseCode();
        if (code == 401) {

            if (retry) {
                api.refreshAccessToken();
                return call(api, method, verb, json, false, api.getAccessToken());
            } else {
                throw new IOException("Account is no longer valid");
            }

        } else if (code != 200) {
            throw new IOException("HTTP response " + code + " to request " + method);
        }

        String responseString = getResponseBody(conn.getInputStream());
        if (responseString.startsWith("[")) {
            responseString = "{response:" + responseString + "}";
        }

        JsonParser parser = new JsonParser();
        return (JsonObject) parser.parse(responseString);
    }

    private static JsonObject call(CoinbaseApi api, String method, RequestVerb verb, Map<String, String> params,
                            boolean retry, String accessToken) throws IOException {
        if (api.allowSecure()) {

            HttpClient client = HttpClientBuilder.create().build();
            String url = BASE_URL + method;
            HttpUriRequest request = null;

            if (verb == RequestVerb.POST || verb == RequestVerb.PUT) {
                switch (verb) {
                    case POST:
                        request = new HttpPost(url);
                        break;
                    case PUT:
                        request = new HttpPut(url);
                        break;
                    default:
                        throw new RuntimeException("RequestVerb not implemented: " + verb);
                }

                List<BasicNameValuePair> paramsBody = new ArrayList<BasicNameValuePair>();

                if (params != null) {
                    List<BasicNameValuePair> convertedParams = convertParams(params);
                    paramsBody.addAll(convertedParams);
                }

                ((HttpEntityEnclosingRequestBase) request).setEntity(new UrlEncodedFormEntity(paramsBody, "UTF-8"));
            } else {
                if (params != null) {
                    url = url + "?" + createRequestParams(params);
                }

                if (verb == RequestVerb.GET) {
                    request = new HttpGet(url);
                } else if (verb == RequestVerb.DELETE) {
                    request = new HttpDelete(url);
                }
            }
            if (request == null)
                return null;

            if (accessToken != null)
                request.addHeader("Authorization", String.format("Bearer %s", accessToken));
            System.out.println("auth header: " + request.getFirstHeader("Authorization"));
            HttpResponse response = client.execute(request);
            int code = response.getStatusLine().getStatusCode();

            if (code == 401) {
                if (retry) {
                    api.refreshAccessToken();
                    call(api, method, verb, params, false, api.getAccessToken());
                } else {
                    throw new IOException("Account is no longer valid");
                }
            } else if (code != 200) {
                throw new IOException("HTTP response " + code + " to request " + method);
            }

            String responseString = EntityUtils.toString(response.getEntity());
            if(responseString.startsWith("[")) {
                // Is an array
                responseString = "{response:" + responseString + "}";
            }

            JsonParser parser = new JsonParser();
            JsonObject resp = (JsonObject) parser.parse(responseString);
            System.out.println(resp.toString());
            return resp;
        }

        String paramStr = createRequestParams(params);
        String url = BASE_URL + method;

        if (paramStr != null && verb == RequestVerb.GET || verb == RequestVerb.DELETE)
            url += "?" + paramStr;

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(verb.name());

        if (accessToken != null)
            conn.setRequestProperty("Authorization", String.format("Bearer %s", accessToken));

        if (verb != RequestVerb.GET && verb != RequestVerb.DELETE && paramStr != null) {
            conn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(paramStr);
            writer.flush();
            writer.close();
        }

        int code = conn.getResponseCode();
        if (code == 401) {

            if (retry) {
                api.refreshAccessToken();
                return call(api, method, verb, params, false, api.getAccessToken());
            } else {
                throw new IOException("Account is no longer valid");
            }

        } else if (code != 200) {
            throw new IOException("HTTP response " + code + " to request " + method);
        }

        String responseString = getResponseBody(conn.getInputStream());
        if (responseString.startsWith("[")) {
            responseString = "{response:" + responseString + "}";
        }

        JsonParser parser = new JsonParser();
        return (JsonObject) parser.parse(responseString);
    }

    public static String createRequestParams(Map<String, String> params) {
        if (params == null)
            return null;

        Set<Map.Entry<String, String>> entries = params.entrySet();
        int size = entries.size();
        int count = 0;
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : entries) {
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(entry.getValue());

            if (count < size - 1) {
                sb.append("&");
            }
            count++;
        }

        return sb.toString();
    }

    public static JsonObject get(CoinbaseApi api, String method, String accessToken) throws IOException {
        return call(api, method, RequestVerb.GET, (Map<String, String>) null, accessToken);
    }

    public static JsonObject get(CoinbaseApi api, String method, Map<String, String> params, String accessToken)
            throws IOException {
        return call(api, method, RequestVerb.GET, params, accessToken);
    }

    public static JsonObject post(CoinbaseApi api, String method, Map<String, String> params, String accessToken)
            throws IOException {
        return call(api, method, RequestVerb.POST, params, accessToken);
    }

    public static JsonObject post(CoinbaseApi api, String method, JsonObject json, String accessToken)
            throws IOException, UnsupportedRequestVerbException {
        return call(api, method, RequestVerb.POST, json, accessToken);
    }

    public static JsonObject put(CoinbaseApi api, String method, Map<String, String> params, String accessToken)
            throws IOException {
        return call(api, method, RequestVerb.PUT, params, accessToken);
    }

    public static JsonObject put(CoinbaseApi api, String method, JsonObject json, String accessToken)
            throws IOException, UnsupportedRequestVerbException {
        return call(api, method, RequestVerb.PUT, json, accessToken);
    }

    public static JsonObject delete(CoinbaseApi api, String method, Map<String, String> params, String accessToken) throws IOException {
        return call(api, method, RequestVerb.DELETE, params, accessToken);
    }

    public static String getResponseBody(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String tmp;
        StringBuilder sb = new StringBuilder();
        while ((tmp = br.readLine()) != null) {
            sb.append(tmp);
        }

        return sb.toString();
    }

    public static List<BasicNameValuePair> convertParams(Map<String, String> params) {
        Set<Map.Entry<String, String>> entries = params.entrySet();
        List<BasicNameValuePair> pairs = new ArrayList<BasicNameValuePair>();
        for (Map.Entry<String, String> entry : entries) {
            pairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }

        return pairs;
    }

    public static void disableCertificateValidation() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }};

        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { return true; }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {
            //Ignore
        }
    }
}
