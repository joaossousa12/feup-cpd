JC = javac
.SUFFIXES: .java .class

.java.class:
	$(JC)	$*.java

CLASSES = \
    Authentication.java \
    Client.java \
    Game.java \
	Server.java \
	\

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
