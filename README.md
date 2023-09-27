# Lang Interpreter

The Java reference implementation of the Lang Interpreter

## Features

- All Lang standard language features
- Support for Java Native Modules and Native Functions

## Use as dependency

**Maven**
```xml
<dependency>
    <groupId>at.jddev0.lang</groupId>
    <artifactId>lang-interpreter</artifactId>
    <version>1.0.0-beta-03</version>
</dependency>
```
**Gradle**
```groovy
implementation 'at.jddev0.lang:lang-interpreter:1.0.0-beta-03'
```

### Lang Platform API
- You can add a dependency to the Lang Platform API for your platform (e.g. Desktop with swing library) too

## Build from Source

- Execute the following command
```bash
./gradlew publishToMavenLocal
```
- `mavenLocal()` must be in the `repositories` section of the `build.gradle` file of the project you'd like to use the locally compiled version of the lang interpreter.