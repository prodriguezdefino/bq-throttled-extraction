module com.google.api.client {
    requires com.google.common;
    requires j2objc.annotations;
    requires opencensus.contrib.http.util;

    requires transitive java.logging;
    requires transitive jsr305;
    requires transitive opencensus.api;
    requires transitive org.apache.httpcomponents.httpclient;
    requires transitive org.apache.httpcomponents.httpcore;

    exports com.google.api.client.http;
    exports com.google.api.client.http.apache;
    exports com.google.api.client.http.javanet;
    exports com.google.api.client.http.json;
    exports com.google.api.client.json;
    exports com.google.api.client.json.rpc2;
    exports com.google.api.client.json.webtoken;
    exports com.google.api.client.testing.http;
    exports com.google.api.client.testing.http.apache;
    exports com.google.api.client.testing.http.javanet;
    exports com.google.api.client.testing.json;
    exports com.google.api.client.testing.json.webtoken;
    exports com.google.api.client.testing.util;
    exports com.google.api.client.util;
    exports com.google.api.client.util.escape;
    exports com.google.api.client.util.store;

}
