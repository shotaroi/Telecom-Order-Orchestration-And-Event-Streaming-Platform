package com.telecom.platform.support.config;

import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    @Value("${spring.opensearch.host:localhost}")
    private String host;

    @Value("${spring.opensearch.port:9200}")
    private int port;

    @Bean
    public OpenSearchClient openSearchClient() {
        String url = "http://" + host + ":" + port;
        RestClient restClient = RestClient.builder(url).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new OpenSearchClient(transport);
    }
}
