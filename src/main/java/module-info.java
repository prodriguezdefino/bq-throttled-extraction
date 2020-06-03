module bq.export.propagator {
    requires com.google.gson;
    requires commons.cli;
    requires gax;
    requires google.cloud.bigquery;
    requires google.cloud.core;
    requires google.cloud.storage;
    requires java.logging;

    exports org.example.bqexportpropagator;
}
