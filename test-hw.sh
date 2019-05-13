#!/usr/bin/env bash
HW_NAME=hello
HW_MAIN_CLASS=HelloUDPClient
TEST_MODE=client-i18n

HW_PACKAGE_DIR=ru/ifmo/rain/ponomarev/$HW_NAME
HW_PACKAGE=ru.ifmo.rain.ponomarev.$HW_NAME
REPOSITORY=../java-advanced-2019
TEST_PACKAGE=info.kgeorgiy.java.advanced.$HW_NAME
MODULE_PATH=${REPOSITORY}/artifacts/:${REPOSITORY}/modules/:${REPOSITORY}/lib/
OUTPUT_DIR=build/
COMPILE_CLASSPATH=${OUTPUT_DIR}:${REPOSITORY}/artifacts/${TEST_PACKAGE}.jar

echo "*****************************************"
echo "Compiling files"
echo "*****************************************"

javac -p ${MODULE_PATH} -d ${OUTPUT_DIR} src/${HW_PACKAGE_DIR}/*.java --add-modules ${TEST_PACKAGE}

echo "*****************************************"
echo "Running Tests"
echo "*****************************************"

java -cp ${COMPILE_CLASSPATH} -p ${MODULE_PATH} -m $TEST_PACKAGE $TEST_MODE $HW_PACKAGE.$HW_MAIN_CLASS $1


