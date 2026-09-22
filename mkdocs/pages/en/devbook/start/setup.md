---
title: Setup
---

# Setting Up a Development Environment

This page lists the dependencies required to run the tutorials in this documentation and explains how to run them.

Most tutorials use the Symbol SDK, which supports both the <NEM:> and <Symbol:> networks.
It is the recommended library for building NEM applications, so the steps below apply to every tutorial.

Select the language you prefer:

=== ":simple-python: Python"

    <table markdown class="setup">
    <tr markdown><td>Prerequisites</td><td markdown>[Python](https://www.python.org/downloads/) 3.10 or later</td></tr>
    <tr markdown><td>Installation</td><td markdown>
    Install the Symbol SDK version 3.3.3 with:
    ```bash
    pip install symbol-sdk-python --upgrade
    ```
    </td></tr>
    <tr markdown><td>Running the Sample Code</td><td markdown>
    Download a sample and run it with:
    ```bash
    python hello-world.py
    ```
    </td></tr></table>

    ??? warning "Troubleshooting"

        On some systems, installing the SDK may require additional system packages,
        because some Python dependencies are built from source.

        If installation fails with errors related to missing headers, libraries, or compiler tools,
        install the required **development packages** for your system and run the installation again.

        Common symptoms include errors mentioning `gcc` or `pysha3`.

        On Ubuntu and Debian, it is typically enough to install:

        ```bash
        sudo apt install python3-dev build-essential
        ```

        Then run the Symbol SDK installation again.

=== ":simple-javascript: JavaScript"

    <table markdown class="setup">
    <tr markdown><td>Prerequisites</td><td markdown>Any actively supported version of [Node.js](https://nodejs.org/)</td></tr>
    <tr markdown><td>Installation</td><td markdown>
    Create a project folder and install the Symbol SDK version 3.3.3 as a dependency:
    ```bash
    mkdir nem-dev && cd nem-dev
    npm init -y
    npm install symbol-sdk
    ```
    </td></tr>
    <tr markdown><td>Running the Sample Code</td><td markdown>
    Download a sample and run it with:
    ```bash
    node hello-world.mjs
    ```
    </td></tr></table>

=== ":fontawesome-brands-java: Java"

    <table markdown class="setup">
    <tr markdown><td>Prerequisites</td><td markdown>[JBang](https://www.jbang.dev/download/)

    The tutorials use JBang to simplify Java version and dependency management, but it is not a requirement
    for applications.</td></tr>
    <tr markdown><td>Installation</td><td markdown>
    The Java snippets use JBang comments to select a compatible Java version and load the Symbol SDK directly from Maven Central:
    ```java
    //JAVA 21+
    //DEPS org.symbol:symbol-sdk:3.3.3
    ```

    When you run a snippet, JBang downloads the Symbol SDK and its dependencies into its local cache.
    No `pom.xml`, `build.gradle`, or manual classpath setup is required.
    </td></tr>
    <tr markdown><td>Running the Sample Code</td><td markdown>
    Download a sample and run it with:
    ```bash
    jbang HelloWorld.java
    ```
    </td></tr></table>

    ??? note "Alternate SDK installation"

        If you are building a Java application instead of running standalone snippets, add the Symbol SDK to your
        project with your build tool.

        === "Gradle"

            ```kotlin
            repositories {
                mavenCentral()
            }

            dependencies {
                implementation("org.symbol:symbol-sdk:3.3.3")
            }
            ```

        === "Maven"

            ```xml
            <dependency>
                <groupId>org.symbol</groupId>
                <artifactId>symbol-sdk</artifactId>
                <version>3.3.3</version>
            </dependency>
            ```

        Use Java 21 or later.

    ??? warning "Troubleshooting"

        * If the `jbang` command is not found after installation, restart your terminal and try again.

        * Java snippets declare `//JAVA 21+`, so JBang will use a compatible JDK when available.

            If JBang cannot find or download one, install a Java 21 or later JDK and run the snippet again.

        * If the Symbol SDK dependency cannot be resolved, check your network connection, clear JBang's cache,
            and run the snippet again:

            ```bash
            jbang cache clear
            jbang HelloWorld.java
            ```

## Next Steps

* Proceed to [Hello World](./hello-world.md)

<style>
.md-typeset .tabbed-labels a {
    font-size: large;
}
table.setup {
    border-collapse:collapse;
}
table.setup td {
    border: 1px solid var(--md-default-bg-color--light);
    padding: 0.5rem;
}
.md-typeset table.setup td:first-child {
    white-space:nowrap;
}
.md-typeset table.setup td:last-child {
    width: 100%;
}
.md-typeset table.setup pre {
    margin-bottom: 0;
}
</style>
