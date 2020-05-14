# mdm-core
Core API for Mauro Data Mapper

| Branch | Version | Status |
| ------ | ------- | ------------ |
| master | 4.0.0-SNAPSHOT | [![Build Status](https://travis-ci.com/MauroDataMapper/mdm-core.svg?token=LJTgeDtbKGx14XtwJGoQ&branch=master)](https://travis-ci.com/MauroDataMapper/mdm-core) |
| develop | 4.0.0-SNAPSHOT | [![Build Status](https://travis-ci.com/MauroDataMapper/mdm-core.svg?token=LJTgeDtbKGx14XtwJGoQ&branch=develop)](https://travis-ci.com/MauroDataMapper/mdm-core) |

## Requirements

* Grails 4.0.3 +
* Gradle 6.3
* Java OpenJDK 12

All of the above can be installed an easily maintained by using [SDKMAN!](https://sdkman.io).

*Grails 4.0.3 will not run in any version above Java 12, and Grails 4 will not work on any version below Java 11* 

## Running

Each sub project is its own gradle or grails application and can be run and tested independantly.
Please see the Grails [documentation](http://docs.grails.org/latest/) for how to use the CLI and how to develop using Grails.

## Developing

### Working with the code

#### IDE Choice

Both Intellij and Eclipse plugins are installed however we recommend using Intellij.
We also provide all instructions using the gradle wrapper as that avoids worrying about which version of gradle you have installed.
However if you make use of SDKMan you can switch between versions quickly and easily using `sdk u gradle <version>`,
this will set the current terminal to that version.

The following will setup intellij or you can open the folder with Intellij and it will auto import the gradle project.

Please note the following:

**Use git flow for all repositories**

## Using Intellij

### Code Style 

To make life easier with the Static Code Analysis we highly recommend importing the `OxfordBRC_Intellij_CodeStyle.xml` file which is in the 
`gradle` folder in this repository.
This can be done by:

1. Intellij Preferences
1. Code Style
1. Click the 'gear' next to the Scheme
1. Import
1. Choose the XML file

This will provide a code style scheme for OxfordBRC work, and allow you to use the `Code` -> `Reformat Code` option to fully reformat
and optimise imports. 
These settings will meet the checkstyle enforced by the Metadata Catalogue code bases.

### Checkstyle

The following will highlight any checkstyle issues as you type.

1. Install the `Checkstyle-IDEA` plugin, available from `Browse Repositories` in the `Preferences` -> `Plugins`
1. `Preferences` -> `Other Settings` -> `Checkstyle`
1. Add the `gradle/checkstyle.xml` file, and activate it