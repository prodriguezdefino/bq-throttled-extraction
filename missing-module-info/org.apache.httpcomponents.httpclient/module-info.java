module org.apache.httpcomponents.httpclient {
    requires java.naming;
    requires org.apache.commons.codec;

    requires transitive commons.logging;
    requires transitive java.security.jgss;
    requires transitive org.apache.httpcomponents.httpcore;

    exports org.apache.http.auth;
    exports org.apache.http.auth.params;
    exports org.apache.http.client;
    exports org.apache.http.client.config;
    exports org.apache.http.client.entity;
    exports org.apache.http.client.methods;
    exports org.apache.http.client.params;
    exports org.apache.http.client.protocol;
    exports org.apache.http.client.utils;
    exports org.apache.http.conn;
    exports org.apache.http.conn.params;
    exports org.apache.http.conn.routing;
    exports org.apache.http.conn.scheme;
    exports org.apache.http.conn.socket;
    exports org.apache.http.conn.ssl;
    exports org.apache.http.conn.util;
    exports org.apache.http.cookie;
    exports org.apache.http.cookie.params;
    exports org.apache.http.impl.auth;
    exports org.apache.http.impl.client;
    exports org.apache.http.impl.conn;
    exports org.apache.http.impl.conn.tsccm;
    exports org.apache.http.impl.cookie;
    exports org.apache.http.impl.execchain;

}
