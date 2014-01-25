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
import com.createtank.payments.coinbase.models.Address;
import com.createtank.payments.coinbase.models.Transaction;
import com.createtank.payments.coinbase.models.Transfer;
import com.createtank.payments.coinbase.models.User;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class CoinbaseApi {

    private static final String OAUTH_BASE_URL = "https://coinbase.com:443/oauth";

    private String clientId;
    private String clientSecret;
    private String redirectUrl;
    private String accessToken;
    private String refreshToken;
    private String apiKey;
    private boolean allowSecure;

    public CoinbaseApi(String clientId, String clientSecret, String redirectUrl) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUrl = redirectUrl;
        allowSecure = true;
    }

    public CoinbaseApi(String clientId, String clientSecret, String redirectUrl,
                       String accessToken, String refreshToken) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUrl = redirectUrl;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        allowSecure = true;
    }

    public CoinbaseApi(String apiKey) {
        this.apiKey = apiKey;
        allowSecure = true;
    }

    //region accessors
    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean allowSecure() {
        return allowSecure;
    }

    public void allowSecure(boolean allowSecure) {
        this.allowSecure = allowSecure;
    }
    //endregion

    //region auth

    /**
     * Generates a valid OAuth url for three-legged authentication
     * @return a valid OAuth url
     */
    public String generateOAuthUrl() {
        String baseUrl = OAUTH_BASE_URL + "/authorize";
        String encodedUrl;
        try {
            encodedUrl = URLEncoder.encode(redirectUrl, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return baseUrl + "?response_type=code&client_id=" + clientId + "&redirect_uri=" + encodedUrl;
    }

    /**
     * Authenticates the client useing OAuth 2.0. This method does not work with api_key auth. After a successful auth
     * attempt, accessToken and refreshToken will be set. When using OAuth, this method must be called before any api
     * calls are made
     * @param code The code to use for the last leg of three-legged auth
     * @return whether or not authentication was successful
     * @throws IOException
     */
    public boolean authenticate(String code) throws IOException {
        RequestClient.disableCertificateValidation();
        if (allowSecure) {
            System.out.println("Using default client");
            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("grant_type", "authorization_code"));
            params.add(new BasicNameValuePair("redirect_uri", redirectUrl));
            params.add(new BasicNameValuePair("code", code));

            return doTokenRequest(params);
        } else {
            Map<String, String> params = new HashMap<String, String>();
            params.put("grant_type", "authorization_code");
            params.put("redirect_uri", URLEncoder.encode(redirectUrl, "UTF-8"));
            params.put("code", code);

            return doTokenRequest(params);
        }
    }

    private boolean doTokenRequest(Collection<BasicNameValuePair> params) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(OAUTH_BASE_URL + "/token");
        List<BasicNameValuePair> paramBody = new ArrayList<BasicNameValuePair>();
        paramBody.add(new BasicNameValuePair("client_id", clientId));
        paramBody.add(new BasicNameValuePair("client_secret", clientSecret));
        paramBody.addAll(params);
        post.setEntity(new UrlEncodedFormEntity(paramBody, "UTF-8"));
        HttpResponse response = client.execute(post);
        int code = response.getStatusLine().getStatusCode();

        if (code == 401) {
            return false;
        } else if (code != 200) {
            throw new IOException("Got HTTP response code " + code);
        }
        String responseString = EntityUtils.toString(response.getEntity());

        JsonParser parser = new JsonParser();
        JsonObject content = (JsonObject) parser.parse(responseString);
        System.out.print("content: " + content.toString());
        accessToken = content.get("access_token").getAsString();
        refreshToken = content.get("refresh_token").getAsString();
        return true;
    }

    private boolean doTokenRequest(Map<String, String> params)
            throws IOException {
        Map<String, String> paramsBody = new HashMap<String, String>();
        paramsBody.put("client_id", clientId);
        paramsBody.put("client_secret", clientSecret);
        paramsBody.putAll(params);

        String bodyStr = RequestClient.createRequestParams(paramsBody);
        System.out.println(bodyStr);
        HttpURLConnection conn = (HttpsURLConnection) new URL(OAUTH_BASE_URL + "/token").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write(bodyStr);
        writer.flush();
        writer.close();

        int code = conn.getResponseCode();

        if (code == 401) {
            return false;
        } else if (code != 200) {
            throw new IOException("Got HTTP response code " + code);
        }
        String response = RequestClient.getResponseBody(conn.getInputStream());

        JsonParser parser = new JsonParser();
        JsonObject content = (JsonObject) parser.parse(response);
        System.out.print("content: " + content.toString());
        accessToken = content.get("access_token").getAsString();
        refreshToken = content.get("refresh_token").getAsString();
        return true;
    }

    /**
     * refreshes the client's access token. This method should only be used when api_key auth is not used. After a
     * successful refresh attempt, the accessToken and refreshToken fields will be updated to contain the new values
     * @return whether or not the refresh was successful.
     * @throws IOException
     */
    public boolean refreshAccessToken() throws IOException  {

        if (allowSecure) {
            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("grant_type", "refresh_token"));
            params.add(new BasicNameValuePair("refresh_token", refreshToken));

            return doTokenRequest(params);
        } else {
            Map<String, String> params = new HashMap<String, String>();
            params.put("grant_type", "refresh_token");
            params.put("refresh_token", refreshToken);

            return doTokenRequest(params);
        }
    }
    //endregion

    //region Users
    /**
     * Get current user
     * @return the current user
     * @throws IOException
     */
    public User me() throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (apiKey != null)
            params.put("api_key", apiKey);
        JsonObject response = RequestClient.get(this, "users", params, accessToken);
        return User.fromJson(response.get("users").getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("user"));
    }
    //endregion

    //region Addresses

    /**
     * List bitcoin addresses associated with the current account
     * @param page the page of addresses to retrieve. Can be used to page through results
     * @param limit the maximum number of addresses to retrieve. Can't exceed 1000
     * @param query string match to filter addresses. Matches the address itself and also
     * if the use has set a ‘label’ on the address.
     * @return the list of bitcoin addresses associated with the current account
     * @throws IOException
     */
    public Address[] getAddresses(int page, int limit, String query) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (apiKey != null)
            params.put("api_key", apiKey);
        params.put("page", Integer.toString(page));
        params.put("limit", Integer.toString(limit));
        if (query != null)
            params.put("query", query);

        JsonObject response = RequestClient.get(this, "addresses", params, accessToken);
        JsonArray addresses = response.getAsJsonArray("addresses");
        int size = addresses.size();
        Address[] result = new Address[size];
        for (int i = 0; i < size; ++i) {
            JsonObject addy = addresses.get(i).getAsJsonObject().getAsJsonObject("address");
            result[i] = Address.fromJson(addy);
        }

        return result;
    }

    /**
     * List bitcoin addresses associated with the current account
     * @param page the page of addresses to retrieve. Can be used to page through results
     * @param limit the maximum number of addresses to retrieve. Can't exceed 1000
     * @return the list of bitcoin addresses associated with the current account
     * @throws IOException
     */
    public Address[] getAddresses(int page, int limit) throws IOException {
        return getAddresses(page, limit, null);
    }

    /**
     * List bitcoin addresses associated with the current account
     * @param page the page of addresses to retrieve. Can be used to page through results
     * @return the list of bitcoin addresses associated with the current account
     * @throws IOException
     */
    public Address[] getAddresses(int page) throws IOException {
        return getAddresses(page, 1000, null);
    }

    /**
     * List bitcoin addresses associated with the current account
     * @param query string match to filter addresses. Matches the address itself and also
     * @return the list of bitcoin addresses associated with the current account
     * @throws IOException
     */
    public Address[] getAddresses(String query) throws IOException {
        return getAddresses(1, 1000, query);
    }
    //endregion

    //region Account
    /**
     * Generates a new bitcoin receive address for the user.
     * @return the newly generated bitcoind address
     * @throws IOException
     */
    public Address generateReceiveAddress() throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (apiKey != null)
            params.put("api_key", apiKey);

        JsonObject response = RequestClient.post(this, "account/generate_receive_address", params, accessToken);
        boolean success = response.get("success").getAsBoolean();
        System.out.println(response.toString());
        return success ? Address.fromJson(response) : null;
    }
    //endregion

    //region Buys

    /**
     * Purchase bitcoin by debiting the user's U.S. bank account.
     * @param qty the number of bitcoins to buy
     * @return A Transfer object containing information about the purchase
     * @throws IOException
     */
    public Transfer buyBitcoins(float qty) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (apiKey != null)
            params.put("api_key", apiKey);

        params.put("qty", Float.toString(qty));
        params.put("agree_btc_amount_varies", Boolean.toString(true));

        JsonObject response = RequestClient.post(this, "buys", params, accessToken);
        boolean success = response.get("success").getAsBoolean();

        return success ? Transfer.fromJson(response.getAsJsonObject("transfer")) : null;
    }
    //endregion

    //region Sells
    /**
     * Sell bitcoin and receive a credit to the user's U.S. bank account.
     * @param qty the number of bitcoins to sell
     * @return A Transfer object containing information about the sale.
     * @throws IOException
     */
    public Transfer sellBitcoin(float qty) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (apiKey != null)
            params.put("api_key", apiKey);
        params.put("qty", Float.toString(qty));
        params.put("agree_btc_amount_varies", Boolean.toString(true));

        JsonObject response = RequestClient.post(this, "sells", params, accessToken);
        boolean success = response.get("success").getAsBoolean();

        return success ? Transfer.fromJson(response.getAsJsonObject("transfer")) : null;
    }
    //endregion

    //region Buttons
    private JsonObject createButtonRequestJson(String name, String type, String amount, String currency,
                                                     String style, String text, String desc, String custom,
                                                     String callbackUrl, String successUrl, String cancelUrl,
                                                     String infoUrl, boolean isVariablePrice,
                                                     boolean includeAddress, boolean includeEmail) {
        JsonObject json = new JsonObject();

        if (apiKey != null)
            json.addProperty("api_key", apiKey);

        JsonObject button = new JsonObject();
        button.addProperty("name", name);
        button.addProperty("price_string", amount);
        button.addProperty("price_currency_iso", currency);

        //optional params
        if (type != null)
            button.addProperty("type", type);

        if (custom != null)
            button.addProperty("custom", custom);

        if (callbackUrl != null)
            button.addProperty("callback_url", callbackUrl);

        if (desc != null)
            button.addProperty("description", desc);

        if (style != null)
            button.addProperty("style", style);

        if (text != null)
            button.addProperty("text", text);

        if (successUrl != null)
            button.addProperty("success_url", successUrl);

        if (cancelUrl != null)
            button.addProperty("cancel_url", cancelUrl);

        if (infoUrl != null)
            button.addProperty("info_url", infoUrl);

        button.addProperty("variable_price", isVariablePrice);
        button.addProperty("include_address", includeAddress);
        button.addProperty("include_email", includeEmail);
        json.add("button", button);

        return json;
    }

    /**
     * Creates a new payment button, page, or iframe
     * @param name The name of the item for which you are collecting bitcoin. For example, Acme Order #123 or Annual Pledge Drive
     * @param amount Price as a decimal string, for example 1.23. Can be more then two significant digits if currency equals BTC.
     * @param currency Price currency as an ISO 4217 code such as USD or BTC. This determines what currency the price is shown in on the payment widget.
     * @param type One of buy_now, donation, and subscription.
     * @param style One of buy_now_large, buy_now_small, donation_large, donation_small, subscription_large, subscription_small, custom_large, custom_small, and none. none is used if you plan on triggering the payment modal yourself using your own button or link.
     * @param text Allows you to customize the button text on custom_large and custom_small styles.
     * @param desc Longer description of the item in case you want it added to the user’s transaction notes.
     * @param custom An optional custom parameter. Usually an Order, User, or Product ID corresponding to a record in your database.
     * @param callbackUrl A custom callback URL specific to this button. It will receive the same information that would otherwise be sent to your Instant Payment Notification URL. If you have an Instant Payment Notification URL set on your account, this will be called instead — they will not both be called.
     * @param successUrl A custom success URL specific to this button. The user will be redirected to this URL after a successful payment. It will receive the same parameters that would otherwise be sent to the default success url set on the account.
     * @param cancelUrl A custom cancel URL specific to this button. The user will be redirected to this URL after a canceled order. It will receive the same parameters that would otherwise be sent to the default cancel url set on the account.
     * @param infoUrl A custom info URL specific to this button. Displayed to the user after a successful purchase for sharing. It will receive the same parameters that would otherwise be sent to the default info url set on the account.
     * @param isVariablePrice Allow users to change the price on the generated button.
     * @param includeAddress Collect shipping address from customer (not for use with inline iframes).
     * @param includeEmail Collect email address from customer (not for use with inline iframes).
     * @return Json response containing the button information
     * @throws IOException
     */
    public JsonObject makeButton(String name, String amount, String currency, String type, String style, String text,
                                 String desc, String custom, String callbackUrl, String successUrl, String cancelUrl,
                                 String infoUrl, boolean isVariablePrice,
                                 boolean includeAddress, boolean includeEmail)
            throws IOException, UnsupportedRequestVerbException {

        JsonObject jsonRequest = createButtonRequestJson(name, type, amount, currency, style, text, desc, custom, callbackUrl,
                successUrl, cancelUrl, infoUrl, isVariablePrice, includeAddress, includeEmail);

        JsonObject resp = RequestClient.post(this, "buttons", jsonRequest, accessToken);

        return resp != null && resp.has("button") ? resp.getAsJsonObject("button") : null;
    }

    /**
     * Creates a new payment button, page, or iframe
     * @param name The name of the item for which you are collecting bitcoin. For example, Acme Order #123 or Annual Pledge Drive
     * @param amount Price as a decimal string, for example 1.23. Can be more then two significant digits if currency equals BTC.
     * @param currency Price currency as an ISO 4217 code such as USD or BTC. This determines what currency the price is shown in on the payment widget.
     * @param type One of buy_now, donation, and subscription.
     * @param style One of buy_now_large, buy_now_small, donation_large, donation_small, subscription_large, subscription_small, custom_large, custom_small, and none. none is used if you plan on triggering the payment modal yourself using your own button or link.
     * @param text Allows you to customize the button text on custom_large and custom_small styles.
     * @param desc Longer description of the item in case you want it added to the user’s transaction notes.
     * @param custom An optional custom parameter. Usually an Order, User, or Product ID corresponding to a record in your database.
     * @param callbackUrl A custom callback URL specific to this button. It will receive the same information that would otherwise be sent to your Instant Payment Notification URL. If you have an Instant Payment Notification URL set on your account, this will be called instead — they will not both be called.
     * @param successUrl A custom success URL specific to this button. The user will be redirected to this URL after a successful payment. It will receive the same parameters that would otherwise be sent to the default success url set on the account.
     * @param cancelUrl A custom cancel URL specific to this button. The user will be redirected to this URL after a canceled order. It will receive the same parameters that would otherwise be sent to the default cancel url set on the account.
     * @param infoUrl A custom info URL specific to this button. Displayed to the user after a successful purchase for sharing. It will receive the same parameters that would otherwise be sent to the default info url set on the account.
     * @return Json response containing the button information
     * @throws IOException
     */
    public JsonObject makeButton(String name, String amount, String currency, String type, String style, String text,
                                 String desc, String custom, String callbackUrl, String successUrl, String cancelUrl,
                                 String infoUrl) throws IOException, UnsupportedRequestVerbException {
        return makeButton(name, amount, currency, type, style, text, desc, custom, callbackUrl, successUrl, cancelUrl,
                infoUrl, false, false, false);
    }

    /**
     * Creates a new payment button, page, or iframe
     * @param name The name of the item for which you are collecting bitcoin. For example, Acme Order #123 or Annual Pledge Drive
     * @param amount Price as a decimal string, for example 1.23. Can be more then two significant digits if currency equals BTC.
     * @param currency Price currency as an ISO 4217 code such as USD or BTC. This determines what currency the price is shown in on the payment widget.
     * @return Json response containing the button information
     * @throws IOException
     */
    public JsonObject makeButton(String name, String amount, String currency)
            throws IOException, UnsupportedRequestVerbException {
        return makeButton(name, amount, currency, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Creates a new payment button, page, or iframe
     * @param name The name of the item for which you are collecting bitcoin. For example, Acme Order #123 or Annual Pledge Drive
     * @param amount Price as a decimal string, for example 1.23. Can be more then two significant digits if currency equals BTC.
     * @param currency Price currency as an ISO 4217 code such as USD or BTC. This determines what currency the price is shown in on the payment widget.
     * @param type One of buy_now, donation, and subscription.
     * @param custom An optional custom parameter. Usually an Order, User, or Product ID corresponding to a record in your database.
     * @return Json response containing the button information
     * @throws IOException
     */
    public JsonObject makeButton(String name, String amount, String currency, String type, String custom)
            throws IOException, UnsupportedRequestVerbException {
        return makeButton(name, amount, currency, type, null, null, null, custom, null, null, null, null,
                false, false, false);
    }
    //endregion

    //region Transactions

    /**
     * List the current user's recent transactions.
     * @param page Used to paginate through results. Thirty transactions are returned per page.
     * @return an array of Transaction objects
     * @throws IOException
     */
    public Transaction[] getTransactions(int page) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (apiKey != null)
            params.put("api_key", apiKey);

        params.put("page", Integer.toString(page));
        JsonObject response = RequestClient.get(this, "transactions", params, accessToken);
        JsonArray transactionsJson = response.getAsJsonArray("transactions");
        Transaction[] transactions = new Transaction[transactionsJson.size()];
        for (int i = 0; i < transactionsJson.size(); ++i) {
            JsonObject transactionJson = transactionsJson.get(i).getAsJsonObject().getAsJsonObject("transaction");
            transactions[i] = Transaction.fromJson(transactionJson);
        }

        return transactions;
    }

    /**
     * Convenience method for retrieving the first page of the current user's recent transactions
     * @return an array of Transaction objects
     * @throws IOException
     */
    public Transaction[] getTransactions() throws IOException {
        return getTransactions(1);
    }

    /**
     * Get details for an individual transaction.
     * @param transactionId the id of the transaction to retrieve
     * @return A Transaction object containing details for the transaction
     * @throws IOException
     */
    public Transaction getTransaction(String transactionId) throws IOException {
        JsonObject response = RequestClient.get(this, "transactions/" + transactionId, accessToken);
        return Transaction.fromJson(response.getAsJsonObject("transaction"));
    }

    private Transaction sendMoney(String to, String amount, String amountString, String currency, String notes,
                                  String fee, String refererId) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (apiKey != null)
            params.put("api_key", apiKey);

        params.put("transaction[to]", to);
        if (amount != null)
            params.put("transaction[amount]", amount);

        if (amountString != null)
            params.put("transaction[amount_string]", amountString);

        if (currency != null)
            params.put("transaction[amount_currency_iso]", currency);

        if (notes != null)
            params.put("transaction[notes]", notes);

        if (fee != null)
            params.put("transaction[user_fee]", fee);

        if (refererId != null)
            params.put("transaction[referrer_id]", refererId);

        JsonObject response = RequestClient.post(this, "transactions/send_money", params, accessToken);

        if (!response.get("success").getAsBoolean())
            return null;

        return Transaction.fromJson(response.getAsJsonObject("transaction"));
    }

    /**
     * Send bitcoins to an email address or bitcoin address.
     * @param to An email address or a bitcoin address
     * @param amount A string amount that will be converted to BTC, such as ‘1’ or ‘1.234567’. Also must be >= ‘0.01’ or it will shown an error. If you want to use a different currency you can set amountString and currency_iso instead. This will automatically convert the amount to BTC and that converted amount will show in the transaction.
     * @param notes Included in the email that the recipient receives.
     * @return A Transaction object
     * @throws IOException
     */
    public Transaction sendMoney(String to, String amount, String notes) throws IOException {
        return sendMoney(to, amount, null, null, notes, null, null);
    }

    /**
     * Send bitcoins to an email address or bitcoin address.
     * @param to An email address or a bitcoin address
     * @param amountString A string amount that can be in any currency. If you use this with currency_iso you should leave amount blank.
     * @param currency A currency symbol such as USD, EUR, or ‘BTC’
     * @param notes Included in the email that the recipient receives.
     * @return A Transaction object
     * @throws IOException
     */
    public Transaction sendMoney(String to, String amountString, String currency, String notes) throws IOException {
        return sendMoney(to, null, amountString, currency, notes, null, null);
    }

    /**
     * Send bitcoins to an email address or bitcoin address.
     * @param to An email address or a bitcoin address
     * @param amount A string amount that will be converted to BTC, such as ‘1’ or ‘1.234567’. Also must be >= ‘0.01’ or it will shown an error. If you want to use a different currency you can set amountString and currency_iso instead. This will automatically convert the amount to BTC and that converted amount will show in the transaction.
     * @param notes Included in the email that the recipient receives.
     * @param fee transaction fee. Coinbase pays transaction fees on payments greater than or equal to 0.01 BTC. But for smaller amounts you may want to add your own amount. Fees can be added as a string, such as ‘0.0005’.
     * @param referrerId id of the user to get a referral credit in the case that this transaction makes the user eligible. The referring user is eligible for a credit if the address in the ‘to’ field is an email address for which there is currently no registered account and the recipient proceeds to buy or sell at least 1 BTC.
     * @return A Transaction Object
     * @throws IOException
     */
    public Transaction sendMoney(String to, String amount, String notes, String fee, String referrerId)
        throws IOException {

        return sendMoney(to, amount, null, null, notes, fee, referrerId);
    }

    /**
     * Send bitcoins to an email address or bitcoin address.
     * @param to An email address or a bitcoin address
     * @param amountString A string amount that can be in any currency. If you use this with currency_iso you should leave amount blank.
     * @param currency A currency symbol such as USD, EUR, or ‘BTC’
     * @param notes Included in the email that the recipient receives.
     * @param fee transaction fee. Coinbase pays transaction fees on payments greater than or equal to 0.01 BTC. But for smaller amounts you may want to add your own amount. Fees can be added as a string, such as ‘0.0005’.
     * @param referrerId id of the user to get a referral credit in the case that this transaction makes the user eligible. The referring user is eligible for a credit if the address in the ‘to’ field is an email address for which there is currently no registered account and the recipient proceeds to buy or sell at least 1 BTC.
     * @return A Transaction Object
     * @throws IOException
     */
    public Transaction sendMoney(String to, String amountString, String currency, String notes, String fee,
                                 String referrerId) throws IOException {
        return sendMoney(to, null, amountString, currency, notes, fee, referrerId);
    }

    private Transaction requestMoney(String from, String amount, String amountString, String currency, String notes)
        throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (apiKey != null)
            params.put("api_key", apiKey);

        params.put("transaction[from]", from);

        if (amount != null)
            params.put("transaction[amount]", amount);

        if (amountString != null)
            params.put("transaction[amount_string]", amountString);

        if (currency != null)
            params.put("transaction[amount_currency_iso]", currency);

        if (notes != null)
            params.put("transaction[notes]", notes);

        JsonObject response = RequestClient.post(this, "transactions/request_money", params, accessToken);
        if (!response.get("success").getAsBoolean())
            return null;

        return Transaction.fromJson(response.getAsJsonObject("transaction"));
    }

    /**
     * Send an invoice/money request to an email address.
     * @param from An email address to send the request
     * @param amount A string amount that will be converted to BTC, such as ‘1’ or ‘1.234567’. Also must be >= ‘0.01’ or it will shown an error. If you want to use a different currency you can set amountString and currency_iso instead. This will automatically convert the amount to BTC and that converted amount will show in the transaction.
     * @param notes Included in the email that the recipient receives.
     * @return A Transaction object
     * @throws IOException
     */
    public Transaction requestMoney(String from, String amount, String notes) throws IOException {
        return requestMoney(from, amount, null, null, notes);
    }

    /**
     * Send an invoice/money request to an email address.
     * @param from An email address to send the request
     * @param amountString A string amount that can be in any currency. If you use this with currency_iso you should leave amount blank.
     * @param currency A currency symbol such as USD, EUR, or ‘BTC’
     * @param notes Included in the email that the recipient receives.
     * @return A Transaction object
     * @throws IOException
     */
    public Transaction requestMoney(String from, String amountString, String currency, String notes)
            throws IOException {
        return requestMoney(from, null, amountString, currency, notes);
    }

    /**
     * Resend emails for a money request.
     * @param requestId The id of the request transaction to resend
     * @return whether or not the request succeeded
     * @throws IOException
     */
    public boolean resendMoneyRequest(String requestId) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (apiKey != null)
            params.put("api_key", apiKey);

        JsonObject response = RequestClient.put(this, "transactions/" + requestId + "/resend_request",
                params, accessToken);

        return response.get("success").getAsBoolean();
    }

    /**
     * Cancel a money request.
     * @param requestId The id of the request transaction to cancel
     * @return whether or not the cancellation was a success
     * @throws IOException
     */
    public boolean cancelMoneyRequest(String requestId) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (apiKey != null)
            params.put("api_key", apiKey);

        JsonObject response = RequestClient.delete(this, "transactions/" + requestId + "/cancel_request",
                params, accessToken);

        return response.get("success").getAsBoolean();
    }

    /**
     * Complete a money request.
     * @param requestId The id of the request transaction to complete
     * @return A Transaction object
     * @throws IOException
     */
    public Transaction completeMoneyRequest(String requestId) throws IOException {
        Map<String, String> params = new HashMap<String, String>();
        if (apiKey != null)
            params.put("api_key", apiKey);

        JsonObject response = RequestClient.put(this, "transactions/" + requestId + "/complete_request",
                params, accessToken);

        if (!response.get("success").getAsBoolean())
            return null;

        return Transaction.fromJson(response.getAsJsonObject("transaction"));
    }
    //endregion
}
