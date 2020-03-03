# Copyright Materialize, Inc. All rights reserved.

FROM alpine:3.11

RUN apk --update add openjdk8-jre

COPY target/tb-0.1-SNAPSHOT.jar /usr/local/tb.jar

CMD ["/usr/bin/java", "-jar", "/usr/local/tb.jar", "-h"]
