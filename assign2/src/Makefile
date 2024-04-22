JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $*.java

CLASSES = \
	Game.java \
	Server.java \
	ClientConnection.java

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
