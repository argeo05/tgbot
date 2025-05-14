package utils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TelegramLauncher {
    private static final int TELEGRAM_STARTUP_DELAY_SECONDS = 1;
    private static final String TELEGRAM_PATH_WINDOWS = System.getProperty("user.home") + "\\AppData\\Roaming\\Telegram Desktop\\Telegram.exe";
    private static final String TELEGRAM_PATH_MACOS = "";
    private static final String TELEGRAM_PATH_LINUX = "";

    public static boolean launchTelegram() {
        System.out.println("Попытка запуска Telegram...");
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder pb = null;
        boolean useDefaultCommand = false;

        try {
            if (os.contains("win")) {
                String primaryPath = TELEGRAM_PATH_WINDOWS;
                File telegramExe = new File(primaryPath);
                System.out.println("Проверка основного пути для Windows: " + primaryPath);

                if (!telegramExe.exists()) {
                    System.out.println("Telegram не найден по основному пути. Пробуем альтернативные пути...");
                    String[] alternativePaths = {
                            "C:\\Program Files\\Telegram Desktop\\Telegram.exe",
                            System.getenv("LOCALAPPDATA") + "\\Microsoft\\WindowsApps\\TelegramDesktop.exe",
                            System.getenv("LOCALAPPDATA") + "\\Telegram Desktop\\Telegram.exe"
                    };
                    for (String altPath : alternativePaths) {
                        System.out.println("Пробуем путь: " + altPath);
                        telegramExe = new File(altPath);
                        if (telegramExe.exists()) {
                            System.out.println("Telegram найден по альтернативному пути: " + altPath);
                            break;
                        }
                    }
                }

                if (telegramExe.exists()) {
                    pb = new ProcessBuilder(telegramExe.getAbsolutePath());
                    System.out.println("Используется путь для Windows: " + telegramExe.getAbsolutePath());
                } else {
                    System.err.println("Не удалось найти исполняемый файл Telegram для Windows.");
                    System.err.println("Пожалуйста, проверьте и установите правильный путь в переменной TELEGRAM_PATH_WINDOWS в TelegramLauncher.java,");
                    System.err.println("или убедитесь, что Telegram установлен в одно из стандартных мест.");
                    return false;
                }
            } else if (os.contains("mac")) {
                if (TELEGRAM_PATH_MACOS != null && !TELEGRAM_PATH_MACOS.isEmpty()) {
                    File macOSExe = new File(TELEGRAM_PATH_MACOS);
                    if (macOSExe.exists()) {
                        pb = new ProcessBuilder(macOSExe.getAbsolutePath());
                        System.out.println("Используется прямой путь для macOS: " + macOSExe.getAbsolutePath());
                    } else {
                        System.out.println("Указанный TELEGRAM_PATH_MACOS (" + TELEGRAM_PATH_MACOS +") не найден. Используем 'open -a Telegram'.");
                        useDefaultCommand = true;
                    }
                } else {
                    useDefaultCommand = true;
                }
                if (useDefaultCommand) {
                    pb = new ProcessBuilder("open", "-a", "Telegram");
                    System.out.println("Используется команда для macOS: open -a Telegram");
                }
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                if (TELEGRAM_PATH_LINUX != null && !TELEGRAM_PATH_LINUX.isEmpty()) {
                    File linuxExe = new File(TELEGRAM_PATH_LINUX);
                    if (linuxExe.exists() && linuxExe.canExecute()) {
                        pb = new ProcessBuilder(linuxExe.getAbsolutePath());
                        System.out.println("Используется прямой путь для Linux: " + linuxExe.getAbsolutePath());
                    } else {
                        System.out.println("Указанный TELEGRAM_PATH_LINUX (" + TELEGRAM_PATH_LINUX +") не найден или не исполняемый. Используем 'telegram-desktop'.");
                        useDefaultCommand = true;
                    }
                } else {
                    useDefaultCommand = true;
                }
                if (useDefaultCommand) {
                    pb = new ProcessBuilder("telegram-desktop");
                    System.out.println("Используется команда для Linux: telegram-desktop");
                }
            } else {
                System.err.println("Неподдерживаемая операционная система для автоматического запуска Telegram: " + os);
                System.err.println("Пожалуйста, запустите Telegram вручную перед использованием рассылки.");
                return false;
            }

            if (pb == null) {
                System.err.println("Не удалось сконфигурировать команду для запуска Telegram.");
                return false;
            }

            System.out.println("Запуск Telegram командой: " + String.join(" ", pb.command()));
            pb.start();

            System.out.println("Ожидание " + TELEGRAM_STARTUP_DELAY_SECONDS + " секунд для инициализации Telegram...");
            TimeUnit.SECONDS.sleep(TELEGRAM_STARTUP_DELAY_SECONDS);
            System.out.println("Ожидание завершено. Предполагается, что Telegram запущен и готов.");
            return true;

        } catch (IOException e) {
            System.err.println("Ошибка ввода-вывода при попытке запуска Telegram: " + e.getMessage());
            System.err.println("Убедитесь, что Telegram установлен, и путь/команда корректны для вашей ОС.");
            System.err.println("Если проблема не решается, запустите Telegram вручную перед использованием рассылки.");
            return false;
        } catch (InterruptedException e) {
            System.err.println("Процесс ожидания запуска Telegram был прерван: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
