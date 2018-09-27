TARGET=MeCab
JAVAC=javac
JAVA=java
JAR=jar
CXX=c++
INCLUDE=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.181-8.b13.39.39.amzn1.x86_64/include/

PACKAGE=org/chasen/mecab
HOME=/home/hadoop
DIR=$(HOME)/spark-hive-udf-mecab

MKBCONFIG=$(DIR)/mecab/bin/mecab-config 
LIBS=`$(MKBCONFIG) --libs`
INC=`$(MKBCONFIG) --cflags` -I$(INCLUDE) -I$(INCLUDE)/linux 

all:
	$(CXX) -O3 -c -fpic $(TARGET)_wrap.cxx  $(INC)
	$(CXX) -shared  $(TARGET)_wrap.o -o lib$(TARGET).so $(LIBS)
	$(JAVAC) $(PACKAGE)/*.java
	$(JAVAC) test.java
	$(JAR) cfv $(TARGET).jar $(PACKAGE)/*.class

test:
	env LD_LIBRARY_PATH=. $(JAVA) test

clean:
	rm -fr *.jar *.o *.so *.class $(PACKAGE)/*.class
	
cleanall:
	rm -fr $(TARGET).java *.cxx
