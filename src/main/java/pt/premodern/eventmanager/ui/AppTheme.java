package pt.premodern.eventmanager.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.JTableHeader;

final class AppTheme {
    static final Color BACKGROUND = new Color(17, 18, 17);
    static final Color SIDEBAR = new Color(39, 39, 39);
    static final Color SURFACE = new Color(34, 34, 34);
    static final Color SURFACE_ALT = new Color(43, 43, 43);
    static final Color INPUT = new Color(55, 55, 55);
    static final Color BORDER = new Color(62, 62, 62);
    static final Color TEXT = new Color(242, 244, 247);
    static final Color MUTED = new Color(165, 166, 171);
    static final Color BLUE = new Color(47, 166, 244);
    static final Color BLUE_DARK = new Color(34, 122, 183);
    static final Color ORANGE = new Color(255, 174, 18);
    static final Color GREEN = new Color(0, 200, 118);
    static final Color RED = new Color(226, 76, 68);

    static final String MANUAL_BUTTON = "eventmanager.manualButton";
    private static final String THEMED_BUTTON = "eventmanager.themedButton";
    private static final String BUTTON_BACKGROUND = "eventmanager.buttonBackground";
    private static final String BUTTON_HOVER = "eventmanager.buttonHover";

    private AppTheme() {
    }

    static void install() {
        install(true);
    }

    static void install(boolean darkMode) {
        UIManager.put("Panel.background", background(darkMode));
        UIManager.put("Label.foreground", text(darkMode));
        UIManager.put("Button.background", surfaceAlt(darkMode));
        UIManager.put("Button.foreground", text(darkMode));
        UIManager.put("Button.select", BLUE_DARK);
        UIManager.put("ComboBox.background", input(darkMode));
        UIManager.put("ComboBox.foreground", text(darkMode));
        UIManager.put("ComboBox.disabledBackground", surface(darkMode));
        UIManager.put("ComboBox.disabledForeground", muted(darkMode));
        UIManager.put("ComboBox.selectionBackground", BLUE_DARK);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("TextField.background", input(darkMode));
        UIManager.put("TextField.foreground", text(darkMode));
        UIManager.put("TextArea.background", input(darkMode));
        UIManager.put("TextArea.foreground", text(darkMode));
        UIManager.put("Table.background", background(darkMode));
        UIManager.put("Table.foreground", text(darkMode));
        UIManager.put("Table.selectionBackground", BLUE_DARK);
        UIManager.put("Table.selectionForeground", Color.WHITE);
        UIManager.put("TableHeader.background", background(darkMode));
        UIManager.put("TableHeader.foreground", text(darkMode));
        UIManager.put("TableHeader.cellBorder", BorderFactory.createLineBorder(border(darkMode)));
        UIManager.put("ScrollPane.background", background(darkMode));
        UIManager.put("Viewport.background", background(darkMode));
        UIManager.put("OptionPane.background", surface(darkMode));
        UIManager.put("OptionPane.messageForeground", text(darkMode));
        UIManager.put("MenuBar.background", menuBackground(darkMode));
        UIManager.put("MenuBar.foreground", text(darkMode));
        UIManager.put("Menu.background", menuBackground(darkMode));
        UIManager.put("Menu.foreground", text(darkMode));
        UIManager.put("Menu.selectionBackground", surfaceAlt(darkMode));
        UIManager.put("Menu.selectionForeground", text(darkMode));
        UIManager.put("MenuItem.background", surface(darkMode));
        UIManager.put("MenuItem.foreground", text(darkMode));
        UIManager.put("MenuItem.selectionBackground", BLUE_DARK);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("PopupMenu.background", surface(darkMode));
        UIManager.put("PopupMenu.foreground", text(darkMode));
    }

    static void styleTree(Component component) {
        styleTree(component, true);
    }

    static void styleTree(Component component, boolean darkMode) {
        styleComponent(component, darkMode);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                styleTree(child, darkMode);
            }
        }
    }

    static void styleComponent(Component component) {
        styleComponent(component, true);
    }

    static void styleComponent(Component component, boolean darkMode) {
        if (component instanceof JMenuBar menuBar) {
            menuBar.setBackground(menuBackground(darkMode));
            menuBar.setForeground(text(darkMode));
            menuBar.setOpaque(true);
            menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, border(darkMode)));
        } else if (component instanceof JMenu || component instanceof JMenuItem) {
            component.setBackground(menuBackground(darkMode));
            component.setForeground(text(darkMode));
            component.setFont(component.getFont().deriveFont(Font.BOLD, 13f));
            if (component instanceof JComponent jComponent) {
                jComponent.setOpaque(true);
            }
        } else if (component instanceof JTable table) {
            styleTable(table, darkMode);
        } else if (component instanceof JScrollPane scrollPane) {
            if (findTitledBorder(scrollPane.getBorder()) == null) {
                scrollPane.setBorder(BorderFactory.createLineBorder(border(darkMode)));
            } else {
                restyleBorder(scrollPane, darkMode);
            }
            scrollPane.getViewport().setBackground(background(darkMode));
            scrollPane.setBackground(background(darkMode));
        } else if (component instanceof JTextField textField) {
            styleTextField(textField, darkMode);
        } else if (component instanceof JTextArea textArea) {
            textArea.setBackground(input(darkMode));
            textArea.setForeground(text(darkMode));
            textArea.setCaretColor(text(darkMode));
            textArea.setSelectionColor(BLUE_DARK);
            textArea.setSelectedTextColor(Color.WHITE);
            textArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        } else if (component instanceof JComboBox<?> comboBox) {
            styleCombo(comboBox, darkMode);
        } else if (component instanceof JSpinner spinner) {
            spinner.setBackground(input(darkMode));
            spinner.setForeground(text(darkMode));
            if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
                styleTextField(editor.getTextField(), darkMode);
            }
        } else if (component instanceof JCheckBox checkBox) {
            checkBox.setOpaque(false);
            checkBox.setForeground(text(darkMode));
        } else if (component instanceof JToggleButton toggleButton) {
            styleToggle(toggleButton, darkMode);
        } else if (component instanceof AbstractButton button) {
            styleButton(button, darkMode);
        } else if (component instanceof JLabel label) {
            label.setForeground(text(darkMode));
        } else if (component instanceof JPanel panel) {
            panel.setBackground(background(darkMode));
            restyleBorder(panel, darkMode);
        } else {
            component.setBackground(background(darkMode));
            component.setForeground(text(darkMode));
        }
    }

    static void styleSidebarButton(AbstractButton button, boolean selected) {
        styleSidebarButton(button, selected, true);
    }

    static void styleSidebarButton(AbstractButton button, boolean selected, boolean darkMode) {
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setHorizontalAlignment(AbstractButton.LEFT);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 15f));
        button.setForeground(selected ? (darkMode ? Color.WHITE : new Color(20, 20, 20)) : text(darkMode));
        button.setBackground(selected ? sidebarSelected(darkMode) : sidebar(darkMode));
        button.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 14));
    }

    static void styleTitle(JLabel label) {
        styleTitle(label, true);
    }

    static void styleTitle(JLabel label, boolean darkMode) {
        label.setForeground(BLUE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 26f));
    }

    static Color background(boolean darkMode) {
        return darkMode ? BACKGROUND : new Color(245, 246, 248);
    }

    static Color sidebar(boolean darkMode) {
        return darkMode ? SIDEBAR : new Color(232, 234, 237);
    }

    static Color surface(boolean darkMode) {
        return darkMode ? SURFACE : Color.WHITE;
    }

    static Color surfaceAlt(boolean darkMode) {
        return darkMode ? SURFACE_ALT : new Color(235, 238, 242);
    }

    static Color input(boolean darkMode) {
        return darkMode ? INPUT : Color.WHITE;
    }

    static Color border(boolean darkMode) {
        return darkMode ? BORDER : new Color(198, 203, 209);
    }

    static Color text(boolean darkMode) {
        return darkMode ? TEXT : new Color(28, 32, 36);
    }

    static Color muted(boolean darkMode) {
        return darkMode ? MUTED : new Color(92, 99, 108);
    }

    private static Color menuBackground(boolean darkMode) {
        return darkMode ? new Color(24, 24, 24) : new Color(238, 238, 238);
    }

    private static Color sidebarSelected(boolean darkMode) {
        return darkMode ? new Color(51, 51, 51) : new Color(211, 216, 222);
    }

    private static void styleButton(AbstractButton button, boolean darkMode) {
        if (Boolean.TRUE.equals(button.getClientProperty(MANUAL_BUTTON))) {
            return;
        }
        boolean primary = isPrimary(button.getText());
        boolean danger = isDanger(button.getText());
        Color normal = button.isEnabled()
                ? danger ? RED : primary ? BLUE : surfaceAlt(darkMode)
                : surfaceAlt(darkMode);
        Color hover = danger ? RED.darker() : primary ? BLUE_DARK : input(darkMode);

        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setForeground(button.isEnabled()
                ? primary || danger ? Color.WHITE : text(darkMode)
                : muted(darkMode));
        button.setBackground(normal);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.putClientProperty(BUTTON_BACKGROUND, normal);
        button.putClientProperty(BUTTON_HOVER, hover);
        if (!Boolean.TRUE.equals(button.getClientProperty(THEMED_BUTTON))) {
            button.putClientProperty(THEMED_BUTTON, true);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (button.isEnabled()) {
                        button.setBackground((Color) button.getClientProperty(BUTTON_HOVER));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (button.isEnabled()) {
                        button.setBackground((Color) button.getClientProperty(BUTTON_BACKGROUND));
                    }
                }
            });
        }
    }

    private static void styleToggle(JToggleButton button, boolean darkMode) {
        if (Boolean.TRUE.equals(button.getClientProperty(MANUAL_BUTTON))) {
            return;
        }
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setForeground(button.isSelected() ? Color.WHITE : text(darkMode));
        button.setBackground(button.isSelected() ? BLUE : surfaceAlt(darkMode));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
    }

    private static void styleTextField(JTextField textField, boolean darkMode) {
        textField.setBackground(input(darkMode));
        textField.setForeground(text(darkMode));
        textField.setCaretColor(text(darkMode));
        textField.setSelectionColor(BLUE_DARK);
        textField.setSelectedTextColor(Color.WHITE);
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border(darkMode)),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void styleCombo(JComboBox<?> comboBox, boolean darkMode) {
        comboBox.setUI(new ThemedComboBoxUI(darkMode));
        comboBox.setBackground(input(darkMode));
        comboBox.setForeground(comboBox.isEnabled() ? text(darkMode) : muted(darkMode));
        comboBox.setOpaque(true);
        comboBox.setBorder(BorderFactory.createLineBorder(border(darkMode)));

        comboBox.setRenderer((list, value, index, selected, cellHasFocus) -> {
            JLabel label = new DefaultTableCellRenderer();
            label.setText(value == null ? "" : value.toString());
            label.setOpaque(true);
            label.setBackground(selected ? BLUE_DARK : input(darkMode));
            label.setForeground(selected ? Color.WHITE : text(darkMode));
            label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return label;
        });
    }

    private static final class ThemedComboBoxUI extends BasicComboBoxUI {
        private final boolean darkMode;

        private ThemedComboBoxUI(boolean darkMode) {
            this.darkMode = darkMode;
        }

        @Override
        protected JButton createArrowButton() {
            JButton button = new JButton() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g2 = (Graphics2D) graphics.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isEnabled() ? surfaceAlt(darkMode) : surface(darkMode));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setColor(border(darkMode));
                    g2.drawLine(0, 0, 0, getHeight());

                    int centerX = getWidth() / 2;
                    int centerY = getHeight() / 2 + 1;
                    int size = Math.max(4, Math.min(getWidth(), getHeight()) / 5);
                    Polygon arrow = new Polygon();
                    arrow.addPoint(centerX - size, centerY - size / 2);
                    arrow.addPoint(centerX + size, centerY - size / 2);
                    arrow.addPoint(centerX, centerY + size);
                    g2.setColor(isEnabled() ? text(darkMode) : muted(darkMode));
                    g2.fillPolygon(arrow);
                    g2.dispose();
                }
            };
            button.setOpaque(true);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder());
            return button;
        }
    }

    private static void styleTable(JTable table) {
        styleTable(table, true);
    }

    private static void styleTable(JTable table, boolean darkMode) {
        table.setBackground(background(darkMode));
        table.setForeground(text(darkMode));
        table.setGridColor(darkMode ? new Color(30, 30, 30) : new Color(222, 226, 230));
        table.setSelectionBackground(BLUE_DARK);
        table.setSelectionForeground(Color.WHITE);
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setBackground(background(darkMode));
            header.setForeground(text(darkMode));
            header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, border(darkMode)));
            header.setOpaque(true);
            header.setDefaultRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                        boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, selected, hasFocus, row, column);
                    c.setForeground(text(darkMode));
                    c.setBackground(background(darkMode));
                    setFont(getFont().deriveFont(Font.BOLD, 14f));
                    setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 2, 1, border(darkMode)),
                            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
                    return c;
                }
            });
        }

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, selected, hasFocus, row, column);
                c.setForeground(selected ? Color.WHITE : text(darkMode));
                c.setBackground(selected ? BLUE_DARK : tableRow(darkMode, row));
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(Number.class, renderer);
        table.setDefaultRenderer(Integer.class, renderer);
    }

    private static Color tableRow(boolean darkMode, int row) {
        if (darkMode) {
            return row % 2 == 0 ? SURFACE_ALT : new Color(12, 13, 13);
        }
        return row % 2 == 0 ? Color.WHITE : new Color(241, 243, 246);
    }

    private static void restyleBorder(JComponent component, boolean darkMode) {
        Border border = component.getBorder();
        TitledBorder titled = findTitledBorder(border);
        if (titled != null) {
            titled.setTitleColor(ORANGE);
            titled.setTitleFont(component.getFont().deriveFont(Font.BOLD, 16f));
            titled.setBorder(BorderFactory.createLineBorder(border(darkMode)));
        }
    }

    private static TitledBorder findTitledBorder(Border border) {
        if (border instanceof TitledBorder titled) {
            return titled;
        }
        if (border instanceof CompoundBorder compound) {
            TitledBorder outside = findTitledBorder(compound.getOutsideBorder());
            return outside != null ? outside : findTitledBorder(compound.getInsideBorder());
        }
        return null;
    }

    private static boolean isPrimary(String text) {
        if (text == null) {
            return false;
        }
        return text.startsWith("Add")
                || text.startsWith("Create")
                || text.startsWith("Generate")
                || text.startsWith("Confirm")
                || text.startsWith("Submit")
                || text.startsWith("Refresh")
                || text.startsWith("Import")
                || text.startsWith("Save")
                || text.startsWith("Start")
                || text.startsWith("End Current Event");
    }

    private static boolean isDanger(String text) {
        if (text == null) {
            return false;
        }
        return text.startsWith("Remove") || text.startsWith("Drop") || text.startsWith("Cancel");
    }
}
