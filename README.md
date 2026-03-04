[![Java CI](https://github.com/kai-niemi/pool-configurer/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/kai-niemi/pooltool/actions/workflows/maven.yml)

<!-- TOC -->
* [About](#about)
  * [Compatibility](#compatibility)
* [Building and Running](#building-and-running)
  * [Install the JDK](#install-the-jdk)
  * [Clone the project](#clone-the-project)
  * [Build the artifact](#build-the-artifact)
  * [Running](#running)
* [Terms of Use](#terms-of-use)
<!-- TOC -->

# About

<img  align="left" src="logo.png" alt="" width="32"/> 

A client-side Hikari connection pool configuration and testing tool for CockroachDB.

Key Features:

- Editor for configuring optimal pool properties using profiles
- Sample SQL workloads to test pool settings
- Visualize pool and workload metrics

Screenshot:

![demo.png](demo.png)

## Compatibility

- JDK21+
- MacOS / Linux
- CockroachDB

# Building and Running

## Install the JDK

MacOS (using sdkman):

    curl -s "https://get.sdkman.io" | bash
    sdk list java
    sdk install java 21.0 (pick version)  

Ubuntu:

    sudo apt-get install openjdk-21-jdk

## Clone the project

    git clone git@github.com:kai-niemi/pool-configurer && cd pool-configurer

## Build the artifact

    chmod +x mvnw
    ./mvnw clean install

## Running

    java -jar target/pc.jar <args>

# Terms of Use

This tool is not supported by Cockroach Labs. Use of this tool is entirely at your
own risk and Cockroach Labs makes no guarantees or warranties about its operation.

See [MIT](LICENSE.txt) for terms and conditions.
