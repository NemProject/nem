FROM ubuntu:22.04

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
  && mvn clean package install -DskipTests=true \
  && bash package.prepare.sh package

# move package
RUN mv /build/nem/package /app

WORKDIR /app

CMD ["java", "-Xms6G", "-Xmx6G", "-cp", "/usersettings:./nis/*:./libs/*", "org.nem.deploy.CommonStarter"]
