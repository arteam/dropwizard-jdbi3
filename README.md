# dropwizard-jdbi3
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
  <version>1.0-rc3</version>
</dependency>
```
