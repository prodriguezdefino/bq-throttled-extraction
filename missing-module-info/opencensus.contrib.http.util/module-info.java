module opencensus.contrib.http.util {
    requires com.google.common;

    requires transitive jsr305;
    requires transitive opencensus.api;

    exports io.opencensus.contrib.http;
    exports io.opencensus.contrib.http.util;

}
