package com.example.demo;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

import jakarta.annotation.PreDestroy;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private int port;

    private RestClient restClient;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        restClient = RestClient.builder(
                new HttpHost(host, port, "http")
        ).build();
        ElasticsearchTransport transport =
                new RestClientTransport(
                        restClient,
                        new JacksonJsonpMapper()
                );
        return new ElasticsearchClient(transport);
    }

    @PreDestroy
    public void close() throws Exception {
        if (restClient != null) {
            restClient.close();
        }
    }
}