module com.google.auth.oauth2 {
    requires auto.value.annotations;
    requires com.google.api.client.json.jackson2;
    requires com.google.common;
    requires java.logging;
    requires jsr305;

    requires transitive com.google.api.client;
    requires transitive com.google.auth;

    exports com.google.auth.http;
    exports com.google.auth.oauth2;

    uses com.google.auth.http.HttpTransportFactory; 

}
