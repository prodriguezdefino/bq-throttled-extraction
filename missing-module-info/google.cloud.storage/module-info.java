module google.cloud.storage {
    requires com.google.api.client.json.jackson2;
    requires com.google.auth.oauth2;
    requires com.google.common;
    requires java.logging;
    requires opencensus.api;
    requires org.threeten.bp;

    requires transitive com.google.api.apicommon;
    requires transitive com.google.api.client;
    requires transitive com.google.api.services.storage;
    requires transitive com.google.auth;
    requires transitive gax;
    requires transitive google.api.client;
    requires transitive google.cloud.core;
    requires transitive google.cloud.core.http;

    exports com.google.cloud.storage;
    exports com.google.cloud.storage.spi;
    exports com.google.cloud.storage.spi.v1;
    exports com.google.cloud.storage.testing;
}
