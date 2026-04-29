package ui;

import dao.AlertDAO;
import model.SecurityAlert;
import util.AlertFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

/**
 * Read-only window that displays only HIGH and CRITICAL alerts.
 *
 * Demonstrates:
 *   - Iteration over a {@link java.util.List}
 *   - StringBuffer formatting via {@link AlertFormatter}
 *   - MouseListener (double-click for full details)
 */
public class CriticalAlertsFrame extends JFrame {

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String>            list      = new JList<>(listModel);
    private List<SecurityAlert>            data;

    public CriticalAlertsFrame() {
        super("CRITICAL Alerts");
        Theme.styleFrame(this);
        setLayout(new BorderLayout());

        add(Theme.headerBar("⚠   HIGH  &  CRITICAL  ALERTS",
                Theme.CRITICAL), BorderLayout.NORTH);

        list.setFont(Theme.CONSOLE);
        list.setBackground(new Color(0x07, 0x10, 0x1F));
        list.setForeground(new Color(0xFF, 0xC4, 0xC4));
        list.setSelectionBackground(Theme.CRITICAL);
        list.setSelectionForeground(Color.WHITE);
        list.setBorder(new EmptyBorder(10, 12, 10, 12));
        list.setFixedCellHeight(26);

        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));

        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(Theme.BG);
        center.setBorder(new EmptyBorder(14, 16, 14, 16));
        center.add(sp, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JButton btnRefresh = Theme.primary("Refresh");
        JButton btnDetail  = Theme.button("View Selected", Theme.WARN);
        JButton btnClose   = Theme.danger("Close");

        // ActionListener WITH lambda
        btnRefresh.addActionListener(e -> reload());
        btnDetail .addActionListener(e -> showSelected());
        btnClose  .addActionListener(e -> dispose());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        south.setBackground(Theme.SURFACE_2);
        south.add(btnRefresh); south.add(btnDetail); south.add(btnClose);
        add(south, BorderLayout.SOUTH);

        // MouseListener -> double-click for detail (without lambda)
        list.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showSelected();
            }
        });

        reload();
        setSize(1100, 600);
        setLocationRelativeTo(null);
    }

    private void reload() {
        listModel.clear();
        try {
            data = new AlertDAO().findCritical();
            for (SecurityAlert a : data) {
                listModel.addElement(AlertFormatter.formatLine(a));
            }
            if (data.isEmpty()) listModel.addElement("(no critical alerts)");
        } catch (SQLException ex) {
            listModel.addElement("DB unreachable: " + ex.getMessage());
        }
    }

    private void showSelected() {
        int i = list.getSelectedIndex();
        if (data == null || i < 0 || i >= data.size()) return;
        JOptionPane.showMessageDialog(this,
                AlertFormatter.formatDetail(data.get(i)),
                "Alert Detail", JOptionPane.WARNING_MESSAGE);
    }
}
