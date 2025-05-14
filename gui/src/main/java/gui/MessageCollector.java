package gui; // Вы можете изменить пакет на ваш

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;

public class MessageCollector {

    public static class TelegramUserInput {
        private final File idFile;
        private final String message;
        private final boolean useRandomDelay;
        private final int maxRandomDelaySeconds;

        public TelegramUserInput(File idFile, String message, boolean useRandomDelay, int maxRandomDelaySeconds) {
            this.idFile = idFile;
            this.message = message;
            this.useRandomDelay = useRandomDelay;
            this.maxRandomDelaySeconds = maxRandomDelaySeconds;
        }

        public File getIdFile() {
            return idFile;
        }

        public String getMessage() {
            return message;
        }

        public boolean isUseRandomDelay() {
            return useRandomDelay;
        }

        public int getMaxRandomDelaySeconds() {
            return maxRandomDelaySeconds;
        }
    }

    private JDialog dialog;
    private JTextField filePathField;
    private JTextArea messageArea;
    private JCheckBox randomDelayCheckbox;
    private JSlider delaySlider;
    private JLabel sliderValueLabel;

    private File selectedIdFile;
    private TelegramUserInput userInputResult = null;

    private static final int SLIDER_MIN_DELAY = 1;    // Минимальная задержка в секундах
    private static final int SLIDER_MAX_DELAY = 300;  // Максимальная задержка в секундах (5 минут)
    private static final int SLIDER_DEFAULT_DELAY = 10; // Задержка по умолчанию

    public MessageCollector(Frame owner) {
        dialog = new JDialog(owner, "Ввод данных для Telegram рассылки", true);
        initComponents();
    }

    private void initComponents() {
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // --- Файл с ID ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3; // Заголовок на всю ширину
        dialog.add(new JLabel("Файл с ID пользователей:"), gbc);

        filePathField = new JTextField(30);
        filePathField.setEditable(false);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2; // Поле занимает 2 колонки
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(filePathField, gbc);

        JButton browseButton = new JButton("Обзор...");
        browseButton.addActionListener(this::browseForFileAction);
        gbc.gridx = 2; // Кнопка в 3-й колонке
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        dialog.add(browseButton, gbc);

        // --- Сообщение ---
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL; // Сброс fill для JLabel
        dialog.add(new JLabel("Сообщение для рассылки:"), gbc);

        messageArea = new JTextArea(7, 30);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.weighty = 0.8; // Даем текстовому полю немного веса для растягивания
        gbc.fill = GridBagConstraints.BOTH;
        dialog.add(scrollPane, gbc);

        // --- Рандомизированная задержка (чекбокс) ---
        randomDelayCheckbox = new JCheckBox("Рандомизированное время отправки");
        randomDelayCheckbox.addItemListener(this::randomDelayCheckboxChanged);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.weighty = 0; // Сброс веса
        gbc.fill = GridBagConstraints.HORIZONTAL;
        dialog.add(randomDelayCheckbox, gbc);

        // --- Ползунок для максимальной задержки ---
        JLabel maxDelayLabelText = new JLabel("Макс. задержка (сек):");
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE; // Сброс fill для JLabel
        gbc.anchor = GridBagConstraints.EAST;
        dialog.add(maxDelayLabelText, gbc);

        delaySlider = new JSlider(JSlider.HORIZONTAL, SLIDER_MIN_DELAY, SLIDER_MAX_DELAY, SLIDER_DEFAULT_DELAY);
        delaySlider.setMajorTickSpacing(SLIDER_MAX_DELAY / 6); // Примерное деление на 6 крупных тиков
        delaySlider.setMinorTickSpacing(SLIDER_MAX_DELAY / 30); // Примерное деление на 30 мелких тиков
        delaySlider.setPaintTicks(true);
        // delaySlider.setPaintLabels(true); // Можно включить, если нужно отображение цифр на ползунке
        delaySlider.addChangeListener(this::delaySliderChanged);
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.weightx = 1.0; // Ползунок растягивается
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST; // Сброс якоря
        dialog.add(delaySlider, gbc);

        sliderValueLabel = new JLabel(SLIDER_DEFAULT_DELAY + " сек");
        gbc.gridx = 2;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.weightx = 0; // Метка не растягивается
        gbc.fill = GridBagConstraints.NONE;
        dialog.add(sliderValueLabel, gbc);

        // Изначальное состояние ползунка и метки
        delaySlider.setEnabled(false);
        sliderValueLabel.setEnabled(false);
        maxDelayLabelText.setEnabled(false); // Также отключаем текстовую метку ползунка


        // --- Кнопки управления ---
        JButton okButton = new JButton("OK");
        okButton.addActionListener(this::okAction);

        JButton cancelButton = new JButton("Отмена");
        cancelButton.addActionListener(e -> {
            userInputResult = null;
            dialog.dispose();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 6; // Сдвигаем вниз
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        dialog.add(buttonPanel, gbc);

        dialog.getRootPane().setDefaultButton(okButton);
        dialog.pack();
        dialog.setMinimumSize(dialog.getPreferredSize());
        dialog.setLocationRelativeTo(dialog.getParent());
    }

    private void browseForFileAction(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите файл с ID пользователей Telegram");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Текстовые файлы (*.txt, *.csv)", "txt", "csv");
        fileChooser.setFileFilter(filter);
        fileChooser.setAcceptAllFileFilterUsed(true);

        int returnValue = fileChooser.showOpenDialog(dialog);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedIdFile = fileChooser.getSelectedFile();
            filePathField.setText(selectedIdFile.getAbsolutePath());
        }
    }

    private void randomDelayCheckboxChanged(ItemEvent e) {
        boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
        delaySlider.setEnabled(selected);
        sliderValueLabel.setEnabled(selected);
        // Также включаем/отключаем текстовую метку для ползунка
        Component[] components = dialog.getContentPane().getComponents();
        for (Component component : components) {
            if (component instanceof JLabel && ((JLabel) component).getText().startsWith("Макс. задержка")) {
                component.setEnabled(selected);
                break;
            }
        }
    }

    private void delaySliderChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        if (!source.getValueIsAdjusting()) { // Обновляем, когда пользователь отпустил ползунок
            sliderValueLabel.setText(source.getValue() + " сек");
        } else { // Можно показывать текущее значение во время перетаскивания
            sliderValueLabel.setText(source.getValue() + " сек*"); // Например, со звездочкой
        }
    }

    private void okAction(ActionEvent e) {
        if (selectedIdFile == null) {
            JOptionPane.showMessageDialog(dialog, "Пожалуйста, выберите файл с ID пользователей.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!selectedIdFile.exists() || !selectedIdFile.isFile()) {
            JOptionPane.showMessageDialog(dialog, "Выбранный файл не существует или не является файлом.", "Ошибка файла", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String message = messageArea.getText();
        if (message == null || message.trim().isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Пожалуйста, введите текст сообщения.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean useRandom = randomDelayCheckbox.isSelected();
        int maxDelay = delaySlider.getValue();

        this.userInputResult = new TelegramUserInput(this.selectedIdFile, message.trim(), useRandom, maxDelay);
        dialog.dispose();
    }

    public TelegramUserInput collectData() {
        this.selectedIdFile = null;
        if (this.filePathField != null) this.filePathField.setText("");
        if (this.messageArea != null) this.messageArea.setText("");
        if (this.randomDelayCheckbox != null) {
            this.randomDelayCheckbox.setSelected(false); // Сброс чекбокса
        }
        // Состояние ползунка и его метки обновится через слушатель randomDelayCheckbox
        if (this.delaySlider != null) {
            this.delaySlider.setValue(SLIDER_DEFAULT_DELAY);
        }
        if (this.sliderValueLabel != null && this.delaySlider != null) { // Обновляем метку при сбросе
            this.sliderValueLabel.setText(this.delaySlider.getValue() + " сек");
        }

        this.userInputResult = null;
        dialog.setVisible(true);
        return this.userInputResult;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Для лучшего вида можно попробовать установить LookAndFeel системы
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            MessageCollector collectorDialog = new MessageCollector(null);
            TelegramUserInput collectedData = collectorDialog.collectData();

            if (collectedData != null) {
                System.out.println("Файл с ID: " + collectedData.getIdFile().getAbsolutePath());
                System.out.println("Сообщение: " + collectedData.getMessage());
                System.out.println("Использовать рандомную задержку: " + collectedData.isUseRandomDelay());
                if (collectedData.isUseRandomDelay()) {
                    System.out.println("Максимальная задержка: " + collectedData.getMaxRandomDelaySeconds() + " сек");
                }
            } else {
                System.out.println("Ввод данных был отменен пользователем.");
            }
            System.exit(0);
        });
    }
}