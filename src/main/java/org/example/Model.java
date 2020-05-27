package org.example;

import com.google.cloud.bigquery.QueryParameterValue;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Holds the classes and entities that represents the model for the solution
 */
public class Model {

    /**
     * Represents the results of the propagation event.
     */
    record PropagationResult(String path, String executionDateString, Integer payloadSize, Optional<String> messageResult) {

        /**
         * Returns the size of a propagation request payload in kilobytes.
         *
         * @return the payload's size.
         */
        public Integer payloadSizeInKB() {
            return payloadSize / 1024;
        }
    }

    /**
     * Captures the command line arguments for ease of consumption.
     */
    record Arguments(String project, String destinationDataset, String exportDestinationTable,
    String exportBucketName, String exportBucketPathPrefix, String bqQuery,
    Map<String, QueryParameterValue> bqQueryParams) {
    }

    /**
     * Serves as the accumulation facility before propagation occurs. The general logic is simply to hold data until the
     * chunk size crosses the configured threshold and then propagates it using the provided propagation function.
     */
    static class Accumulator {

        private final BiFunction<String, List<String>, PropagationResult> propagationFunction;
        private final String fileLocation;
        private Stream.Builder<PropagationResult> resultsBuilder = Stream.builder();
        private final Supplier<Stream<PropagationResult>> resultStreamSupplier = () -> resultsBuilder.build();
        private Long accumulatedSize = 0L;
        private final Long accumulatedSizeLimit;
        private final List<String> accumulatedEntries = new LinkedList<>();
        private final Throttler throttler;

        public Accumulator(Long accumulatedSizeLimit, String fileLocation,
                BiFunction<String, List<String>, PropagationResult> propagationFunction, Throttler throttler) {
            this.propagationFunction = propagationFunction;
            this.fileLocation = fileLocation;
            this.throttler = throttler;
            this.accumulatedSizeLimit = accumulatedSizeLimit;
        }

        /**
         * Takes an entry and accumulates it, based on the current accumulated size it may trigger the propagation after
         * throttling the attempt.
         *
         * @param entry the entry to be accumulated.
         * @return this same instance.
         */
        public Accumulator accumulate(String entry) {
            if (accumulatedEntries.add(entry)) {
                accumulatedSize += entry.getBytes().length;
                if (accumulatedSize >= accumulatedSizeLimit) {
                    triggerPropagation();
                }
            }
            return this;
        }

        /**
         * Forces the propagation of the current accumulated state.
         *
         * @return the same instance, but with its previous content drained.
         */
        public Accumulator drain() {
            if (accumulatedSize > 0) {
                triggerPropagation();
            }
            return this;
        }

        /**
         * Returns the propagation results.
         *
         * @return
         */
        public Stream<PropagationResult> getResults() {
            return resultStreamSupplier.get();
        }

        /**
         * Helper method to combine multiple instances of accumulators.
         *
         * @param acc1 first accumulator
         * @param acc2 second accumulator
         * @return the first accumulator instance with the incorporation of the second accumulator's data
         */
        public static Accumulator combine(Accumulator acc1, Accumulator acc2) {
            acc2.accumulatedEntries.forEach(entry -> {
                acc1.accumulate(entry);
            });
            acc2.resultsBuilder.build().forEach(result -> acc1.resultsBuilder.accept(result));
            return acc1;
        }

        /**
         * Triggers the propagation of the accumulated entries, uses the configured Throttle before propagation.
         */
        private void triggerPropagation() {
            throttler.throttle();
            resultsBuilder.accept(propagationFunction.apply(fileLocation, accumulatedEntries));
            accumulatedEntries.clear();
            accumulatedSize = 0L;
        }

    }

    /**
     * Defines a Throttling behavior to be used by the Accumulator when propagating data.
     */
    public interface Throttler {

        /**
         * Will decide to throttle the execution based on some internal state.
         */
        void throttle();
    }

    /**
     * Basic implementation for a throttler, using sleep method in Thread will hold the execution if the repeated calls
     * are produced before the min wait time (hold by the internal state). This implementation class is not thread safe,
     * but that's fine in this context since there is only one Accumulator at a time and no parallel processing of the
     * data.
     */
    static class SimpleThrottler implements Throttler {

        private Long last = Instant.now().toEpochMilli();
        private final Long millisToWait;

        public SimpleThrottler(Long millisToWait) {
            this.millisToWait = millisToWait;
        }

        @Override
        public void throttle() {
            var end = Instant.now();
            var timeToSleep = millisToWait - (end.toEpochMilli() - last);
            // check if we need to sleep to throttle the requests based on the configured time.
            if (timeToSleep > 0) {
                try {
                    Thread.sleep(timeToSleep);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Model.class.getName()).log(Level.SEVERE, "Thread sleep innterruption!", ex);
                    throw new RuntimeException(ex);
                }
            }
            last = Instant.now().toEpochMilli();
        }
    }

}
