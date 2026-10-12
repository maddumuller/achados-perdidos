package br.udesc.achados.ocorrencia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OcorrenciaApplication {

    public static void main(String[] args) {
        SpringApplication.run(OcorrenciaApplication.class, args);
    }
}
