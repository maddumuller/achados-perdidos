package br.edu.udesc.gateway.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "request_logs")
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private String path;

    @Column
    private String routeName;

    @Column(nullable = false)
    private int status;

    @Column(nullable = false)
    private long durationMs;

    @Column(nullable = false)
    private String clientIp;

    @Column
    private String userId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public RequestLog() {
    }

    public RequestLog(String correlationId, String method, String path, String routeName,
                      int status, long durationMs, String clientIp, String userId, Instant createdAt) {
        this.correlationId = correlationId;
        this.method = method;
        this.path = path;
        this.routeName = routeName;
        this.status = status;
        this.durationMs = durationMs;
        this.clientIp = clientIp;
        this.userId = userId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
