package com.createtank.payments.coinbase;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;

/**
 * Singleton for managing web
 */
public class RequestClient {

    private static final String BASE_URL = "https://coinbase.com:443/api/v1/";

    private static enum RequestVerb {
        GET,
        POST,
        PUT,
        DELETE
    }

    private static JsonObject call(CoinbaseApi api, String method, RequestVerb verb, Map<String, String> params, String accessToken)
            throws IOException {
        return call(api, method, verb, params, true, accessToken);
    }

    private static JsonObject call(CoinbaseApi api, String method, RequestVerb verb, Map<String, String> params,
                            boolean retry, String accessToken) throws IOException {
        String paramStr = createRequestParams(params);
        String url = BASE_URL + method;

        if (paramStr != null && verb == RequestVerb.GET || verb == RequestVerb.DELETE)
            url += "?" + paramStr;

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(verb.name());

        if (accessToken != null)
            conn.setRequestProperty("Authorization", String.format("Bearer %s", accessToken));

        if (verb != RequestVerb.GET && verb != RequestVerb.DELETE) {
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
        }

        return sb.toString();
    }

    public static JsonObject get(CoinbaseApi api, String method, String accessToken) throws IOException {
        return call(api, method, RequestVerb.GET, null, accessToken);
    }

    public static JsonObject get(CoinbaseApi api, String method, Map<String, String> params, String accessToken) throws IOException {
        return call(api, method, RequestVerb.GET, params, accessToken);
    }

    public static JsonObject post(CoinbaseApi api, String method, Map<String, String> params, String accessToken) throws IOException {
        return call(api, method, RequestVerb.POST, params, accessToken);
    }

    public static JsonObject put(CoinbaseApi api, String method, Map<String, String> params, String accessToken) throws IOException {
        return call(api, method, RequestVerb.PUT, params, accessToken);
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
}
