module google.cloud.core {
    requires auto.value.annotations;
    requires com.google.api.client;
    requires com.google.api.client.json.jackson2;
    requires com.google.protobuf.util;

    requires transitive com.google.api.apicommon;
    requires transitive com.google.auth;
    requires transitive com.google.auth.oauth2;
    requires transitive com.google.common;
    requires transitive com.google.protobuf;
    requires transitive gax;
    requires transitive java.logging;
    requires transitive java.sql;
    requires transitive jsr305;
    requires transitive org.threeten.bp;
    requires transitive proto.google.common.protos;
    requires transitive proto.google.iam.v1;
    requires transitive google.cloud.bigquery;

    exports com.google.cloud;
    exports com.google.cloud.spi;
    exports com.google.cloud.testing;

    uses com.google.cloud.bigquery.BigQueryFactory;
    uses com.google.cloud.bigquery.spi.BigQueryRpcFactory;
}
