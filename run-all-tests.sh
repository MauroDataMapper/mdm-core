#!/bin/zsh

# This executes all the commands Jenkinsfile executes in the same order and format that Jenkins does
# The hope is that we can repeat the tests locally and get the same instability
echo ">> Info <<"
./gradlew -v
./gradlew jvmArgs
./gradlew sysProps
pushd mdm-core
./grailsw -v
popd
echo ">> Clean <<"
./gradlew jenkinsClean
echo ">> Compile <<"
./gradlew --build-cache compile
echo ">> License Check <<"
./gradlew --build-cache license
echo ">> Unit Tests <<"
./gradlew :mdm-common:test
pushd mdm-core
./grailsw test-app -unit
popd
pushd mdm-plugin-email-proxy
./grailsw test-app -unit
popd
pushd mdm-plugin-datamodel
./grailsw test-app -unit
popd
pushd mdm-plugin-terminology
./grailsw test-app -unit
popd
pushd mdm-security
./grailsw test-app -unit
popd
pushd mdm-plugin-authentication-basic
./grailsw test-app -unit
popd
pushd mdm-plugin-dataflow
./grailsw test-app -unit
popd
pushd mdm-plugin-referencedata
./grailsw test-app -unit
popd
echo ">> Integration Tests <<"
pushd mdm-core
./grailsw -Dgrails.integrationTest=true test-app -integration
popd
pushd mdm-plugin-email-proxy
./grailsw -Dgrails.integrationTest=true test-app -integration
popd
pushd mdm-plugin-datamodel
./grailsw -Dgrails.integrationTest=true test-app -integration
popd
pushd mdm-plugin-terminology
./grailsw -Dgrails.integrationTest=true test-app -integration
popd
pushd mdm-security
./grailsw -Dgrails.integrationTest=true test-app -integration
popd
pushd mdm-plugin-dataflow
./grailsw -Dgrails.integrationTest=true test-app -integration
popd
pushd mdm-plugin-referencedata
./grailsw -Dgrails.integrationTest=true test-app -integration
popd
echo ">> Functional Tests <<"
pushd mdm-core
./grailsw -Dgrails.functionalTest=true test-app -integration
popd
pushd mdm-security
./grailsw -Dgrails.functionalTest=true test-app -integration
popd
pushd mdm-plugin-datamodel
./grailsw -Dgrails.functionalTest=true test-app -integration
popd
pushd mdm-plugin-terminology
./grailsw -Dgrails.functionalTest=true test-app -integration
popd
pushd mdm-plugin-authentication-basic
./grailsw -Dgrails.functionalTest=true test-app -integration
popd
pushd mdm-plugin-dataflow
./grailsw -Dgrails.functionalTest=true test-app -integration
popd
pushd mdm-plugin-referencedata
./grailsw -Dgrails.functionalTest=true test-app -integration
popd
echo ">> E2E Tests <<"
pushd mdm-testing-functional
./grailsw -Dgrails.test.package=core test-app
./grailsw -Dgrails.test.package=security test-app
./grailsw -Dgrails.test.package=authentication test-app
./grailsw -Dgrails.test.package=datamodel test-app
./grailsw -Dgrails.test.package=terminology test-app
./grailsw -Dgrails.test.package=dataflow test-app
./grailsw -Dgrails.test.package=referencedata test-app
popd
echo ">> Root Test Report <<"
./gradlew --build-cache rootTestReport