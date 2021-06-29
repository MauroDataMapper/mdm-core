#!/bin/zsh

# This executes all the commands Jenkinsfile executes in the same order and format that Jenkins does
# The hope is that we can repeat the tests locally and get the same instability
echo ">> Info <<"
./gradlew -v
./gradlew jvmArgs sysProps jenkinsClean
pushd mdm-core
./gradlew -v
popd
echo ">> Compile <<"
./gradlew --build-cache compile
echo ">> License Check <<"
./gradlew --build-cache license
echo ">> Unit Tests <<"
./gradlew --build-cache test
echo ">> Integration Tests <<"
./gradlew --build-cache -Dgradle.integrationTest=true \
  :mdm-core:integrationTest \
  :mdm-plugin-email-proxy:integrationTest \
  :mdm-plugin-datamodel:integrationTest \
  :mdm-plugin-terminology:integrationTest \
  :mdm-security:integrationTest \
  :mdm-plugin-dataflow:integrationTest \
  :mdm-plugin-referencedata:integrationTest \
  :mdm-plugin-federation:integrationTest
echo ">> Functional Tests <<"
./gradlew --build-cache -Dgradle.functionalTest=true \
  :mdm-core:integrationTest \
  :mdm-plugin-authentication-apikey:integrationTest \
  :mdm-plugin-authentication-basic:integrationTest \
  :mdm-plugin-dataflow:integrationTest \
  :mdm-plugin-datamodel:integrationTest \
  :mdm-plugin-referencedata:integrationTest \
  :mdm-plugin-terminology:integrationTest \
  :mdm-plugin-profile:integrationTest \
  :mdm-security:integrationTest \
  :mdm-plugin-federation:integrationTest
echo ">> E2E Tests <<"
./gradlew --build-cache -Dgradle.test.package=core :mdm-testing-functional:integrationTest
./gradlew --build-cache -Dgradle.test.package=security :mdm-testing-functional:integrationTest
./gradlew --build-cache -Dgradle.test.package=authentication :mdm-testing-functional:integrationTest
./gradlew --build-cache -Dgradle.test.package=datamodel :mdm-testing-functional:integrationTest
./gradlew --build-cache -Dgradle.test.package=terminology :mdm-testing-functional:integrationTest
./gradlew --build-cache -Dgradle.test.package=dataflow :mdm-testing-functional:integrationTest
./gradlew --build-cache -Dgradle.test.package=referencedata :mdm-testing-functional:integrationTest
./gradlew --build-cache -Dgradle.test.package=federation :mdm-testing-functional:integrationTest
echo ">> Root Test Report <<"
./gradlew --build-cache rootTestReport
#./gradlew --build-cache jacocoTestReport
#./gradlew --build-cache staticCodeAnalysis