package gui;

import utils.TelegramLauncher;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Sender {

    private Robot robot;
    private static final int DEFAULT_DELAY_SECONDS = 5;
    private static final int TYPING_DELAY_MS = 10;
    private static final int ACTION_DELAY_MS = 60;

    public Sender() {
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            System.err.println("Не удалось инициализировать Robot: " + e.getMessage());
            throw new RuntimeException("Ошибка инициализации Robot для автоматизации", e);
        }
    }

    public void startSending(MessageCollector.TelegramUserInput userInput, Point searchBarCoordinates) {
        if (!TelegramLauncher.launchTelegram()) {
            System.out.println("Не удалось запустить Telegram или процесс был отменен. Отправка сообщений не будет выполнена.");
            return;
        }

        String message = userInput.getMessage();
        File idFile = userInput.getIdFile();
        boolean useRandomDelay = userInput.isUseRandomDelay();
        int maxRandomDelaySeconds = userInput.getMaxRandomDelaySeconds();

        List<String> userIds = readUserIdsFromFile(idFile);
        if (userIds.isEmpty()) {
            System.out.println("Файл с ID пользователей пуст или не удалось его прочитать. Отправка отменена.");
            return;
        }

        System.out.println("Начинаем рассылку...");
        Random random = new Random();
        robot.delay(1000);

        for (String userId : userIds) {
            if (userId == null || userId.trim().isEmpty()) {
                System.out.println("Пропущен пустой ID пользователя.");
                continue;
            }
            System.out.println("Отправка сообщения пользователю: " + userId);

            try {
                robot.mouseMove(searchBarCoordinates.x, searchBarCoordinates.y);
                robot.delay(ACTION_DELAY_MS / 2);
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                robot.delay(ACTION_DELAY_MS);

                int selectAllKey = osUsesCommandInsteadOfControl() ? KeyEvent.VK_META : KeyEvent.VK_CONTROL;
                robot.keyPress(selectAllKey);
                robot.keyPress(KeyEvent.VK_A);
                robot.delay(TYPING_DELAY_MS);
                robot.keyRelease(KeyEvent.VK_A);
                robot.keyRelease(selectAllKey);
                robot.delay(ACTION_DELAY_MS);

                robot.keyPress(KeyEvent.VK_DELETE);
                robot.delay(TYPING_DELAY_MS);
                robot.keyRelease(KeyEvent.VK_DELETE);
                robot.delay(ACTION_DELAY_MS);

                pasteText(userId);
                robot.delay(ACTION_DELAY_MS);

                robot.keyPress(KeyEvent.VK_ENTER);
                robot.delay(TYPING_DELAY_MS);
                robot.keyRelease(KeyEvent.VK_ENTER);
                robot.delay(ACTION_DELAY_MS * 3 + (userId.length() * 20));

                pasteText(message);
                robot.delay(ACTION_DELAY_MS);

                robot.keyPress(KeyEvent.VK_ENTER);
                robot.delay(TYPING_DELAY_MS);
                robot.keyRelease(KeyEvent.VK_ENTER);
                System.out.println("Сообщение для " + userId + " отправлено.");

                long delayMilliseconds;
                if (useRandomDelay) {
                    int randomSeconds = (maxRandomDelaySeconds > 0) ? (random.nextInt(maxRandomDelaySeconds) + 1) : 1;
                    System.out.println("Случайная задержка: " + randomSeconds + " сек.");
                    delayMilliseconds = TimeUnit.SECONDS.toMillis(randomSeconds);
                } else {
                    System.out.println("Стандартная задержка: " + DEFAULT_DELAY_SECONDS + " сек.");
                    delayMilliseconds = TimeUnit.SECONDS.toMillis(DEFAULT_DELAY_SECONDS);
                }
                delayMilliseconds += random.nextInt(500) + 100;
                robot.delay((int)delayMilliseconds);

            } catch (Exception e) {
                System.err.println("Произошла ошибка при отправке сообщения пользователю " + userId + ": " + e.getMessage());
                e.printStackTrace();
                try {
                    System.out.println("Попытка восстановиться и продолжить со следующим пользователем...");
                    TimeUnit.SECONDS.sleep(DEFAULT_DELAY_SECONDS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    System.err.println("Рассылка прервана во время восстановления после ошибки.");
                    return;
                }
            }
        }
        System.out.println("Рассылка завершена.");
    }

    private List<String> readUserIdsFromFile(File file) {
        List<String> ids = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) {
                    ids.add(trimmedLine);
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла с ID: " + e.getMessage());
        }
        return ids;
    }

    private void pasteText(String text) {
        try {
            StringSelection stringSelection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            robot.delay(100);

            int pasteKey = osUsesCommandInsteadOfControl() ? KeyEvent.VK_META : KeyEvent.VK_CONTROL;

            robot.keyPress(pasteKey);
            robot.keyPress(KeyEvent.VK_V);
            robot.delay(TYPING_DELAY_MS);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(pasteKey);
        } catch (Exception e) {
            System.err.println("Ошибка при вставке текста из буфера обмена: " + e.getMessage());
        }
    }

    private boolean osUsesCommandInsteadOfControl() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
}
