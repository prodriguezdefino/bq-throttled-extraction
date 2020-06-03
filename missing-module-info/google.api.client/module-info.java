module google.api.client {
    requires com.google.api.client.json.jackson2;
    requires com.google.common;
    requires java.logging;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;

    requires transitive com.google.api.client;
    requires transitive com.google.api.client.auth;

    exports com.google.api.client.googleapis;
    exports com.google.api.client.googleapis.apache;
    exports com.google.api.client.googleapis.auth.oauth2;
    exports com.google.api.client.googleapis.batch;
    exports com.google.api.client.googleapis.batch.json;
    exports com.google.api.client.googleapis.compute;
    exports com.google.api.client.googleapis.javanet;
    exports com.google.api.client.googleapis.json;
    exports com.google.api.client.googleapis.media;
    exports com.google.api.client.googleapis.notifications;
    exports com.google.api.client.googleapis.notifications.json;
    exports com.google.api.client.googleapis.services;
    exports com.google.api.client.googleapis.services.json;
    exports com.google.api.client.googleapis.testing;
    exports com.google.api.client.googleapis.testing.auth.oauth2;
    exports com.google.api.client.googleapis.testing.compute;
    exports com.google.api.client.googleapis.testing.json;
    exports com.google.api.client.googleapis.testing.notifications;
    exports com.google.api.client.googleapis.testing.services;
    exports com.google.api.client.googleapis.testing.services.json;
    exports com.google.api.client.googleapis.util;

}
