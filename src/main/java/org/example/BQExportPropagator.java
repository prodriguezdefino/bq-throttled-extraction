package org.example;

import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.JobStatistics;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.example.Model.*;
import org.example.Functions.TriFunction;

public class BQExportPropagator {

    static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Logger LOG = Logger.getLogger(BQExportPropagator.class.getCanonicalName());
    private static final Long DESIRED_FILE_SIZE = 1024L * 1024 * 10;
    private static final long MILLIS_PER_PROPAGATION_EVENT = 1000L;
    private static final int GCS_RESULT_PAGE_SIZE = 10;

    /**
     * Close to a MB of data.
     */
    private static final Long ACCUMULATION_SIZE_LIMIT = 1024L * 900L;

    public static void main(String[] arguments) {
        try {
            var args = captureArguments(arguments);
            var gcsBucketPrefix = "gs://" + args.exportBucketName() + "/" + args.exportBucketPathPrefix();
            var exportURIs = createExportWildcardPaths(args.bqQuery(), args.bqQueryParams(), args.project(),
                    args.destinationDataset(), args.exportDestinationTable(), gcsBucketPrefix);
            moveDataToExportTable(args.bqQuery(), args.bqQueryParams(), args.project(), args.destinationDataset(),
                    args.exportDestinationTable());

            exportTableToGCS(args.project(), args.destinationDataset(), args.exportDestinationTable(),
                    gcsBucketPrefix, exportURIs, "NEWLINE_DELIMITED_JSON");
            propagateExportResults(args.exportBucketName(), args.exportBucketPathPrefix(), ACCUMULATION_SIZE_LIMIT,
                    MILLIS_PER_PROPAGATION_EVENT, Functions::processGCSBlob);
        } catch (InterruptedException ex) {
            LOG.log(Level.SEVERE, "Export procedure interrupted.", ex);
        } catch (BigQueryException bqex) {
            LOG.log(Level.SEVERE, "Errors occurred while interacting with BigQuery service.", bqex);
        } catch (ParseException ex) {
            LOG.info("Exiting");
            System.exit(1);
        }
    }

    private static Arguments captureArguments(String[] args) throws ParseException {
        var options = new Options();
        var required = true;

        var project = new Option("p", "project", true, "GCP Project");
        project.setRequired(required);
        options.addOption(project);

        var dataset = new Option("d", "dataset", true, "BigQuery Dataset to store temporal export data");
        dataset.setRequired(required);
        options.addOption(dataset);

        var table = new Option("t", "tableprefix", true,
                "BigQuery Table prefix name to store temporal export data, current datetime will be added to the name");
        table.setRequired(required);
        options.addOption(table);

        var bucket = new Option("b", "bucket", true, "Cloud Storage Bucket to store temporal export data");
        bucket.setRequired(required);
        options.addOption(bucket);

        var pathPrefix = new Option("p", "pathprefix", true,
                "String path where the export is going to be stored, current datetime will be added to the path");
        pathPrefix.setRequired(required);
        options.addOption(pathPrefix);

        var query = new Option("q", "query", true, "The query that will be exported and propagated");
        query.setRequired(required);
        options.addOption(query);

        var parser = new DefaultParser();
        var formatter = new HelpFormatter();
        Arguments capturedArguments = null;

        try {
            var cmd = parser.parse(options, args);
            capturedArguments = new Arguments(
                    cmd.getOptionValue("project"),
                    cmd.getOptionValue("dataset"),
                    cmd.getOptionValue("tableprefix") + "_" + DATE_FORMATTER.format(LocalDateTime.now()),
                    cmd.getOptionValue("bucket"),
                    cmd.getOptionValue("pathprefix") + "/" + DATE_FORMATTER.format(LocalDateTime.now()),
                    cmd.getOptionValue("query"),
                    Map.of());
        } catch (ParseException e) {
            formatter.printHelp("bq-export-propagator", options);
            throw e;
        }
        return capturedArguments;
    }

    /**
     * By running a BQ dry run builds a list of wildcard URIs that can be used as an export job target. This can be used
     * when the exports job needs to be chunked in files of an expected size (DESIRED_FILE_SIZE).
     *
     * @param queryString The query to be executed
     * @param project the project that host the BQ instance
     * @param destinationDataset the destination dataset
     * @param destinationTable the destination table
     * @param params the query parameters
     * @param locationPrefix the GCS location prefix where the wildcard URIs will be appended to
     * @return A list of Strings containing the wildcard URIs for the export job.
     *
     * @throws BigQueryException if there are issues while interacting with BigQuery service
     */
    private static List<String> createExportWildcardPaths(String queryString, Map<String, QueryParameterValue> params,
            String project, String destinationDataset, String destinationTable, String locationPrefix) {
        var bigquery = BigQueryOptions.getDefaultInstance().getService();

        var queryConfigBuilder = buildQueryConfig(queryString, project, destinationDataset, destinationTable, params);

        // now we need to extract the job size in order to define how many export file we will need
        var queryConfig = queryConfigBuilder
                .setDryRun(Boolean.TRUE)
                .build();

        var dryRunJob = bigquery.create(JobInfo.of(queryConfig));

        // the number of export URIs that will be sent to the BQ export request
        Long numberOfWildcardURIs = 1L;

        if (dryRunJob.getStatistics() instanceof JobStatistics.QueryStatistics stats) {
            numberOfWildcardURIs = stats.getTotalBytesProcessed() / DESIRED_FILE_SIZE;
            numberOfWildcardURIs = numberOfWildcardURIs == 0L ? 1L : numberOfWildcardURIs;
        }

        // create the wildcard URIs for the export
        return IntStream.rangeClosed(1, numberOfWildcardURIs.intValue())
                .mapToObj(i -> String.format("%s/export/%d/*-file", locationPrefix, i))
                .collect(Collectors.toList());
    }

    /**
     * Moves the results of the executed query to an specific project.dataset.table location in BQ.
     *
     * @param queryString The query to be executed
     * @param project the project that host the BQ instance
     * @param destinationDataset the destination dataset
     * @param destinationTable the destination table
     * @param params the query parameters
     * @throws InterruptedException
     * @throws BigQueryException if there are issues while interacting with BigQuery service
     */
    private static void moveDataToExportTable(String queryString, Map<String, QueryParameterValue> params,
            String project, String destinationDataset, String destinationTable) throws InterruptedException {

        var bigquery = BigQueryOptions.getDefaultInstance().getService();
        var queryConfigBuilder = buildQueryConfig(queryString, project, destinationDataset, destinationTable, params);

        // Run the query to move data to the temporary table and wait for completion
        var queryConfig = queryConfigBuilder.build();

        LOG.info(String.format("Moving data to table %s.%s.%s", project, destinationDataset, destinationTable));

        var moveDataJob = bigquery.create(JobInfo.of(queryConfig));
        var completedMoveDataJob = Optional.ofNullable(moveDataJob.waitFor());

        var statusMessage = completedMoveDataJob
                .flatMap(j -> Optional.ofNullable(j.getStatus().getError()))
                .map(bqError -> bqError.getMessage())
                .orElse(String.format("Data moved successfully to destination table %s.%s",
                        destinationDataset, destinationTable));

        LOG.info(statusMessage);
    }

    /**
     * Configuration builder helper method for BigQuery queries.
     *
     * @param queryString The query to be executed
     * @param project the project that host the BQ instance
     * @param destinationDataset the destination dataset
     * @param destinationTable the destination table
     * @param params the query parameters
     * @return a query job builder object.
     */
    private static QueryJobConfiguration.Builder buildQueryConfig(String queryString, String project,
            String destinationDataset, String destinationTable, Map<String, QueryParameterValue> params) {
        // partially construct the Query configuration
        var queryConfigBuilder = QueryJobConfiguration.newBuilder(queryString)
                // Save the results of the query to a permanent table.
                .setDestinationTable(TableId.of(project, destinationDataset, destinationTable));
        // add any existing parameter to the query
        params.entrySet().stream().forEach(paramEntry -> {
            queryConfigBuilder.addNamedParameter(paramEntry.getKey(), paramEntry.getValue());
        });
        return queryConfigBuilder;
    }

    /**
     * Exports a BigQuery to a specified GCS location.
     *
     * @param projectId project where table is hosted
     * @param datasetName dataset where the table exists
     * @param tableName table to be extracted
     * @param baseLocation GCS location prefix
     * @param destinationURIs GCS wilcard locations
     * @param formatName file format for the extract
     * @throws InterruptedException
     * @throws BigQueryException if there are issues while interacting with BigQuery service
     */
    private static void exportTableToGCS(String projectId, String datasetName, String tableName, String baseLocation,
            List<String> destinationURIs, String formatName) throws InterruptedException {
        // creates a representation of the table to be extracted
        var bigquery = BigQueryOptions.getDefaultInstance().getService();
        var tableId = TableId.of(projectId, datasetName, tableName);
        var table = bigquery.getTable(tableId);

        LOG.info(String.format("Exporting data from table %s.%s.%s to gcs location %s", projectId, datasetName, tableName,
                baseLocation));

        // request the extraction execution
        var job = table.extract(formatName, destinationURIs);

        // Blocks until this job completes its execution, either failing or succeeding.
        var operationMessage = Optional.ofNullable(job.waitFor())
                .flatMap(j -> Optional.ofNullable(j.getStatus().getError()))
                .map(bqError -> bqError.getMessage())
                .orElse(String.format("Data extracted from table %s.%s.%s successfully to destinations at %s",
                        projectId, datasetName, tableName, baseLocation));

        LOG.info(operationMessage);
    }

    /**
     * Propagates the exported data to GCS using the provided propagation function.
     *
     * @param bucketName Bucket name of where the export is located
     * @param gcsPrefixLocation path prefix for the exported files
     * @param blobProcessorFunction A propagation function.
     */
    private static void propagateExportResults(String bucketName, String gcsPrefixLocation, Long accumulationMaxSize,
            Long millisPerPropagation, TriFunction<Long, Long, Blob, Stream<PropagationResult>> blobProcessorFunction) {

        var storage = StorageOptions.getDefaultInstance().getService();
        var bucketClient = storage.get(bucketName);
        var prefix = gcsPrefixLocation.endsWith("/") ? gcsPrefixLocation : gcsPrefixLocation + "/";

        // capture the entries in GCS based on the path prefix and stream them considering only files (skip dirs), 
        // process those files that were exported and print the propagation results.
        StreamSupport
                .stream(bucketClient
                        .list(Storage.BlobListOption.prefix(prefix),
                                Storage.BlobListOption.pageSize(GCS_RESULT_PAGE_SIZE))
                        .iterateAll()
                        .spliterator(), false)
                .filter(blob -> !blob.isDirectory())
                // Each file may be bigger than the desired propagation chunk size, 
                // then this function will return multiple propagation results per file.
                // Lets partially apply the blob processor function with the params available before the stream 
                // resolves its content. 
                .flatMap(Functions.curry(blobProcessorFunction)
                        .apply(accumulationMaxSize)
                        .apply(millisPerPropagation))
                .filter(pResult -> !pResult.messageResult().isEmpty())
                .forEach(result -> {
                    LOG.info(String.format("Propagated %dKb from %s with message %s at %s", result.payloadSizeInKB(),
                            result.path(), result.messageResult().orElse("No Message."), result.executionDateString()));
                });
    }

}
