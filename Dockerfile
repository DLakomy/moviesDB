FROM alpine:3.18.2 AS builder

RUN apk add bash openjdk11 curl

RUN set -o pipefail && curl -sL "https://github.com/sbt/sbt/releases/download/v1.9.0/sbt-1.9.0.tgz" | \
    tar xz -C /opt && \
    ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt

WORKDIR /build
COPY . .
RUN ["sbt", "assembly"]
RUN find . -name 'moviesdb*.jar' -exec cp {} ./moviesdb.jar \;

# the app is ready, final image
FROM alpine:3.18.2

RUN set -o pipefail && apk add --no-cache openjdk11 curl && \
    adduser -D moviesdb && mkdir /var/db && chown moviesdb:moviesdb /var/db

USER moviesdb

WORKDIR /app
COPY --from=builder /build/moviesdb.jar .

VOLUME [ "/var/db" ]
EXPOSE 8080

# ofc it would be better to actually check if the db connection works etc.
# I've used just this curl invocation for simplicity
HEALTHCHECK --interval=30s --timeout=30s --start-period=15s --retries=3 CMD [ "curl", "-sSf", "localhost:8080/docs/"]

ENV MOVIESDB_DBPATH /var/db/movies.db
ENV MOVIESDB_HOST 0.0.0.0
ENV MOVIESDB_PORT 8080

ENTRYPOINT [ "java", "-jar", "moviesdb.jar" ]
