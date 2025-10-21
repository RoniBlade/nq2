package ru.tedo.Service;

import com.google.common.io.CharStreams;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tedo.Model.CommonHttpResponseEntity;
import ru.tedo.Model.HttpProxyEntity;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Map;

public class CommonHTTPrequestService {
    final static Logger logger = LoggerFactory.getLogger(CommonHTTPrequestService.class);

    public CommonHttpResponseEntity processHttpGetRequest(String url, Integer timeoutSeconds, HttpProxyEntity proxy) throws Exception {
        return processHttpGetRequest(url,timeoutSeconds,proxy,null);
    }
    public CommonHttpResponseEntity processHttpGetRequest(String url, Integer timeoutSeconds, HttpProxyEntity proxy, SSLContext sslContext) throws Exception {
        CloseableHttpClient httpClient = getClient(url,timeoutSeconds,proxy,sslContext);
        HttpGet httpget = new HttpGet(url);

        CloseableHttpResponse response = httpClient.execute(httpget);
        HttpEntity entity = response.getEntity();
        InputStream in = entity.getContent();
        String respBody = CharStreams.toString(new InputStreamReader(
                in, Charsets.UTF_8));
        httpClient.getConnectionManager().shutdown();
        logger.info("HTTPs request URL= " + url);
        logger.info("HTTPs result = " + respBody);
        if (response.getStatusLine().getStatusCode() != 200) {
            logger.error("Error processing HTTP request with code " + response.getStatusLine().getStatusCode());
            throw new Exception("Http error with code " + response.getStatusLine().getStatusCode());
        }
        CommonHttpResponseEntity result = new CommonHttpResponseEntity();
        result.setResponseCode(response.getStatusLine().getStatusCode() );
        result.setBody(respBody);
        Header[] headers = response.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            Header h = headers[i];
            result.getHeaders().put(h.getName(),h.getValue());
        }
        return result;
    }

    public CommonHttpResponseEntity processHttpPostRequest(String url, String body, int timeoutSeconds, HttpProxyEntity proxy, Map<String,String> httpHeaders) throws Exception {
        return processHttpPostRequest(url,body,timeoutSeconds,proxy, null,  httpHeaders);
    }
    public CommonHttpResponseEntity processHttpPostRequest(String url, String body, int timeoutSeconds, HttpProxyEntity proxy, SSLContext sslContext, Map<String,String> httpHeaders) throws Exception {
        CloseableHttpClient httpClient = getClient(url,timeoutSeconds,proxy,sslContext);
        HttpPost httpPost = new HttpPost(url);
        if (httpHeaders != null) {
            for (Map.Entry<String, String> ent : httpHeaders.entrySet()) {
                httpPost.setHeader(ent.getKey(), ent.getValue());
            }
        }
        StringEntity bodyEntity = new StringEntity(body);
        httpPost.setEntity(bodyEntity);
        CloseableHttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = response.getEntity();
        InputStream in = entity.getContent();
        String respBody = CharStreams.toString(new InputStreamReader(
                in, Charsets.UTF_8));
        httpClient.getConnectionManager().shutdown();
        logger.info("HTTPs request URL= " + url);
        logger.info("HTTPs result = " + respBody);
        if (response.getStatusLine().getStatusCode() != 200) {
            logger.error("Error processing HTTP request with code " + response.getStatusLine().getStatusCode());
            throw new Exception("Http error with code " + response.getStatusLine().getStatusCode());
        }
        CommonHttpResponseEntity result = new CommonHttpResponseEntity();
        result.setResponseCode(response.getStatusLine().getStatusCode() );
        result.setBody(respBody);
        Header[] headers = response.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            Header h = headers[i];
            result.getHeaders().put(h.getName(),h.getValue());
        }
        return result;
    }

    private CloseableHttpClient getClient(String url, Integer timeoutSeconds, HttpProxyEntity proxy, SSLContext sslContext) throws Exception {

        HttpClientBuilder builder = HttpClients.custom();

        if (url.toUpperCase(Locale.ROOT).startsWith("HTTPS:")) {
            // если ничего не указано, то доверяем всем сертификатам
            if (sslContext == null) {
                CustomSSLContext cc = new CustomSSLContext();
                sslContext = cc.getSSLContext();
            }
            builder.setSSLContext(sslContext);
        }

        if (timeoutSeconds > 0) {
            RequestConfig.Builder requestConfig = RequestConfig.custom();
            requestConfig.setConnectTimeout(timeoutSeconds * 1000);
            requestConfig.setConnectionRequestTimeout(timeoutSeconds * 1000);
            requestConfig.setSocketTimeout(timeoutSeconds * 1000);
            builder.setDefaultRequestConfig(requestConfig.build());
        }

        if (proxy != null && !proxy.getProxyHost().isEmpty()) {
            logger.info("Add proxy host & port: " + proxy.getProxyHost() + ":" + proxy.getProxyPort());
            HttpHost proxyh = new HttpHost(proxy.getProxyHost(), proxy.getProxyPort());
            builder.setProxy(proxyh);
        }

        if (proxy != null && !proxy.getProxyUser().isEmpty()) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(proxy.getProxyHost(), proxy.getProxyPort()),
                    new UsernamePasswordCredentials(proxy.getProxyUser(), proxy.getProxyPassword()));
            builder.setDefaultCredentialsProvider(credsProvider);
            logger.info("Add proxy credentials : " + proxy.getProxyUser() + "/" + proxy.getProxyPassword());
        }

        CloseableHttpClient httpClient = builder.build();
        return httpClient;

    }
}

class CustomSSLContext {
    public SSLContext getSSLContext() throws Exception {
        TrustManager[] trustAllCertificates = new TrustManager[]{new TrustAllCertificates()};

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());

        return sslContext;
    }
}

class TrustAllCertificates implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        // Allow all client certificates
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
        // Allow all server certificates
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}

