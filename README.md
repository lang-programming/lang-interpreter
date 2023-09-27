# Lang Interpreter

The Java reference implementation of the Lang Interpreter

## Features

- All Lang standard language features
- Support for Java Native Modules and Native Functions

## Build from Source

- Execute the following command
```bash
./gradlew publishToMavenLocal
```
- `mavenLocal()` must be in the `repositories` section of the `build.gradle` file of the project you'd like to use the locally compiled version of the lang interpreter.