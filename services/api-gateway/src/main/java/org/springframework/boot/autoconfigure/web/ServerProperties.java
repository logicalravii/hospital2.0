package org.springframework.boot.autoconfigure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl;

@ConfigurationProperties(prefix = "server", ignoreUnknownFields = true)
public class ServerProperties {

    private final Http2 http2 = new Http2();
    private Ssl ssl;

    public Http2 getHttp2() {
        return http2;
    }

    public Ssl getSsl() {
        return ssl;
    }

    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }
}
