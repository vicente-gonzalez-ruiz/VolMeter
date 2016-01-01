# Regla para compilar los fuentes Java
%.class: %.java
	javac $*.java

# Clases
BIN =
BIN += VolMeter.class

# Objetivo por defecto
all: $(BIN)

bin:	all

# Creaci'on del fichero .jar con todas las clases
jar:	bin
	jar cvfm VolMeter.jar meta-inf/manifest.mf -C . *.class

test:	jar
	arecord -f cd -t raw | java -jar VolMeter.jar

clean:
	rm -f *.class VolMeter.jar

