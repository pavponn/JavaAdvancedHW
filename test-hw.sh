
HW_NAME=arrayset
HW_MAIN_CLASS=ArraySet
TEST_MODE=NavigableSet

HW_PACKAGE_DIR=ru/ifmo/rain/ponomarev/$HW_NAME
HW_PACKAGE=ru.ifmo.rain.ponomarev.$HW_NAME
REPOSITORY=../java-advanced-2019
TEST_PACKAGE=info.kgeorgiy.java.advanced.$HW_NAME


echo "*****************************************"
echo "Compiling files"
echo "*****************************************"

javac -d build "src/${HW_PACKAGE_DIR}/"*.java

echo "*****************************************"
echo "Run Tests"
echo "*****************************************"

java -cp build -p "${REPOSITORY}/artifacts/:${REPOSITORY}/modules/:${REPOSITORY}/lib/" -m $TEST_PACKAGE $TEST_MODE $HW_PACKAGE.$HW_MAIN_CLASS


