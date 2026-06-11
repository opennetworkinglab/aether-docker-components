package org.iosmcn.ims.n5.configuration;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
public class OkHttpPriorKnowledgeClientConfiguration {

    @Bean("OkHttpPriorKnowledgeClient")
    public OkHttpClient OkHttpPriorKnowledgeClient() {
        ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);

        return new OkHttpClient.Builder()
                .protocols(Arrays.asList(Protocol.H2_PRIOR_KNOWLEDGE))
                .connectionPool(connectionPool)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }
}
