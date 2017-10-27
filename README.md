# dropwizard-jdbi3
[![Build Status](https://travis-ci.org/arteam/dropwizard-jdbi3.svg?branch=master)](https://travis-ci.org/arteam/dropwizard-jdbi3)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.arteam/dropwizard-jdbi3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.arteam/dropwizard-jdbi3/)

Dropwizard integration with JDBI3

# Description

This module glues together the Dropwizard framework and the `jdbi3` library. It provides a `JdbiFactory` which allows
to create a `Jdbi` object from the Dropwizard's `Environment` and `PooledDataSourceFactory`. It registers metrics 
for SQL queries and a health check for the connection.

# Use

```xml
<dependency>
  <groupId>com.github.arteam</groupId>
  <artifactId>dropwizard-jdbi3</artifactId>
  <version>1.2</version>
</dependency>
```
