module gax {
    requires com.google.api.client;
    requires com.google.auth.oauth2;
    requires java.logging;
    requires opencensus.api;

    requires transitive com.google.api.apicommon;
    requires transitive com.google.auth;
    requires transitive com.google.common;
    requires transitive jsr305;
    requires transitive org.threeten.bp;

    exports com.google.api.gax.batching;
    exports com.google.api.gax.core;
    exports com.google.api.gax.longrunning;
    exports com.google.api.gax.paging;
    exports com.google.api.gax.retrying;
    exports com.google.api.gax.rpc;
    exports com.google.api.gax.rpc.internal;
    exports com.google.api.gax.tracing;
}
