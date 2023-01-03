FROM ubuntu:22.04 AS builder

# install dependencies (install tzdata first to prevent 'geographic area' prompt)
RUN apt-get update \
  && apt-get install -y tzdata \
  && apt-get install -y openjdk-11-jdk-headless git libssl-dev maven ca-certificates \
  && update-ca-certificates \
  && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /build/nem

# copy local code into /build/nem
COPY . /build/nem

WORKDIR /build/nem

# actually build (using java 11)
RUN mvn clean package -DskipTests=true \
  && cd infra \
  && bash ./package.prepare.sh package

# second stage of multistage build
FROM ubuntu:22.04 AS runner

ENV MEMORY_MS=6G
ENV MEMORY_MX=6G

ARG UID=11001
ARG UNAME=nem

RUN useradd --uid $UID -ms /bin/bash $UNAME

#install jre 11 for runtime
RUN apt-get update \
  && apt-get install -y openjdk-11-jre-headless

VOLUME ["/usersettings"]

WORKDIR /app

COPY --from=builder /build/nem/infra/package /app

RUN chown -R ${UNAME} /app

USER $UNAME

CMD ["bash", "-c", "java -Xms${MEMORY_MS} -Xmx${MEMORY_MX} -cp /usersettings:./nis/*:./libs/* org.nem.deploy.CommonStarter"]
