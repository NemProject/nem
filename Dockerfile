FROM ubuntu:20.04

# install dependencies (install tzdata first to prevent 'geographic area' prompt)
RUN apt-get update \
  && apt-get install -y tzdata \
  && apt-get install -y openjdk-8-jdk-headless git libssl-dev maven ca-certificates \
  && update-ca-certificates \
  && rm -rf /var/lib/apt/lists/*

# add github to ssh known hosts
RUN mkdir -p ~/.ssh && ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts

# clone repositories into /build
RUN --mount=type=ssh mkdir -p /build \
  && cd /build \
  && git clone git@github.com:NemProject/nis-client.git \
  && cd nis-client \
  && git submodule update --init --recursive

# actually build
RUN cd /build/nis-client && mvn clean package -DskipTests=true

RUN mkdir -p /app

WORKDIR /app

#CMD ["/usr/bin/java", "-Xms6G", "-Xmx6G", "-cp", "/app/testnet/:package/nis/*:package/libs/*", "foo" ]
CMD ["echo", "foo bar"]
