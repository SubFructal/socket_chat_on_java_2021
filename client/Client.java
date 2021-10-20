package ru.sendgoods.javamultithreading.level6.lecture15.chat.client;

import ru.sendgoods.javamultithreading.level6.lecture15.chat.Connection;
import ru.sendgoods.javamultithreading.level6.lecture15.chat.ConsoleHelper;
import ru.sendgoods.javamultithreading.level6.lecture15.chat.Message;
import ru.sendgoods.javamultithreading.level6.lecture15.chat.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {

    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        new Client().run();
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            ConsoleHelper.writeMessage("Возникла ошибка во время работы клиента.");
            return;
        }

        if (clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        } else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }
        while (clientConnected) {
            String data = ConsoleHelper.readString();
            if (data.equalsIgnoreCase("exit")) {
                break;
            }

            if (shouldSendTextFromConsole()) {
                sendTextMessage(data);
            }
        }
    }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес сервера");
        String serverAddress = ConsoleHelper.readString();
        return serverAddress;
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите порт сервера");
        int port = ConsoleHelper.readInt();
        return port;
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите ваше имя");
        String userName = ConsoleHelper.readString();
        return userName;
    }

    protected boolean shouldSendTextFromConsole() {
        return true;
    }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Произошла ошибка во время отправки сообщения");
            clientConnected = false;
        }
    }


    public class SocketThread extends Thread {

        @Override
        public void run() {
            try {
                String serverAddress = Client.this.getServerAddress();
                int serverPort = Client.this.getServerPort();
                Socket socket = new Socket(serverAddress, serverPort);
                Client.this.connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }

        }

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник " + userName + " присоединился к чату");
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("Участник " + userName + " покинул чат");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this) {
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            while (true) {
                MessageType messageType = connection.receive().getType();
                if (messageType == MessageType.NAME_REQUEST) {
                    String userName = Client.this.getUserName();
                    connection.send(new Message(MessageType.USER_NAME, userName));
                } else if (messageType == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();

                if (message.getType() == MessageType.TEXT) {
                    processIncomingMessage(message.getData());
                } else if (message.getType() == MessageType.USER_ADDED) {
                    informAboutAddingNewUser(message.getData());
                } else if (message.getType() == MessageType.USER_REMOVED) {
                    informAboutDeletingNewUser(message.getData());
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

    }
}