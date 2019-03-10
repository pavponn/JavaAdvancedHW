
REPOSITORY=../java-advanced-2019
MODULE_PATH="${REPOSITORY}/artifacts/:${REPOSITORY}/modules/:${REPOSITORY}/lib/"
ADD_PATH="${REPOSITORY}/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor/"
SRC_PATH="src/"

javadoc -d doc -link https://docs.oracle.com/en/java/javase/11/docs/api -cp ${MODULE_PATH} -private -html4 -sourcepath ${SRC_PATH} ru.ifmo.rain.ponomarev.implementor ${ADD_PATH}Impler.java ${ADD_PATH}JarImpler.java ${ADD_PATH}ImplerException.java
