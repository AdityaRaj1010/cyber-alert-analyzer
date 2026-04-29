package ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Centralized professional dark theme for the Cybersecurity Alert Analyzer.
 *
 * Uses ONLY core Java AWT / Swing - no third-party LookAndFeel libraries.
 * Apply once at startup with {@link #install()} and additionally call
 * {@link #styleFrame(JFrame)} / {@link #styleTable(JTable)} for tables.
 */
public final class Theme {

    private Theme() {}

    /* ================== Color palette (dark navy + cyan accent) ================== */
    public static final Color BG          = new Color(0x0B, 0x14, 0x24);   // page bg
    public static final Color SURFACE     = new Color(0x14, 0x1F, 0x36);   // panel bg
    public static final Color SURFACE_2   = new Color(0x1B, 0x29, 0x44);   // raised panel
    public static final Color BORDER      = new Color(0x33, 0x46, 0x6B);
    public static final Color TEXT        = new Color(0xE6, 0xED, 0xF7);
    public static final Color TEXT_MUTED  = new Color(0xA1, 0xAE, 0xC4);

    public static final Color ACCENT      = new Color(0x22, 0xD3, 0xEE);   // cyan
    public static final Color ACCENT_DARK = new Color(0x06, 0x91, 0xA8);
    public static final Color OK          = new Color(0x10, 0xB9, 0x81);   // green
    public static final Color WARN        = new Color(0xF5, 0x9E, 0x0B);   // orange
    public static final Color DANGER      = new Color(0xEF, 0x44, 0x44);   // red
    public static final Color CRITICAL    = new Color(0xDC, 0x26, 0x26);   // deep red

    /* ================== Typography (enlarged) ================== */
    public static final Font  TITLE       = new Font("Segoe UI",      Font.BOLD,   28);
    public static final Font  HEADING     = new Font("Segoe UI",      Font.BOLD,   22);
    public static final Font  SUBHEAD     = new Font("Segoe UI",      Font.PLAIN,  18);
    public static final Font  BODY        = new Font("Segoe UI",      Font.PLAIN,  16);
    public static final Font  BODY_BOLD   = new Font("Segoe UI",      Font.BOLD,   16);
    public static final Font  BUTTON      = new Font("Segoe UI",      Font.BOLD,   16);
    public static final Font  CONSOLE     = new Font("Consolas",      Font.PLAIN,  15);
    public static final Font  TABLE       = new Font("Segoe UI",      Font.PLAIN,  15);
    public static final Font  TABLE_HEAD  = new Font("Segoe UI",      Font.BOLD,   16);

    /* ================== One-time UIManager install ================== */
    public static void install() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignore) {}

        UIManager.put("Panel.background",       new ColorUIResource(BG));
        UIManager.put("OptionPane.background",  new ColorUIResource(SURFACE));
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(TEXT));
        UIManager.put("OptionPane.messageFont", new FontUIResource(BODY));
        UIManager.put("OptionPane.buttonFont",  new FontUIResource(BUTTON));
        UIManager.put("Label.foreground",       new ColorUIResource(TEXT));
        UIManager.put("Label.font",             new FontUIResource(BODY));
        UIManager.put("Button.font",            new FontUIResource(BUTTON));
        UIManager.put("ComboBox.font",          new FontUIResource(BODY));
        UIManager.put("TextField.font",         new FontUIResource(BODY));
        UIManager.put("PasswordField.font",     new FontUIResource(BODY));
        UIManager.put("CheckBox.font",          new FontUIResource(BODY));
        UIManager.put("CheckBox.foreground",    new ColorUIResource(TEXT));
        UIManager.put("CheckBox.background",    new ColorUIResource(BG));
        UIManager.put("RadioButton.font",       new FontUIResource(BODY));
        UIManager.put("RadioButton.foreground", new ColorUIResource(TEXT));
        UIManager.put("RadioButton.background", new ColorUIResource(BG));
        UIManager.put("ToolTip.font",           new FontUIResource(BODY));
        UIManager.put("Table.font",             new FontUIResource(TABLE));
        UIManager.put("TableHeader.font",       new FontUIResource(TABLE_HEAD));
        UIManager.put("TabbedPane.font",        new FontUIResource(BODY_BOLD));
        UIManager.put("TitledBorder.font",      new FontUIResource(BODY_BOLD));
        UIManager.put("TitledBorder.titleColor",new ColorUIResource(ACCENT));
    }

    /* ================== Frame & component helpers ================== */
    public static void styleFrame(JFrame f) {
        f.getContentPane().setBackground(BG);
    }

    /** A flat dark button with a colored bar on hover. */
    public static JButton button(String text, Color accent) {
        JButton b = new JButton(text);
        b.setFont(BUTTON);
        b.setForeground(TEXT);
        b.setBackground(SURFACE_2);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 22, 10, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addChangeListener(e -> {
            if (b.getModel().isRollover())  b.setBackground(accent);
            else if (b.getModel().isPressed()) b.setBackground(accent.darker());
            else b.setBackground(SURFACE_2);
        });
        return b;
    }
    public static JButton primary(String text) { return button(text, ACCENT_DARK); }
    public static JButton danger (String text) { return button(text, CRITICAL); }
    public static JButton ok     (String text) { return button(text, OK.darker()); }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(TITLE);
        l.setForeground(ACCENT);
        return l;
    }
    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(HEADING);
        l.setForeground(TEXT);
        return l;
    }
    public static JLabel body(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BODY);
        l.setForeground(TEXT);
        return l;
    }

    public static JTextField textField(int cols) {
        JTextField t = new JTextField(cols);
        t.setFont(BODY);
        t.setBackground(SURFACE);
        t.setForeground(TEXT);
        t.setCaretColor(ACCENT);
        t.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        return t;
    }
    public static JPasswordField passwordField(int cols) {
        JPasswordField t = new JPasswordField(cols);
        t.setFont(BODY);
        t.setBackground(SURFACE);
        t.setForeground(TEXT);
        t.setCaretColor(ACCENT);
        t.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
        return t;
    }

    public static Border card() {
        return BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18));
    }

    public static JPanel card(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(SURFACE);
        p.setBorder(card());
        return p;
    }

    /** Apply consistent styling to a JTable + its scroll pane. */
    public static void styleTable(JTable t) {
        t.setBackground(SURFACE);
        t.setForeground(TEXT);
        t.setGridColor(BORDER);
        t.setSelectionBackground(ACCENT_DARK);
        t.setSelectionForeground(TEXT);
        t.setRowHeight(28);
        t.setFont(TABLE);
        t.setShowGrid(true);
        t.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader h = t.getTableHeader();
        h.setBackground(SURFACE_2);
        h.setForeground(ACCENT);
        h.setFont(TABLE_HEAD);
        h.setBorder(new LineBorder(BORDER));
        h.setReorderingAllowed(false);

        DefaultTableCellRenderer r = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object v,
                    boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, v, sel, foc, row, col);
                c.setFont(TABLE);
                if (!sel) c.setBackground(row % 2 == 0 ? SURFACE : SURFACE_2);
                c.setForeground(severityColor(String.valueOf(tbl.getValueAt(row, 2))));
                return c;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(r);
        }
    }
    public static Color severityColor(String sev) {
        if (sev == null) return TEXT;
        switch (sev) {
            case "CRITICAL": return CRITICAL;
            case "HIGH":     return DANGER;
            case "MEDIUM":   return WARN;
            case "LOW":      return OK;
            default:         return TEXT;
        }
    }

    /** A thin colored stripe used as a header bar across frames. */
    public static JPanel headerBar(String text, Color accent) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, accent.darker(),
                        getWidth(), getHeight(), SURFACE_2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(0, 56));
        JLabel l = new JLabel("  " + text);
        l.setFont(HEADING);
        l.setForeground(TEXT);
        p.add(l, BorderLayout.WEST);
        p.setBorder(new EmptyBorder(8, 16, 8, 16));
        return p;
    }
}
