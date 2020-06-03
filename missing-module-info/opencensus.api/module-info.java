module opencensus.api {
    requires java.logging;

    requires transitive grpc.context;
    requires transitive jsr305;

    exports io.opencensus.common;
    exports io.opencensus.internal;
    exports io.opencensus.metrics;
    exports io.opencensus.metrics.data;
    exports io.opencensus.metrics.export;
    exports io.opencensus.resource;
    exports io.opencensus.stats;
    exports io.opencensus.tags;
    exports io.opencensus.tags.propagation;
    exports io.opencensus.tags.unsafe;
    exports io.opencensus.trace;
    exports io.opencensus.trace.config;
    exports io.opencensus.trace.export;
    exports io.opencensus.trace.internal;
    exports io.opencensus.trace.propagation;
    exports io.opencensus.trace.samplers;
    exports io.opencensus.trace.unsafe;

}
