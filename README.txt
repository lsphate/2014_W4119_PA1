A. Brief description

	My program contains three classes, first one is called User.class, which define class named User, for storing users’ usernames, passwords, IPs, and other bunch of things. Second is called Server.class, which contains classes that do works like read the user_pass.txt, open a socket to accept login requirements, and handle each connected client with a single thread and socket. The last is called Client.class, which responses for sending and receiving messages via server.

B. Details on development environment

	It is hard to install JAVA 1.6 on Mac OSX 10.9 Maverick since Apple has it’s own JAVA version update control, so I use JAVA 1.7.0_67 to build my program. I’ve worked very carefully to avoid any API that has not been involved in JDK 1.6, but available in 1.7. And I’ve also test my code on a 1.6 machine as well.
	I used Netbeans 8.0.1 as my IDE, Mac/ubuntu Terminal and Windows CMD for run and test the JAVA program.

C. Instructions on how to run your code

	Just simply type make, and the .class file should be placed in ./classes directory. The runnable .class file is Server.class and Client.class
	Use “java Server <server_port>” to invoke server application.
	Use “java Client <server_IP> <server_port> to invoke client application.
	Any lack of arguments will receive error.