# Campus Chat 💬

Campus Chat is a Java-based real-time messaging application built using TCP socket programming, multithreading, and Java Swing. It allows multiple users to connect to a central server, exchange messages, and view currently connected users through a graphical user interface.

The project demonstrates fundamental computer networking concepts, client-server communication, and concurrent programming.

## 🚀 Features

- **Real-Time Messaging:** Exchange messages between connected clients.
- **Client-Server Architecture:** A central server manages client connections and communication.
- **Multithreading:** Supports multiple connected clients concurrently.
- **Graphical User Interface:** Desktop interface built with Java Swing.
- **Live User List:** View connected users and the total user count.
- **JSON Communication:** Uses Gson to process structured messages.
- **Connection Management:** Connect to a server using its IP address, port, and username.

## 🛠️ Technologies Used

- Java 21
- TCP Sockets
- Multithreading
- Java Swing
- Gson
- Apache Maven

## 📁 Project Structure

```text
CampusChat/
├── src/
│   └── main/
│       └── java/
│           └── campus/
│               └── chat/
│                   ├── ChatClient.java
│                   └── ChatServer.java
├── .gitignore
├── pom.xml
└── README.md
```

*Note: The package directories and filenames above are illustrative. Adjust them to match the actual source tree.*

## 📋 Prerequisites

Before running the application, install:

- Java Development Kit (JDK) 21 or compatible version
- Apache Maven
- Git (optional, for cloning the repository)

Verify your installations:

```bash
java -version
mvn -version
```

## ⚙️ Installation and Setup

### 1. Clone the Repository

```bash
git clone https://github.com/Siddharth-Tapkir/CampusChat.git
cd CampusChat
```

### 2. Compile the Project

```bash
mvn clean compile
```

### 3. Start the Server

Run the main class containing the server's `main()` method.

For example, if your Maven project uses the Exec Maven Plugin:

```bash
mvn exec:java -Dexec.mainClass="campus.chat.ChatServer"
```

### 4. Start the Client

Open another terminal and run the client main class:

```bash
mvn exec:java -Dexec.mainClass="campus.chat.ChatClient"
```

If the Exec Maven Plugin is not configured in `pom.xml`, use your IDE or the appropriate Java command to run the main classes.

## 💻 How to Use

1. Start the chat server.
2. Launch the client application.
3. Enter the server IP address, port, and username.
4. Connect to the server.
5. Send messages and communicate with other connected users.
6. View the list of currently connected users.

For clients running on the same machine, use `127.0.0.1` as the server address. For clients on different machines, use the server machine's reachable IP address and ensure the server port is accessible.

## 🎯 Learning Objectives

- Understand TCP socket programming in Java.
- Implement client-server communication.
- Use multithreading to handle concurrent client connections.
- Exchange structured messages using JSON.
- Build a desktop GUI using Java Swing.
- Apply networking concepts in a practical mini-project.

## 🔮 Future Enhancements

- Private messaging between users
- Message timestamps
- Chat rooms and group messaging
- Persistent chat history
- User authentication
- Improved GUI and notifications

## 👨‍💻 Author

**Siddharth Tapkir**

GitHub: [@Siddharth-Tapkir](https://github.com/Siddharth-Tapkir)

## 📄 License

This project is intended for educational purposes. Add a license file if you wish to distribute it under a specific open-source license.
