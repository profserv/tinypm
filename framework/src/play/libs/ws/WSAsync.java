package play.libs.ws;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import oauth.signpost.AbstractOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.http.HttpRequest;

import org.apache.commons.lang.NotImplementedException;

import play.Logger;
import play.Play;
import play.libs.F.Promise;
import play.libs.MimeTypes;
import play.libs.OAuth.ServiceInfo;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSImpl;
import play.libs.WS.WSRequest;
import play.mvc.Http.Header;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.AsyncHttpClientConfig.Builder;
import com.ning.http.client.ByteArrayPart;
import com.ning.http.client.FilePart;
import com.ning.http.client.Part;
import com.ning.http.client.PerRequestConfig;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Realm.AuthScheme;
import com.ning.http.client.Realm.RealmBuilder;
import com.ning.http.client.Response;

/**
 * Simple HTTP client to make webservices requests.
 * 
 * <p/>
 * Get latest BBC World news as a RSS content
 * <pre>
 *    HttpResponse response = WS.url("http://newsrss.bbc.co.uk/rss/newsonline_world_edition/front_page/rss.xml").get();
 *    Document xmldoc = response.getXml();
 *    // the real pain begins here...
 * </pre>
 * <p/>
 * 
 * Search what Yahoo! thinks of google (starting from the 30th result).
 * <pre>
 *    HttpResponse response = WS.url("http://search.yahoo.com/search?p=<em>%s</em>&pstart=1&b=<em>%s</em>", "Google killed me", "30").get();
 *    if( response.getStatus() == 200 ) {
 *       html = response.getString();
 *    }
 * </pre>
 */
public class WSAsync implements WSImpl {

    private AsyncHttpClient httpClient;

    public WSAsync() {
        String proxyHost = Play.configuration.getProperty("http.proxyHost", System.getProperty("http.proxyHost"));
        String proxyPort = Play.configuration.getProperty("http.proxyPort", System.getProperty("http.proxyPort"));
        String proxyUser = Play.configuration.getProperty("http.proxyUser", System.getProperty("http.proxyUser"));
        String proxyPassword = Play.configuration.getProperty("http.proxyPassword", System.getProperty("http.proxyPassword"));
        String userAgent = Play.configuration.getProperty("http.userAgent");

        Builder confBuilder = new AsyncHttpClientConfig.Builder();
        if (proxyHost != null) {
            int proxyPortInt = 0;
            try {
                proxyPortInt = Integer.parseInt(proxyPort);
            } catch (NumberFormatException e) {
                Logger.error("Cannot parse the proxy port property '%s'. Check property http.proxyPort either in System configuration or in Play config file.", proxyPort);
                throw new IllegalStateException("WS proxy is misconfigured -- check the logs for details");
            }
            ProxyServer proxy = new ProxyServer(proxyHost, proxyPortInt, proxyUser, proxyPassword);
            confBuilder.setProxyServer(proxy);
        }
        if (userAgent != null) {
            confBuilder.setUserAgent(userAgent);
        }
        // when using raw urls, AHC does not encode the params in url.
        // this means we can/must encode it(with correct encoding) before passing it to AHC
        confBuilder.setUseRawUrl(true);
        httpClient = new AsyncHttpClient(confBuilder.build());
    }

    public void stop() {
        Logger.trace("Releasing http client connections...");
        httpClient.close();
    }

    public WSRequest newRequest(String url, String encoding) {
        return new WSAsyncRequest(url, encoding);
    }

    public class WSAsyncRequest extends WSRequest {

        protected String type = null;

        protected WSAsyncRequest(String url, String encoding) {
            super(url, encoding);
        }

        /** Execute a GET request synchronously. */
        @Override
        public HttpResponse get() {
            this.type = "GET";
            sign();
            try {
                return new HttpAsyncResponse(prepare(httpClient.prepareGet(url)).execute().get());
            } catch (Exception e) {
                Logger.error(e.toString());
                throw new RuntimeException(e);
            }
        }

        /** Execute a GET request asynchronously. */
        @Override
        public Promise<HttpResponse> getAsync() {
            this.type = "GET";
            sign();
            return execute(httpClient.prepareGet(url));
        }

        /** Execute a POST request.*/
        @Override
        public HttpResponse post() {
            this.type = "POST";
            sign();
            try {
                return new HttpAsyncResponse(prepare(httpClient.preparePost(url)).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a POST request asynchronously.*/
        @Override
        public Promise<HttpResponse> postAsync() {
            this.type = "POST";
            sign();
            return execute(httpClient.preparePost(url));
        }

        /** Execute a PUT request.*/
        @Override
        public HttpResponse put() {
            this.type = "PUT";
            try {
                return new HttpAsyncResponse(prepare(httpClient.preparePut(url)).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a PUT request asynchronously.*/
        @Override
        public Promise<HttpResponse> putAsync() {
            this.type = "PUT";
            return execute(httpClient.preparePut(url));
        }

        /** Execute a DELETE request.*/
        @Override
        public HttpResponse delete() {
            this.type = "DELETE";
            try {
                return new HttpAsyncResponse(prepare(httpClient.prepareDelete(url)).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a DELETE request asynchronously.*/
        @Override
        public Promise<HttpResponse> deleteAsync() {
            this.type = "DELETE";
            return execute(httpClient.prepareDelete(url));
        }

        /** Execute a OPTIONS request.*/
        @Override
        public HttpResponse options() {
            this.type = "OPTIONS";
            try {
                return new HttpAsyncResponse(prepare(httpClient.prepareOptions(url)).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a OPTIONS request asynchronously.*/
        @Override
        public Promise<HttpResponse> optionsAsync() {
            this.type = "OPTIONS";
            return execute(httpClient.prepareOptions(url));
        }

        /** Execute a HEAD request.*/
        @Override
        public HttpResponse head() {
            this.type = "HEAD";
            try {
                return new HttpAsyncResponse(prepare(httpClient.prepareHead(url)).execute().get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a HEAD request asynchronously.*/
        @Override
        public Promise<HttpResponse> headAsync() {
            this.type = "HEAD";
            return execute(httpClient.prepareHead(url));
        }

        /** Execute a TRACE request.*/
        @Override
        public HttpResponse trace() {
            this.type = "TRACE";
            throw new NotImplementedException();
        }

        /** Execute a TRACE request asynchronously.*/
        @Override
        public Promise<HttpResponse> traceAsync() {
            this.type = "TRACE";
            throw new NotImplementedException();
        }

        private WSRequest sign() {
            if (this.oauthToken != null && this.oauthSecret != null) {
                WSOAuthConsumer consumer = new WSOAuthConsumer(oauthInfo, oauthToken, oauthSecret);
                try {
                    consumer.sign(this, this.type);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return this;
        }

        private BoundRequestBuilder prepare(BoundRequestBuilder builder) {
            checkFileBody(builder);
            if (this.username != null && this.password != null && this.scheme != null) {
                AuthScheme authScheme;
                switch (this.scheme) {
                case DIGEST: authScheme = AuthScheme.DIGEST; break;
                case NTLM: authScheme = AuthScheme.NTLM; break;
                case KERBEROS: authScheme = AuthScheme.KERBEROS; break;
                case SPNEGO: authScheme = AuthScheme.SPNEGO; break;
                case BASIC: authScheme = AuthScheme.BASIC; break;
                default: throw new RuntimeException("Scheme " + this.scheme + " not supported by the UrlFetch WS backend.");
                }
                builder.setRealm(
                        (new RealmBuilder())
                        .setScheme(authScheme)
                        .setPrincipal(this.username)
                        .setPassword(this.password)
                        .build()
                );
            }
            for (String key: this.headers.keySet()) {
                builder.addHeader(key, headers.get(key));
            }
            builder.setFollowRedirects(this.followRedirects);
            PerRequestConfig perRequestConfig = new PerRequestConfig();
            perRequestConfig.setRequestTimeoutInMs(this.timeout * 1000);
            builder.setPerRequestConfig(perRequestConfig);
            return builder;
        }

        private Promise<HttpResponse> execute(BoundRequestBuilder builder) {
            try {
                final Promise<HttpResponse> smartFuture = new Promise<HttpResponse>();
                prepare(builder).execute(new AsyncCompletionHandler<HttpResponse>() {
                    @Override
                    public HttpResponse onCompleted(Response response) throws Exception {
                        HttpResponse httpResponse = new HttpAsyncResponse(response);
                        smartFuture.invoke(httpResponse);
                        return httpResponse;
                    }
                    @Override
                    public void onThrowable(Throwable t) {
                        throw new RuntimeException(t);
                    }
                });

                return smartFuture;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void checkFileBody(BoundRequestBuilder builder) {
            if (this.fileParams != null) {
                //could be optimized, we know the size of this array.
                for (int i = 0; i < this.fileParams.length; i++) {
                    builder.addBodyPart(new FilePart(this.fileParams[i].paramName,
                            this.fileParams[i].file,
                            MimeTypes.getMimeType(this.fileParams[i].file.getName()),
                            encoding));
                }
                if (this.parameters != null) {
                    try {
                        // AHC only supports ascii chars in keys in multipart
                        for (String key : this.parameters.keySet()) {
                            Object value = this.parameters.get(key);
                            if (value instanceof Collection<?> || value.getClass().isArray()) {
                                Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
                                for (Object v : values) {
                                    Part part = new ByteArrayPart(key, null, v.toString().getBytes(encoding), "text/plain", encoding);
                                    builder.addBodyPart( part );
                                }
                            } else {
                                Part part = new ByteArrayPart(key, null, value.toString().getBytes(encoding), "text/plain", encoding);
                                builder.addBodyPart( part );
                            }
                        }
                    } catch(UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
                return;
            }
            if (this.parameters != null && !this.parameters.isEmpty()) {
                boolean isPostPut = "POST".equals(this.type) || ("PUT".equals(this.type));

                if (isPostPut) {
                    // Since AHC is hardcoded to encode using utf-8, we must build
                    // the content our self..
                    StringBuilder sb = new StringBuilder();

                    for (String key : this.parameters.keySet()) {
                        Object value = this.parameters.get(key);
                        if (value == null) continue;


                        if (value instanceof Collection<?> || value.getClass().isArray()) {
                            Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
                            for (Object v: values) {
                                if (sb.length() > 0) {
                                    sb.append('&');
                                }
                                sb.append(encode(key));
                                sb.append('=');
                                sb.append(encode(v.toString()));
                            }
                        } else {
                            // Since AHC is hardcoded to encode using utf-8, we must build
                            // the content our self..
                            if (sb.length() > 0) {
                                sb.append('&');
                            }
                            sb.append(encode(key));
                            sb.append('=');
                            sb.append(encode(value.toString()));
                        }
                    }
                    try {
                        byte[] bodyBytes = sb.toString().getBytes( this.encoding );
                        InputStream bodyInStream = new ByteArrayInputStream( bodyBytes );
                        builder.setBody( bodyInStream );
                    } catch ( UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }


                } else {
                    for (String key : this.parameters.keySet()) {
                        Object value = this.parameters.get(key);
                        if (value == null) continue;
                        if (value instanceof Collection<?> || value.getClass().isArray()) {
                            Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
                            for (Object v: values) {
                                // must encode it since AHC uses raw urls
                                builder.addQueryParameter(encode(key), encode(v.toString()));
                            }
                        } else {
                            // must encode it since AHC uses raw urls
                            builder.addQueryParameter(encode(key), encode(value.toString()));
                        }
                    }

                }
            }
            if (this.body != null) {
                if (this.parameters != null && !this.parameters.isEmpty()) {
                    throw new RuntimeException("POST or PUT method with parameters AND body are not supported.");
                }
                if(this.body instanceof InputStream) {
                    builder.setBody((InputStream)this.body);
                } else {
                    if(this.body != null) {
                        try {
                            byte[] bodyBytes = this.body.toString().getBytes( this.encoding );
                            InputStream bodyInStream = new ByteArrayInputStream( bodyBytes );
                            builder.setBody( bodyInStream );
                        } catch ( UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            String contentType;
            if(this.mimeType != null) {
                contentType = this.mimeType;
            } else {
                contentType = "application/x-www-form-urlencoded";
            }
            this.headers.put("Content-Type", contentType + "; charset="+this.encoding);

        }

    }

    /**
     * An HTTP response wrapper
     */
    public static class HttpAsyncResponse extends HttpResponse {

        private Response response;

        /**
         * you shouldnt have to create an HttpResponse yourself
         * @param method
         */
        public HttpAsyncResponse(Response response) {
            this.response = response;
        }

        /**
         * the HTTP status code
         * @return the status code of the http response
         */
        @Override
        public Integer getStatus() {
            return this.response.getStatusCode();
        }

        @Override
        public String getHeader(String key) {
            return response.getHeader(key);
        }

        @Override
        public List<Header> getHeaders() {
            Map<String, List<String>> hdrs = response.getHeaders();
            List<Header> result = new ArrayList<Header>();
            for (String key: hdrs.keySet()) {
                result.add(new Header(key, hdrs.get(key)));
            }
            return result;
        }

        /**
         * get the response as a stream
         * @return an inputstream
         */
        @Override
        public InputStream getStream() {
            try {
                return response.getResponseBodyAsStream();
            } catch (IllegalStateException e) {
                return new ByteArrayInputStream(new byte[]{}); // Workaround AHC's bug on empty responses
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    private static class WSOAuthConsumer extends AbstractOAuthConsumer {

        public WSOAuthConsumer(String consumerKey, String consumerSecret) {
            super(consumerKey, consumerSecret);
        }

        public WSOAuthConsumer(ServiceInfo info, String token, String secret) {
            super(info.consumerKey, info.consumerSecret);
            setTokenWithSecret(token, secret);
        }

        @Override
        protected HttpRequest wrap(Object request) {
            if (!(request instanceof WSRequest)) {
                throw new IllegalArgumentException("WSOAuthConsumer expects requests of type play.libs.WS.WSRequest");
            }
            return new WSRequestAdapter((WSRequest)request);
        }

        public WSRequest sign(WSRequest request, String method) throws OAuthMessageSignerException, OAuthExpectationFailedException, OAuthCommunicationException {
            WSRequestAdapter req = (WSRequestAdapter)wrap(request);
            req.setMethod(method);
            sign(req);
            return request;
        }

        public class WSRequestAdapter implements HttpRequest {

            private WSRequest request;
            private String method;

            public WSRequestAdapter(WSRequest request) {
                this.request = request;
            }

            public Map<String, String> getAllHeaders() {
                return request.headers;
            }

            public String getContentType() {
                return request.mimeType;
            }

            public String getHeader(String name) {
                return request.headers.get(name);
            }

            public InputStream getMessagePayload() throws IOException {
                return null;
            }

            public String getMethod() {
                return this.method;
            }

            private void setMethod(String method) {
                this.method = method;
            }

            public String getRequestUrl() {
                return request.url;
            }

            public void setHeader(String name, String value) {
                request.setHeader(name, value);
            }

            public void setRequestUrl(String url) {
                request.url = url;
            }

        }

    }

}