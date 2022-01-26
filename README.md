# Mauro Data Mapper Core Backend Application

Core API for Mauro Data Mapper

| Branch | Build Status |
| ------ | ------------ |
| main | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper%2Fmdm-core%2Fmain)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%2Fmdm-core/branches) |
| develop | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper%2Fmdm-core%2Fdevelop)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%2Fmdm-core/branches) |

## Requirements

* Grails 5.1.1+
* Gradle 7.3.3
* Java 17

All of the above can be installed an easily maintained by using [SDKMAN!](https://sdkman.io).

* PostgreSQL 12+

This is required to run any of the sub projects in development or production mode. 
In test mode they will use an in-memory H2 database.

## Running

Each sub project is its own gradle or grails application and can be run and tested independantly.
Please see the Grails [documentation](http://docs.grails.org/latest/) for how to use the CLI and how to develop using Grails.

Ensure the following database setup has been run
```bash
psql -U postgres
-- Create the development user
postgres=# CREATE USER maurodatamapper WITH SUPERUSER PASSWORD 'MauroDataMapper1234';
-- Create the database
postgres=# CREATE DATABASE maurodatamapper OWNER maurodatamapper;
```

### Standalone for backend testing

This will bring up all the current plugins/modules available to the MDM core. 

```bash
cd mdm-testing-functional
grails run-app
```

### Development Mode

Any sub-project which provides domains must extend `uk.ac.ox.softeng.maurodatamapper.core.gorm.mapping.PluginSchemaHibernateMappingContext`
this class will provide the name of the schema to be used for those domains.

Use the Grails CLI to start the sub-project

```shell script
grails> run-app

# debug mode
grails> run-app --debug-jvm
```

## Migrating from Metadata Catalogue

Please see [MC to MDM Migration](https://github.com/MauroDataMapper/mc-to-mdm-migration#mc-to-mdm-migration) repository.

## Developing

### Working with the code

Please note the following:

**Use git flow for all repositories**

## Testing

All testing should be done using the Grails CLI.
Navigate into the desired sub-project and enter the CLI and use one of the below commands.

```shell script
# To run the unit tests only
grails> test-app -unit

# To run the integration tests (all tests in src/integration-test
grails> test-app -integration

# To run just the integration tests
grails> -Dgradle.integrationTest=true test-app -integration

# To run just the functional tests
grails> -Dgradle.functionalTest=true test-app -integration
# or
grails> test-app *FunctionalSpec -integration

# Add --debug-jvm to start a debugger hook
```

You can also run all the tests in the same format that Jenkins runs them by using the shell script as below. This will run all the tests in order and
generate a complete test report.

```shell
# Run the tests
$ ./run-all-tests.sh

# Open the complete test report
$ open build/reports/tests/index.html 
```

All pushes and Pull Requests to the MauroDataMapper repo will produce a build inside our Jenkins system which will run all of the above tests.

Please note the Grails default setup is to place both integration tests and functional tests into the same directory and both use the same annotation
`grails.testing.mixin.integration.Integration` hence the reason we use system properties to allow running the tests separately.

## IDE Choice

Both Intellij and Eclipse plugins are installed however we recommend using Intellij.
We also provide all instructions using the gradle wrapper as that avoids worrying about which version of gradle you have installed.
However if you make use of SDKMan you can switch between versions quickly and easily using `sdk u gradle <version>`,
this will set the current terminal to that version.

### Using Intellij

Open the folder with Intellij and it will auto import the gradle project.

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
These settings will meet the checkstyle enforced by the Mauro Data Mapper code bases.

#### Checkstyle

The following will highlight any checkstyle issues as you type.

1. Install the `Checkstyle-IDEA` plugin, available from `Browse Repositories` in the `Preferences` -> `Plugins`
1. `Preferences` -> `Other Settings` -> `Checkstyle`
1. Add the `gradle/checkstyle.xml` file, and activate it