# coinbase-java
A java wrapper around the coinbase REST APIs

### Dependencies:
- [google-gson](https://code.google.com/p/google-gson/)
- [apache httpclient-4.3.1](http://hc.apache.org/httpclient-3.x/)

### Supported APIs
- Authentication
- Get current user
- list addresses (page, limit, and query support)
- generate recieve address
- buy
- sell
- buttons (no support for suggested pricing)
- list transactions
- get transaction details
- send money
- request money
- resend money request
- cancel money request
- complete money request

### Usage:
All work is done via the `CoinbaseApi` class. You can construct an instance of this class one of two ways:
```java
//Constructs an instance of CoinbaseApi using OAuth
CoinbaseApi api = new CoinbaseApi(clientId, clientSecret, redirectUrl);

//Constructs an instance of CoinbaseApi using and api key
CoinbaseApi api = new CoinbaseApi(apiKey);
```

**OAuth:**
If you are using OAuth as your authentication mechanism, you will first need to authenticate. coinbase-java only supports coinbase's new three-legged OAuth and there are no plans to support their old, deprecated two-legged OAuth.

The first thing you need to do is generate an authentication URL. You can do this by calling:

```java
String authUrl = api.generateOAuthUrl();
//get the auth code using the authUrl
if (api.authenticate(code)) {
    //auth success
} else {
    //auth failed
}

```

For information on coinbase's three-legged auth, see [here](https://coinbase.com/docs/api/authentication).
