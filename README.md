# TestContainers Mule Module
![Maven Central](https://img.shields.io/maven-central/v/com.mariocairone.mule/testcontainers-mule?style=flat-square) ![GitHub](https://img.shields.io/github/license/mariocairone/testcontainers-mule) ![GitHub issues](https://img.shields.io/github/issues/mariocairone/testcontainers-mule) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/mariocairone/testcontainers-mule?style=social)
## Docker Image

MuleSoft do not provide an official docker image for the Mule runtime.

The module will use the docker image [mariocairone/mule-ee:latest](https://cloud.docker.com/repository/docker/mariocairone/mule-ee) by default.

## Setup

As a dependency on your Maven project:

```xml
<dependency>
   <groupId>com.mariocairone.mule</groupId>
   <artifactId>testcontainers-mule</artifactId>
   <version>1.0.0</version>
</dependency>
```

You can also build the `.jar` file yourself, assuming you that have Maven and JDK 1.8+ installed:
```shell
mvn clean install
```

The resulting `.jar` file will be located in the `target/` folder.

You can also find `SNAPSHOT` builds of the latest and greatest changes on the master branch in the SonaType snapshots repository.

To add that snapshot repository to your Maven pom.xml use the following snippet:
```xml
<repositories>
    <repository>
        <id>oss-sonatype</id>
        <name>oss-sonatype</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

## Usage Example

The example below shows how to use the module to create a Mule container with a deployed application in a JUnit test class.

```java

package com.mariocairone.mule.testcontainers;

import org.junit.ClassRule;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.wait.strategy.Wait;


public class MuleServerContainerTest {

	@ClassRule
	public static final MuleServerContainer mule = new MuleServerContainer()
		.withDeployedApplications("target", "myApp*.zip")
		.withExposedPorts(8081)
		.withMuleArg("mule.env", "test")
		.withMuleLogFolder("target/logs")
		.waitingFor(Wait.forHttp("/")
			.forStatusCode(200);

	private static Integer mulePort;

	@BeforeClass
	public static void init() {
		mulePort = mule.getFirstMappedPort();
	}

	// Add your test
	 @Test
	 public void test()  {

	 }
}

```
