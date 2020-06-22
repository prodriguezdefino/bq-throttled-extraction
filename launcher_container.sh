#!/bin/bash

mkdir -p temps

docker run -it -e GOOGLE_APPLICATION_CREDENTIALS=/cred/credentials.json \
	-v <SOME_PATH_TO_CREDENTIALS>:/cred/credentials.json \
	-v <SOME_PATH_TO_TEMPS>:/temps:rw \
	bq-extract-propagator:latest \
    --project=<GCP_PROJECT> \
    --dataset=<BQ_DATASET_FOR_TEMP_TABLE> \
    --tableprefix=<BQ_TEMP_TABLE_PREFIX> \
    --bucket=<BUCKET_FOR_TEMPORAL_EXPORT> \
    --pathprefix=<BUCKET_PATH_PREFIX> \
    --query="SOME SQL QUERY TO EXTRACT DATA" \
    $@
