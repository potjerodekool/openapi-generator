# openapi-generator
Code generation for Springboot based from open api

Add the plugin to the build section in your pom and specify were your openapi file is located
and in which package the configuration file (with io.swagger.v3.oas.models.OpenAPI bean) must be generated:

```
<build>
  <plugins>
    ...
    <plugin>
      <groupId>io.github.potjerodekool</groupId>
      <artifactId>open-api-maven-plugin</artifactId>
      <version>1.0-SNAPSHOT</version>
      <executions>
        <execution>
          <goals>
            <goal>generate</goal>
          </goals>
          <configuration>
            <openApiFile>openapi/spec.yaml</openApiFile>
            <configPackageName>com.github.potjerodekool.demo.config</configPackageName>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

The open api file should only contain things like info and servers.
The paths must be specified in one or more separate yaml files in the paths directory relative to the open api file specified in your pom.
For exmple:

/openapi/paths/com/github/potjerodekool/demo/api1.yml
/openapi/paths/com/github/potjerodekool/demo/api2.yml
/openapi/spec.yaml

The yaml files from the paths directory will be automaticly be picked up.
In the above example you get

Api1Api.java
Api1Delegate.java
Api1Impl.java
Api2Api.java
Api2Delegate.java
Api2Impl.java

in the com.github.potjerodekool.demo package.

The Api file is an interface and is implemented by the Impl file. The delegate is also an interface which should be implemented by the developer and must be annoted
with the Bean annotation from Spring so its gets injected in the Impl.

Schemas must be placed in yaml files in the schemas directory. Just like with paths the package names for the schemas code will be derived from the directory structure.
For each schema one or more java files are generated. For example if you have a schema 'Pet' with and Id and Name property and you specify the Id property readonly
then the Id property will only show up in the response model.
