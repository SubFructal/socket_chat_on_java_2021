package ru.sendgoods.javamultithreading.level6.lecture15.chat.client;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BotClient extends Client {

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        int x = (int) (Math.random() * 100);
        return "date_bot_" + x;
    }

    public static void main(String[] args) {
        new BotClient().run();
    }

    public class BotSocketThread extends SocketThread {

        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            String hi = "Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.";
            BotClient.this.sendTextMessage(hi);
            super.clientMainLoop();
        }


        @Override
        protected void processIncomingMessage(String message) {
            super.processIncomingMessage(message);

            String[] split = message.split(": ");
            if (split.length != 2) return;

            String userName = split[0];
            String messageRequest = split[1];

            String pattern = null;
            switch (messageRequest) {
                case "дата":
                    pattern = "d.MM.YYYY";
                    break;
                case "день":
                    pattern = "d";
                    break;
                case "месяц":
                    pattern = "MMMM";
                    break;
                case "год":
                    pattern = "YYYY";
                    break;
                case "время":
                    pattern = "H:mm:ss";
                    break;
                case "час":
                    pattern = "H";
                    break;
                case "минуты":
                    pattern = "m";
                    break;
                case "секунды":
                    pattern = "s";
                    break;
            }

            if (pattern != null) {
                String answer = new SimpleDateFormat(pattern).format(Calendar.getInstance().getTime());
                BotClient.this.sendTextMessage("Информация для " + userName + ": " + answer);
            }

        }
    }
}