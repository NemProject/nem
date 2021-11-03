FROM ubuntu:20.04 as builder

# install dependencies (install tzdata first to prevent 'geographic area' prompt)
RUN apt-get update \
  && apt-get install -y tzdata \
  && apt-get install -y openjdk-8-jdk-headless git libssl-dev maven ca-certificates \
  && update-ca-certificates \
  && rm -rf /var/lib/apt/lists/*

# add github to ssh known hosts
# remove this once all repos become public
RUN mkdir -p -m 0600 ~/.ssh && ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts

# clone repositories into /build
RUN --mount=type=ssh mkdir -p /build \
  && cd /build \
  && git clone git@github.com:NemProject/nis-client.git \
  && cd nis-client \
  && git submodule update --init --recursive

# actually build
RUN cd /build/nis-client && mvn clean package -DskipTests=true

WORKDIR /build/nis-client/nis/target

## runner
FROM ubuntu:20.04

ENV NODE_CONFIG="./config.properties"
ENV DB_CONFIG="./db.properties"
ENV INITIAL_HEAP_SIZE=-Xms2G
ENV MAX_HEAP_SIZE=-Xmx4G

RUN apt-get update \
  && apt-get install -y tzdata \
  && apt-get install -y openjdk-8-jdk-headless libssl-dev maven ca-certificates \
  && update-ca-certificates \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /build/nis-client/nis/target/obfuscationLibs/ ./libs/
COPY --from=builder /build/nis-client/nis/target/*.jar ./
COPY ${NODE_CONFIG} ${DB_CONFIG} ./

CMD ["sh", "-c", "java ${INITIAL_HEAP_SIZE} ${MAX_HEAP_SIZE} -cp './libs/*:./*' org.nem.deploy.CommonStarter" ]