PWD=$(pwd)

TASK=info.kgeorgiy.java.advanced.implementor
REPOSITORY=../java-advanced-2019
TASK_RELPATH=info/kgeorgiy/java/advanced/implementor
MODULE_PATH="${REPOSITORY}/artifacts/:${REPOSITORY}/modules/:${REPOSITORY}/lib/"
DEP_PATH=${REPOSITORY}/modules/${TASK}/${TASK_RELPATH}/

PROJECT=JavaAdvancedHW
PACKAGE=ru.ifmo.rain.ponomarev.implementor
PROJECT_RELPATH=ru/ifmo/rain/ponomarev/implementor
OUTPUT_PATH=${PWD}/out/production/${PROJECT}/
SOURCES=${PWD}/src/${PROJECT_RELPATH}
OBJECTS=${OUTPUT_PATH}/${PROJECT_RELPATH}

MANIFEST=${PWD}/MANIFEST.MF
JAR_FILE=${PWD}/Implementor.jar


echo "*****************************************"
echo "Compiling files for jar"
echo "*****************************************"

javac -d ${OUTPUT_PATH} -p ${REPOSITORY}/modules/${TASK} ${DEP_PATH}/ImplerException.java ${DEP_PATH}/*Impler.java
javac -d ${OUTPUT_PATH} -p ${MODULE_PATH} ${SOURCES}/*.java --add-modules ${TASK}


echo "*****************************************"
echo "Creating Jar"
echo "*****************************************"

pushd ${OUTPUT_PATH}
jar cmf ${MANIFEST} ${JAR_FILE} ${PROJECT_RELPATH}/*.class ${TASK_RELPATH}/*.class
popd


