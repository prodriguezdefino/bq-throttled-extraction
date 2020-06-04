# BigQuery Throttled Extractor

This project is intended as a PoC for extracting BigQuery stored data, specifically the results of an arbitrary query, to an arbitrary location introducing a throttle mechanism to do not overload the destination. The idea is to be a base line implementation for a more complete solution, it provides a simple implementation for propagating the extracted data and pretty naive throttling mechanism, in both cases more complete implementations can be plugged in by implementing a Function with the correct signature in the case of the propagation function and by implementing the `Throttler` interface present as part of the defined `Model` class. 

This PoC receives as parameters the following entries: 
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

The launcher will trigger the provided query on the required GCP project BigQuery instance...

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