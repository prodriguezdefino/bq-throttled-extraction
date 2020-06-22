#!/bin/bash

docker build --target uber-jar -t bq-extract-propagator:uber-jar .

docker build --target native-image -t bq-extract-propagator:native-image .

docker tag bq-extract-propagator:uber-jar bq-extract-propagator:latest
