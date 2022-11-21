FROM ubuntu:22.04 AS builder

# install dependencies (install tzdata first to prevent 'geographic area' prompt)
RUN apt-get update \
  && apt-get install -y tzdata \
  && apt-get install -y openjdk-8-jdk-headless git libssl-dev maven ca-certificates \
  && update-ca-certificates \
  && rm -rf /var/lib/apt/lists/*

# clone repositories into /build (using https)
RUN mkdir -p /build \
  && cd /build \
  && git clone https://github.com/NemProject/nem.git \
  && cd nem

# actually build (using java 8)
RUN echo 2 | update-alternatives --config java \
  && cd /build/nem \
  && mvn clean install -DskipTests=true \
  && cd infra \
  && bash ./package.prepare.sh package


FROM ubuntu:22.04 AS runner

ARG UID=1000
ARG UNAME=nem

RUN useradd --uid $UID -ms /bin/bash $UNAME

#install jre 11 for run time
RUN apt-get update \
  && apt-get install -y openjdk-11-jre-headless

VOLUME ["/usersettings"]

WORKDIR /app

USER $UNAME

COPY --from=builder /build/nem/infra/package /app

CMD ["java", "-Xms6G", "-Xmx6G", "-cp", "/usersettings:./nis/*:./libs/*", "org.nem.deploy.CommonStarter"]
