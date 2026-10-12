package br.edu.udesc.gateway.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "routes")
public class RouteDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String pathPrefix;

    @NotBlank
    @Column(nullable = false)
    private String targetUrl;

    @Column(nullable = false)
    private boolean stripPrefix = false;

    @Column(nullable = false)
    private boolean requiresAuth = false;

    @Column
    private String requiredRole;

    @Min(1)
    @Column(nullable = false)
    private int rateLimitPerMinute = 60;

    @Column(nullable = false)
    private boolean enabled = true;

    public RouteDefinition() {
    }

    public RouteDefinition(String name, String pathPrefix, String targetUrl, boolean stripPrefix,
                           boolean requiresAuth, String requiredRole, int rateLimitPerMinute, boolean enabled) {
        this.name = name;
        this.pathPrefix = pathPrefix;
        this.targetUrl = targetUrl;
        this.stripPrefix = stripPrefix;
        this.requiresAuth = requiresAuth;
        this.requiredRole = requiredRole;
        this.rateLimitPerMinute = rateLimitPerMinute;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public boolean isStripPrefix() {
        return stripPrefix;
    }

    public void setStripPrefix(boolean stripPrefix) {
        this.stripPrefix = stripPrefix;
    }

    public boolean isRequiresAuth() {
        return requiresAuth;
    }

    public void setRequiresAuth(boolean requiresAuth) {
        this.requiresAuth = requiresAuth;
    }

    public String getRequiredRole() {
        return requiredRole;
    }

    public void setRequiredRole(String requiredRole) {
        this.requiredRole = requiredRole;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
