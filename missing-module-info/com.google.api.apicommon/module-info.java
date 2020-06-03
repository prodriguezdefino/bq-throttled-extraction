module com.google.api.apicommon {
    requires transitive com.google.common;
    requires transitive jsr305;

    exports com.google.api.core;
    exports com.google.api.pathtemplate;
    exports com.google.api.resourcenames;

}
