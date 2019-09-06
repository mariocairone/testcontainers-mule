# TestContainers Mule Module

## Docker Image

Mulesoft do not provide an official docker images for the mule runtime.

The module will use by default the docker image [mariocairone/mule-ee:latest](https://cloud.docker.com/repository/docker/mariocairone/mule-ee).

## Install the Module

To install the module in your local maven repository runt the following command

```shell
mvn clean install
```

## Usage Example

The example below shows how to use the module to create a mule container with a deployed application in a Junit test class.

```java

package com.mariocairone.testcontainers.mule;

import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.wait.strategy.Wait;


public class MuleServerContainerTest {


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
		mule.start();
		mulePort = mule.getFirstMappedPort();
	}

	// Add your test
	 @Test
	 public void test()  {

	 }
}

```
