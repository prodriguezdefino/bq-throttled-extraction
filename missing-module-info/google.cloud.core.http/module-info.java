module google.cloud.core.http {
    requires com.google.api.client.extensions.appengine;
    requires com.google.auth;
    requires com.google.common;
    requires gax;
    requires gax.httpjson;
    requires jsr305;
    requires opencensus.contrib.http.util;

    requires transitive com.google.api.apicommon;
    requires transitive com.google.api.client;
    requires transitive com.google.auth.oauth2;
    requires transitive google.api.client;
    requires transitive google.cloud.core;
    requires transitive opencensus.api;

    exports com.google.cloud.http;

}
