FROM maven:3.6.3-openjdk-14-slim as build-uber-jar

WORKDIR /bq-extract-propagator
COPY pom.xml .
COPY src src
RUN mvn package 
RUN mv target/$(mvn help:evaluate -Dexpression=project.build.finalName -q -DforceStdout).jar target/bqextractpropagator.jar

FROM openjdk:14-jdk-alpine as uber-jar

COPY --from=build-uber-jar /bq-extract-propagator/target/bqextractpropagator.jar /bq-extract-propagator/bqextractpropagator.jar
ENTRYPOINT ["java", "--enable-preview","-jar", "/bq-extract-propagator/bqextractpropagator.jar"]

FROM oracle/graalvm-ce:19.3.2-java11 as native-builder

RUN gu install native-image

RUN mkdir -p /opt/maven
RUN curl -s https://apache.osuosl.org/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz | tar xvz --strip-components=1 -C /opt/maven

ENV PATH="/opt/maven/bin:${PATH}"

WORKDIR /bq-extract-propagator

COPY pom.xml .
COPY src src
RUN mvn package -Pnative-image

FROM oracle/graalvm-ce:19.3.2-java11 as native-image

COPY --from=native-builder /bq-extract-propagator/target/bqexportpropagator /bqexportpropagator
RUN chmod +x /bqexportpropagator

ENTRYPOINT ["/bqexportpropagator"]