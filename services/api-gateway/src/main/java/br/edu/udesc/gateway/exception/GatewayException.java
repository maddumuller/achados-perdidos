package br.edu.udesc.gateway.exception;

import java.util.Collections;
import java.util.Map;

public class GatewayException extends RuntimeException {

    private final int status;
    private final Map<String, String> headers;

    public GatewayException(int status, String message) {
        super(message);
        this.status = status;
        this.headers = Collections.emptyMap();
    }

    public GatewayException(int status, String message, Map<String, String> headers) {
        super(message);
        this.status = status;
        this.headers = headers != null ? headers : Collections.emptyMap();
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
