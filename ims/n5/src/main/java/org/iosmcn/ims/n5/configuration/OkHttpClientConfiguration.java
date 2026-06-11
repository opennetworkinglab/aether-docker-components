package org.iosmcn.ims.n5.configuration;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for OkHttpClient.
 * This class provides a bean definition for an OkHttpClient instance
 * configured to use HTTP/2 without an initial HTTP/1.1 negotiation.
 */
@Configuration
public class OkHttpClientConfiguration {

    /**
     * Creates and provides a configured OkHttpClient bean.
     * The client is set to use HTTP/2 Prior Knowledge mode,
     * which allows direct communication over HTTP/2 without
     * an upgrade request.
     *
     * @return A configured OkHttpClient instance.
     */
    @Bean("okHttpClient")
    public OkHttpClient okHttpClient() {
        ConnectionPool connectionPool = new ConnectionPool(5, 5, TimeUnit.MINUTES);

        return new OkHttpClient.Builder()
                .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .connectionPool(connectionPool)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
}
