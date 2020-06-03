module gax.httpjson {
    requires com.google.api.client.json.jackson2;
    requires com.google.auth.oauth2;
    requires com.google.common;

    requires transitive com.google.api.apicommon;
    requires transitive com.google.api.client;
    requires transitive com.google.auth;
    requires transitive com.google.gson;
    requires transitive gax;
    requires transitive jsr305;
    requires transitive org.threeten.bp;

    exports com.google.api.gax.httpjson;

}
