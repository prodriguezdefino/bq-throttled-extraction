FROM maven:3.6.3-openjdk-14-slim as build

WORKDIR /bq-extract-propagator
COPY pom.xml .
COPY src src
RUN mvn package 
RUN mv target/$(mvn help:evaluate -Dexpression=project.build.finalName -q -DforceStdout).jar target/bqextractpropagator.jar

FROM openjdk:14-jdk-alpine

COPY --from=build /bq-extract-propagator/target/bqextractpropagator.jar /bq-extract-propagator/bqextractpropagator.jar
ENTRYPOINT ["java", "--enable-preview","-jar", "/bq-extract-propagator/bqextractpropagator.jar"]