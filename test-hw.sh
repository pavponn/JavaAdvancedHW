
HW_NAME=implementor
HW_MAIN_CLASS=Implementor
TEST_MODE=jar-interface

HW_PACKAGE_DIR=ru/ifmo/rain/ponomarev/$HW_NAME
HW_PACKAGE=ru.ifmo.rain.ponomarev.$HW_NAME
REPOSITORY=../java-advanced-2019
TEST_PACKAGE=info.kgeorgiy.java.advanced.$HW_NAME
MODULE_PATH="${REPOSITORY}/artifacts/:${REPOSITORY}/modules/:${REPOSITORY}/lib/"

echo "*****************************************"
echo "Compiling files"
echo "*****************************************"

javac -p "${MODULE_PATH}" -d build "src/${HW_PACKAGE_DIR}/"*.java --add-modules info.kgeorgiy.java.advanced.implementor

echo "*****************************************"
echo "Run Tests"
echo "*****************************************"

java -cp build -p "${MODULE_PATH}" -m $TEST_PACKAGE $TEST_MODE $HW_PACKAGE.$HW_MAIN_CLASS


