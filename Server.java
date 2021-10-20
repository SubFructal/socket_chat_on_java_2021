package ru.sendgoods.javamultithreading.level6.lecture15.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ConsoleHelper.writeMessage("Введите порт сервера");
        int port = ConsoleHelper.readInt();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            ConsoleHelper.writeMessage("Чат сервер запущен");
            while (true) {
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Произошла ошибка при запуске или работе сервера.");
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ConsoleHelper.writeMessage("Установлено новое соединение с удаленным адресом " +
                    socket.getRemoteSocketAddress());

            String userName = null;

            try (Connection connection = new Connection(socket)) {
                userName = this.serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                notifyUsers(connection, userName);
                serverMainLoop(connection, userName);
            } catch (IOException | ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Произошла ошибка при обмене данными с удаленным адресом" +
                        socket.getRemoteSocketAddress());
            }

            if (userName != null) {
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }

            ConsoleHelper.writeMessage("Соединение с удаленным адресом " + socket.getRemoteSocketAddress() +
                    "закрыто.");
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message message = connection.receive();
                if (message.getType() != MessageType.USER_NAME) {
                    ConsoleHelper.writeMessage("Получено сообщение от " + socket.getRemoteSocketAddress() +
                            ". Тип сообщения не соответствует протоколу.");
                    continue;
                }
                String userName = message.getData();
                if (userName.isEmpty()) {
                    ConsoleHelper.writeMessage("Попытка подключения к серверу с пустым именем от " +
                            socket.getRemoteSocketAddress());
                    continue;
                }
                if (connectionMap.containsKey(userName)) {
                    ConsoleHelper.writeMessage("Попытка подключения к серверу с уже используемым именем от " +
                            socket.getRemoteSocketAddress());
                    continue;
                }
                connectionMap.put(userName, connection);
                connection.send(new Message(MessageType.NAME_ACCEPTED));
                return userName;
            }
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (String name : connectionMap.keySet()) {
                if (!name.equals(userName)) {
                    connection.send(new Message(MessageType.USER_ADDED, name));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    String text = userName + ": " + message.getData();
                    sendBroadcastMessage(new Message(MessageType.TEXT, text));
                } else {
                    ConsoleHelper.writeMessage("Получено сообщение от " + socket.getRemoteSocketAddress() +
                            ". Тип сообщения не соответствует протоколу.");
                }
            }

        }
    }

    public static void sendBroadcastMessage(Message message) {
        for (Map.Entry<String, Connection> pair : connectionMap.entrySet()) {
            try {
                pair.getValue().send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Error! Can't send message " + pair.getValue().getRemoteSocketAddress());
            }
        }

    }
}