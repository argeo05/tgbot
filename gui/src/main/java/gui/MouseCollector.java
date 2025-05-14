package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyAdapter;
import utils.TelegramLauncher;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MouseCollector {

    private static class PointHolder {
        Point point = null;
    }

    private final PointHolder lastClickCoordinatesHolder = new PointHolder();
    private JLabel coordsLabel;

    public Point setupCoordinatesWithGlobalHotkey(String targetName) {
        if (!TelegramLauncher.launchTelegram()) {
            System.out.println("Не удалось запустить Telegram или процесс был отменен.");
        }

        final JDialog helperDialog = new JDialog((Frame) null, "Захват координат (глобальный F12)", true);
        helperDialog.setLayout(new BorderLayout(10, 10));
        helperDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        final String captureKeyText = "F12";

        coordsLabel = new JLabel("Координаты: (нажмите " + captureKeyText + ")", SwingConstants.CENTER);

        JLabel instructionLabel = new JLabel(
                "<html><body style='width: 300px; text-align: center;'>" +
                        "1. Наведите курсор мыши на " + targetName + " на экране.<br><br>" +
                        "2. Нажмите клавишу <b>" + captureKeyText + "</b> для захвата координат курсора.<br><br>" +
                        "3. Для завершения нажмите кнопку 'Готово' или клавишу Enter в этом окне.</body></html>",
                SwingConstants.CENTER
        );
        instructionLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        coordsLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

        JButton doneButton = new JButton("Готово (Enter)");

        Action doneAction = new AbstractAction("Готово (Enter)") {
            @Override
            public void actionPerformed(ActionEvent e) {
                helperDialog.dispose();
            }
        };
        doneButton.setAction(doneAction);

        JRootPane rootPane = helperDialog.getRootPane();
        KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enterKeyStroke, "doneAction");
        rootPane.getActionMap().put("doneAction", doneAction);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.add(doneButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

        helperDialog.add(instructionLabel, BorderLayout.NORTH);
        helperDialog.add(coordsLabel, BorderLayout.CENTER);
        helperDialog.add(buttonPanel, BorderLayout.SOUTH);

        helperDialog.pack();
        helperDialog.setMinimumSize(helperDialog.getPreferredSize());
        helperDialog.setLocationRelativeTo(null);
        helperDialog.setAlwaysOnTop(true);

        NativeKeyAdapter globalF12Listener = new NativeKeyAdapter() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                if (e.getKeyCode() == NativeKeyEvent.VC_F12) {
                    Point currentMousePosition = MouseInfo.getPointerInfo().getLocation();
                    lastClickCoordinatesHolder.point = currentMousePosition;

                    SwingUtilities.invokeLater(() -> {
                        if (coordsLabel != null) {
                            coordsLabel.setText("<html>Координаты: <b>X=" + currentMousePosition.x + ", Y=" + currentMousePosition.y + "</b></html>");
                        }
                    });
                }
            }
        };

        boolean hookOwnedByThisMethod = false;
        try {
            Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);

            if (!GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.registerNativeHook();
                hookOwnedByThisMethod = true;
            }
            GlobalScreen.addNativeKeyListener(globalF12Listener);

            helperDialog.setVisible(true);

        } catch (NativeHookException ex) {
            System.err.println("Проблема при регистрации глобального перехватчика: " + ex.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Не удалось активировать глобальный перехват клавиш (" + ex.getMessage() + ").\n" +
                            "Возможно, программа не имеет необходимых разрешений или есть конфликт.",
                    "Ошибка глобального перехвата", JOptionPane.ERROR_MESSAGE);
            return null;
        } finally {
            GlobalScreen.removeNativeKeyListener(globalF12Listener);
            if (hookOwnedByThisMethod && GlobalScreen.isNativeHookRegistered()) {
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException ex) {
                    System.err.println("Проблема при отмене регистрации глобального перехватчика: " + ex.getMessage());
                }
            }
        }
        return lastClickCoordinatesHolder.point;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MouseCollector setuper = new MouseCollector();
            Point clickPoint = setuper.setupCoordinatesWithGlobalHotkey(
                    "tg message"
            );

            if (clickPoint != null) {
                System.out.println("Захваченные координаты (положение мыши): X=" + clickPoint.x + ", Y=" + clickPoint.y);
            } else {
                System.out.println("Координаты не были захвачены.");
            }
            System.exit(0);
        });
    }
}