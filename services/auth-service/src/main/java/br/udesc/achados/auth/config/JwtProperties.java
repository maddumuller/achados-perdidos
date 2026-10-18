package br.udesc.achados.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class JwtProperties {

    private String jwtSecret = "troque-este-segredo-com-no-minimo-32-caracteres";
    private int jwtExpirationMinutes = 120;
    private String adminEmail = "admin@achados.local";
    private String adminPassword = "Admin@1234";

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public int getJwtExpirationMinutes() {
        return jwtExpirationMinutes;
    }

    public void setJwtExpirationMinutes(int jwtExpirationMinutes) {
        this.jwtExpirationMinutes = jwtExpirationMinutes;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}
