JC = javac
JFLAGS = -cp external-libraries/bcrypt.jar:.
.SUFFIXES: .java .class

.java.class:
	$(JC)	$(JFLAGS)	$*.java

CLASSES = \
    Authentication.java \
    Client.java \
    ClientConnection.java \
    Game.java \
	Server.java \
	\

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
