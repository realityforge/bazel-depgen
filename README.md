# bazel-depgen: Generate Bazel dependencies

[![Build Status](https://secure.travis-ci.org/realityforge/bazel-depgen.svg?branch=master)](http://travis-ci.org/realityforge/bazel-depgen)
[<img src="https://img.shields.io/maven-central/v/org.realityforge.bazel.depgen/bazel-depgen.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.realityforge.bazel-depgen%22%20a%3A%22revapi.diff%22)

## What is bazel-depgen?

This is a simple, self-contained, command line application that generates [bazel](https://bazel.build/)
dependencies transitively for maven artifacts.

### Getting Started

The tool is released to Maven Central and can be downloaded using normal dependency download mechanisms.
The Maven dependency is:

```xml
<dependency>
  <groupId>org.realityforge.bazel.depgen</groupId>
  <artifactId>bazel-depgen</artifactId>
  <version>0.01</version>
  <classification>all</classification>
</dependency>
```

# Contributing

The project was released as open source so others could benefit from the project. We are thankful for any
contributions from the community. A [Code of Conduct](CODE_OF_CONDUCT.md) has been put in place and
a [Contributing](CONTRIBUTING.md) document is under development.

# License

The project is licensed under [Apache License, Version 2.0](LICENSE).
