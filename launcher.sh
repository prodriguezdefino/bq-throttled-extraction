#!/bin/bash

mkdir -p temps 

GOOGLE_APPLICATION_CREDENTIALS=<SOME_GOOGLE_CREDENTIALS_FILE> java --enable-preview -jar target/bq-export-propagator-0.0.1-SNAPSHOT-bundled.jar \
  --project=<GCP_PROJECT> \
  --dataset=<BQ_DATASET_FOR_TEMP_TABLE> \
  --tableprefix=<BQ_TEMP_TABLE_PREFIX> \
  --bucket=<BUCKET_FOR_TEMPORAL_EXPORT> \
  --pathprefix=<BUCKET_PATH_PREFIX> \
  --query="SOME SQL QUERY TO EXTRACT DATA"

