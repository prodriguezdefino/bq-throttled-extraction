module com.google.protobuf.util {
    requires com.google.common;
    requires com.google.gson;
    requires java.logging;

    requires transitive com.google.protobuf;

    exports com.google.protobuf.util;

}
