# BigQuery Throttled Extractor

This project is intended as a PoC for extracting BigQuery stored data, specifically the results of an arbitrary query, to an arbitrary location introducing a throttle mechanism to do not overload the destination. The idea is to be a base line implementation for a more complete solution, it provides a simple implementation for propagating the extracted data and pretty naive throttling mechanism, in both cases more complete implementations can be plugged in by implementing a Function with the correct signature in the case of the propagation function and by implementing the `Throttler` interface present as part of the defined `Model` class. 

This PoC receives as parameters the following required entries: 
``` 
usage: bq-export-propagator
 -b,--bucket <arg>        Cloud Storage Bucket to store temporal export
                          data
 -d,--dataset <arg>       BigQuery Dataset to store temporal export data
 -p,--pathprefix <arg>    String path where the export is going to be
                          stored, current datetime will be added to the
                          path
 -q,--query <arg>         The query that will be exported and propagated
 -t,--tableprefix <arg>   BigQuery Table prefix name to store temporal
                          export data, current datetime will be added to
                          the name
```

### Notes on the JDK version

This project makes use of the preview features of JDK 14 like pattern matching for `instanceof` and `records`, since the build script encapsulates those details this can be ommited in most cases; when needing compliance with a lower version of the JDK the changes can be easily introduced by changing the `record` definitions to static inner classes.

## Extraction Procedure

The launcher will start the procedure by triggering the provided query on the specified GCP project BigQuery instance, the results of that query will be stored as a temporal table in the provided Dataset. 

Once that query finishes the Launcher will trigger the export of the temporal table into the specified GCS location, the process will hint BigQuery the desired max file size by sending multiple URI destinations, this to distribute the data in multiple files minimizing potential retries in case of failures. 

After the GCS export completes the process will start reading the stored GCS files one by one and accumulate the results (expected as JSON objects) until a max size it's reached, when that happens the `Accumulator` object will interact with the configured `Throttler` instance before propagating any data to the destination. The propagation is made using the configured Function on the `Launcher::propagateExportResults` method. 

As the data is being propagated to the destination the process will be reporting in the logs the progress. Once all the files has been read and propagated the process will print out in the logs those propagation requests that may have failed during the execution.

### Note 
The current implementation does not handle retries or backoff policies while propagating results, a production level implementation should extend the provided dummy implementation introducing those features (exponential backoff, retries and error tracking).

## Prerequisites 

### Permissions

The Launcher needs a service account crendentials file available in the environment variable `GOOGLE_APPLICATION_CREDENTIALS`, the launcher scripts (normal shell and container based) already include the placeholder to indicate the path from where the file can be found in the local environment. Needed permissions: 
* Storage Object Creator
* BigQuery Data Editor applied to the temporary Dataset
* BigQuery Data Viewer applied to the source Dataset (where the query reads data from)

### Infrastructure

This project includes a Terraform script, under the [tf](/tf) directory, to recreate and cleanup the needed resources. To enumerate them: 
* a couple of BigQuery Datasets 
    * one to store the source data tables (not necessary if that already exists)
    * another to store the temporal tables to store the query results, this dataset has a pretty aggresive expiration policy (to ease the cleanup after exports)
* a bucket to temporarily store the BigQuery query results
* a launcher GCE instance, which as startup script install `docker` so it can be used to kickoff the build, this instance creation is controlled by th terraform variable `create_launcher_instance`.

### Build

The minimum requirements to build and launch this extraction process is to have a local Docker installation. The included `build.sh` script will take care of construction of the docker image and the `launcher_contrainer.sh` script can be used to run it. 

For development purposes JDK 14 and a Maven installation are required for the Java code, Terraform is also required to recreate the needed infrastructure pieces. 

## Configuration Knobs 

The Launcher script enables the configuration of most destination and temporal resources (GCP project, BigQuery resources and temporal bucket ones), but added to this there are a few internal tweaks that can be done by changing some magic numbers (Java constants) in the source code: 
* `Launcher.DESIRED_FILE_SIZE` the process uses this number while trying to hint BQ the desired file sizes when exporting the data to GCS. When the query results are small enough all data will be exported to a single file regardless this number.
* `Launcher.MILLIS_PER_PROPAGATION_EVENT` the amount of milliseconds between propagations, this is used to instantiate the `Throttler` object in use by the `Accumulator` object.
* `Launcher.GCS_RESULT_PAGE_SIZE` the number of elements present in the GCS list request, basically the page size. 

## Extending/Changing current functionality

The included default implementation for the propagation function can be described as (review the `org.example.bqexportpropagator.Functions::dummyPropagate` method): 
* captures the accumulated entries coming from the a GCP bucket and the name reference to the in-process file, 
* builds a JSON object with the `records` property containing all the entries (assumed to be JSON objects themselves)
* store this JSON object in a file in the local filesystem (to generate some IO interaction). 

If an arbitary implementation were to call a remote API to propagate the results the new logic could be included in this section of the code: 
``` java 
try {
    // simulate some IO latency since there is no remote call being done
    Files.writeString(Files.createDirectories(Paths.get("temps")).resolve(processingDateString), payload);
    LOG.info(String.format("Propagated %d bytes from %s with message %s at %s", size, fileLocation, message.get(),
            processingDateString));
    success = true;
} catch (IOException ex) {
    LOG.log(Level.SEVERE, "Error storing file", ex);
}
```
The `payload` reference in this code holds the String JSON representation of the payload to be sent as part of a request. Also this implementation assumes the existence of a `temps` directory where the extracted files will be stored, the launcher scripts takes care of creating it before hand.

## Further Experiments

Completely unrelated with the scope of this PoC, there are a couple of ongoing experiments related with native GraalVM based images and tailored JRE images. This experiments are currently not successful and possibly will end up being abandoned since they require changes on the dependant libraries in order to work properly, but in the meantime worth trying some workarounds. 

### GraalVM Image

There are big chances that GraalVM may be the future of the Java runtimes, currently thought the way that most libraries and applications are written make them imcompatible with the ideas behind native images, e.g.: extensive use of reflection, unsafe calls, classpath scans for resources, etc. 

In order to circumvent this limitations GraalVM provide tools that help scanning this not desired uses of internal apis or reflective calls and also a way to configure the native image creation toolkit to be aware of their existence and introduce the necessary changes in the resulting binary. With that in mind is that this repo includes a folder named [native-image-configs](/native-image-configs) containing all the captured configurations by running our process in a GraalVM enabling the tracing agent, this generates most of the desired configs but sadly not all of them. 

As it is at this time, the repository includes the scripts needed to create a native image for GraalVM including the captured configurations by the tracing agent, plus some manually introduced by running the process on it binary (native) version. In short it does not work for now, will keep trying and researching different methods. 

### JLink Image



