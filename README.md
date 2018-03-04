reproducible-build-maven-plugin
===============================

[![Build Status](https://travis-ci.org/Zlika/reproducible-build-maven-plugin.svg?branch=master)](https://travis-ci.org/Zlika/reproducible-build-maven-plugin)
[![Coverage Status](https://coveralls.io/repos/Zlika/reproducible-build-maven-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/Zlika/reproducible-build-maven-plugin?branch=master)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/4950cc475731475b93c8389b9ec4fa21)](https://www.codacy.com/app/Zlika/reproducible-build-maven-plugin?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Zlika/reproducible-build-maven-plugin&amp;utm_campaign=Badge_Grade)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.zlika/reproducible-build-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.zlika/reproducible-build-maven-plugin)

A Maven plugin to make your build byte-for-byte reproducible.
Cf. http://zlika.github.io/reproducible-build-maven-plugin/

### Requirements

* Java 8
* Maven 3.0.0 or newer

### How to compile

To compile the project and run its integration tests:

```
mvn clean install -Prun-its
```
