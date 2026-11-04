package org.springframework.boot.web.context;

import org.springframework.context.ApplicationContext;

public interface WebServerApplicationContext extends ApplicationContext {

    static boolean hasServerNamespace(ApplicationContext context, String serverNamespace) {
        return org.springframework.boot.web.server.context.WebServerApplicationContext.hasServerNamespace(context, serverNamespace);
    }
}
