package org.example;

import com.google.cloud.storage.Blob;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.example.BQExportPropagator.DATE_FORMATTER;
import org.example.Model.*;

/**
 * Holds utility functions.
 */
public class Functions {

    private static final Logger LOG = Logger.getLogger(Functions.class.getCanonicalName());
    private static final Gson GSON = new Gson();

    /**
     * Process a GCS blob file reading its content and propagating them based on the configured size limit and rate for
     * the target destination.
     *
     * @param accumulationSizeLimit
     * @param millisPerPropagationEvent
     * @param file the GCS blob file to be process
     * @return a Stream of results, one per propagation event
     */
    static Stream<PropagationResult> processGCSBlob(Long accumulationSizeLimit,
            Long millisPerPropagationEvent, Blob file) {
        Stream<PropagationResult> results = Stream.empty();
        // Open a channel to read data from the GCS blob
        try ( var reader = new BufferedReader(Channels.newReader(file.reader(), Charset.defaultCharset()))) {
            // stream through the lines in the blob file and accumulate entries for propagation
            results = reader
                    .lines()
                    .reduce(new Accumulator(accumulationSizeLimit, file.getSelfLink(), Functions::dummyPropagate,
                            new SimpleThrottler(millisPerPropagationEvent)),
                            (accumulator, json) -> accumulator.accumulate(json),
                            Accumulator::combine)
                    // lets make sure we drain the accumulation before getting the results
                    .drain()
                    .getResults();
            // TODO: maybe here keep track of the processed files in case of error
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, String.format("Error while reading from GCS for file %s", file.getSelfLink()), ex);
        }
        return results;
    }

    /**
     * Dummy propagation function, it stores the chunked entries from the original file location in a local folder to
     * simulate some latency.
     *
     * @param fileLocation original file location
     * @param accumulatedEntries a list of entries to be propagated
     * @return the propagation results
     */
    private static PropagationResult dummyPropagate(String fileLocation, List<String> accumulatedEntries) {
        var processingDateString = DATE_FORMATTER.format(LocalDateTime.now());
        var arrayPayload = accumulatedEntries.stream().collect(Collectors.joining(","));
        var records = GSON.fromJson(String.format("[%s]", arrayPayload), JsonArray.class);
        var requestJson = new JsonObject();
        requestJson.add("records", records);
        var payload = GSON.toJson(requestJson);
        var size = payload.getBytes().length;
        var message = Optional.of(String.format("Sent to endpoint payload %s...", payload.substring(0, payload.length() < 30 ? payload.length() : 30)));
        var success = false;

        try {
            // simulate some IO latency since there is no remote call being done
            Files.writeString(Files.createDirectories(Paths.get("temps")).resolve(processingDateString), payload);
            LOG.info(String.format("Propagated %d bytes from %s with message %s at %s", size, fileLocation, message.get(),
                    processingDateString));
            success = true;
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Error storing file", ex);
        }
        return new PropagationResult(fileLocation, processingDateString, size, message, success);
    }

    /**
     * Represents a function with 3 parameters.
     *
     * @param <A> Type of function's first parameter.
     * @param <B> Type of function's second parameter.
     * @param <C> Type of function's third parameter.
     * @param <R> Type of function's return.
     */
    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {

        R apply(A a, B b, C c);

        default <V> TriFunction<A, B, C, V> andThen(Function<? super R, ? extends V> after) {
            Objects.requireNonNull(after);
            return (A a, B b, C c) -> after.apply(apply(a, b, c));
        }
    }

    /**
     * Enables the partial execution of a TriFunction in multiple steps.
     *
     * @param <A> Type of function's first parameter.
     * @param <B> Type of function's second parameter.
     * @param <C> Type of function's third parameter.
     * @param <R> Type of function's return.
     * @param func TriFunction instance to partially apply.
     * @return a curried function representation.
     */
    public static <A, B, C, R> Function<A, Function<B, Function<C, R>>> curry(TriFunction<A, B, C, R> func) {
        return (a) -> (b) -> (c) -> func.apply(a, b, c);
    }

}
