module com.google.api.client.auth {
    requires com.google.common;
    requires java.logging;

    requires transitive com.google.api.client;

    exports com.google.api.client.auth.oauth;
    exports com.google.api.client.auth.oauth2;
    exports com.google.api.client.auth.openidconnect;

}
