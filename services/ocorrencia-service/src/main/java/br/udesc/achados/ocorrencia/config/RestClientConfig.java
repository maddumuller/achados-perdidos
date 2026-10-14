package br.udesc.achados.ocorrencia.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(OcorrenciaProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient itemServiceRestClient(OcorrenciaProperties props) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        return RestClient.builder()
                .baseUrl(props.itemServiceUrl())
                .requestFactory(new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient))
                .build();
    }
}
