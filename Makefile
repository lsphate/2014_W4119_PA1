target:
	mkdir -p classes
	javac ./src/User.java -d ./classes
	javac ./src/Server.java -d ./classes -classpath ./classes
	javac ./src/Client.java -d ./classes -classpath ./classes
