module google.cloud.bigquery {
    requires auto.value.annotations;
    requires com.google.api.client;
    requires com.google.api.client.json.jackson2;
    requires com.google.auth;
    requires com.google.auth.oauth2;
    requires java.logging;
    requires org.checkerframework.checker.qual;
    requires org.threeten.bp;

    requires transitive com.google.api.apicommon;
    requires transitive com.google.api.services.bigquery;
    requires transitive com.google.common;
    requires transitive gax;
    requires transitive google.cloud.core;
    requires transitive google.cloud.core.http;
    requires transitive jsr305;

    exports com.google.cloud.bigquery;
    exports com.google.cloud.bigquery.benchmark;
    exports com.google.cloud.bigquery.spi;
    exports com.google.cloud.bigquery.spi.v2;
    exports com.google.cloud.bigquery.testing;
}
