package br.edu.udesc.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private String jwtSecret = "troque-este-segredo-com-no-minimo-32-caracteres";
    private String adminKey = "admin-dev-key";
    private Services services = new Services();

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getAdminKey() {
        return adminKey;
    }

    public void setAdminKey(String adminKey) {
        this.adminKey = adminKey;
    }

    public Services getServices() {
        return services;
    }

    public void setServices(Services services) {
        this.services = services;
    }

    public static class Services {
        private String authUrl = "http://localhost:8082";
        private String ocorrenciaUrl = "http://localhost:8083";
        private String itemUrl = "http://localhost:8081";

        public String getAuthUrl() {
            return authUrl;
        }

        public void setAuthUrl(String authUrl) {
            this.authUrl = authUrl;
        }

        public String getOcorrenciaUrl() {
            return ocorrenciaUrl;
        }

        public void setOcorrenciaUrl(String ocorrenciaUrl) {
            this.ocorrenciaUrl = ocorrenciaUrl;
        }

        public String getItemUrl() {
            return itemUrl;
        }

        public void setItemUrl(String itemUrl) {
            this.itemUrl = itemUrl;
        }
    }
}
