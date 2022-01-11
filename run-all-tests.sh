#!/bin/zsh

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    -u|--unit-test)
    UNIT=true
    shift # past argument
    ;;
   -i|--integration-test)
    INTEGRATION=true
    shift # past argument
    ;;
    -f|--functional-test)
    FUNCTIONAL=true
    shift # past argument
    ;;
    -e|--e2e-test)
    E2E=true
    shift # past argument
    ;;
    -r|--build-report)
    E2E=true
    shift # past argument
    ;;
    -p|--plugin)
    PLUGIN="$2"
    shift # past argument
    shift # past value
    ;;
    -h|--help)
    USAGE=true
    shift # past argument
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters

USAGE="${USAGE:-false}"
UNIT="${UNIT:-false}"
INTEGRATION="${INTEGRATION:-false}"
FUNCTIONAL="${FUNCTIONAL:-false}"
E2E="${E2E:-false}"
REPORT="${REPORT:-false}"

if $UNIT || $INTEGRATION || $FUNCTIONAL || $E2E
then
  echo 'Restricted testing mode'
else
  echo 'Testing everything'
  UNIT=true
  INTEGRATION=true
  FUNCTIONAL=true
  E2E=true
fi

function unitTest(){
  echo ">> Unit Tests <<"
./gradlew --build-cache test
}

function integrationTest(){
  echo ">> Integration Tests <<"
./gradlew --build-cache -Dgradle.integrationTest=true \
  :mdm-core:integrationTest \
  :mdm-plugin-datamodel:integrationTest \
  :mdm-plugin-email-proxy:integrationTest \
  :mdm-plugin-dataflow:integrationTest \
  :mdm-plugin-profile:integrationTest
  #  :mdm-security:integrationTest \
#  :mdm-plugin-terminology:integrationTest \
#  :mdm-plugin-referencedata:integrationTest \

#  :mdm-plugin-federation:integrationTest \
#
}

function functionalTest(){
  echo ">> Functional Tests <<"
./gradlew --build-cache -Dgradle.functionalTest=true \
  :mdm-core:integrationTest \
  :mdm-plugin-datamodel:integrationTest \
  :mdm-plugin-dataflow:integrationTest \
  :mdm-plugin-profile:integrationTest
#  :mdm-security:integrationTest \
#  :mdm-plugin-authentication-apikey:integrationTest \
#  :mdm-plugin-authentication-basic:integrationTest \
#  :mdm-plugin-terminology:integrationTest \
 \
#  :mdm-plugin-referencedata:integrationTest \
#   \
#  :mdm-plugin-federation:integrationTest

}

function e2eTest(){
  echo ">> E2E Tests <<"
#  ./gradlew --build-cache -Dgradle.test.package=core :mdm-testing-functional:integrationTest
#  ./gradlew --build-cache -Dgradle.test.package=security :mdm-testing-functional:integrationTest
#  ./gradlew --build-cache -Dgradle.test.package=authentication :mdm-testing-functional:integrationTest
#  ./gradlew --build-cache -Dgradle.test.package=datamodel :mdm-testing-functional:integrationTest
#  ./gradlew --build-cache -Dgradle.test.package=terminology :mdm-testing-functional:integrationTest
#  ./gradlew --build-cache -Dgradle.test.package=dataflow :mdm-testing-functional:integrationTest
#  ./gradlew --build-cache -Dgradle.test.package=referencedata :mdm-testing-functional:integrationTest
#  ./gradlew --build-cache -Dgradle.test.package=federation :mdm-testing-functional:integrationTest
#  ./gradlew --build-cache -Dgradle.test.package=profile :mdm-testing-functional:integrationTest
}

function initialReport(){
  echo ">> Info <<"
  ./gradlew -v
  pushd mdm-core || exit
  ./gradlew -v
  popd || exit
}

function compile() {
  echo ">> Compile <<"
  ./gradlew --build-cache compile
  exit_on_error $?
  echo ">> License Check <<"
  ./gradlew --build-cache license
}

exit_on_error() {
    exit_code=$1
    last_command=${@:2}
    if [ $exit_code -ne 0 ]; then
        exit $exit_code
    fi
}

# enable !! command completion
#set -o history -o histexpand

########################################################################################################
# This executes all the commands Jenkinsfile executes in the same order and format that Jenkins does
# The hope is that we can repeat the tests locally and get the same instability


function usage(){
  echo
  echo "Usage: ./run-all-tests.sh [OPTIONS]"
  echo "
Build and test the mdm-core.
If no options provided then all tests will be run, if options are provided then only the tests stated by the options will be run.
If the tests run then a HTML report will be generated and will automatically open in your default browser.
"
  echo "
Options:
  -r, --report            Output the initial report which details the build environment.
  -u, --unit-test         Run the unit tests
  -i, --integration-test  Run the integration tests
  -f, --functional-test   Run the functional tests
  -e, --e2e-test          Run the end-to-end tests which are contained inside the MTF module
  -h, --help              This help
"
}

if $USAGE
then
  usage
else

  if $REPORT; then initialReport; fi;

  if $UNIT || $INTEGRATION || $FUNCTIONAL || $E2E
  then
    ./gradlew jenkinsClean
    compile
  fi


  if [ -n "$PLUGIN" ]
  then
    echo "Testing plugin $PLUGIN only"
    if $UNIT; then ./gradlew --build-cache -Dgradle.integrationTest=true ":${PLUGIN}:test"; fi;
    if $INTEGRATION; then ./gradlew --build-cache -Dgradle.integrationTest=true ":${PLUGIN}:integrationTest"; fi;
    if $FUNCTIONAL; then ./gradlew --build-cache -Dgradle.functionalTest=true ":${PLUGIN}:integrationTest"; fi;
  else
    if $UNIT; then unitTest; fi;
    if $INTEGRATION; then integrationTest; fi;
    if $FUNCTIONAL; then functionalTest; fi;
    if $E2E; then e2eTest; fi;
  fi

   if $UNIT || $INTEGRATION || $FUNCTIONAL || $E2E
      then
        echo ">> Root Test Report <<"
        ./gradlew --build-cache rootTestReport
      fi
fi
#./gradlew --build-cache jacocoTestReport
#./gradlew --build-cache staticCodeAnalysis