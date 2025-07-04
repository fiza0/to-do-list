import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TodoApp extends JFrame {
    private JTable todoTable;
    private DefaultTableModel tableModel;
    private JTextField taskField, descriptionField, dueDateField, priorityField;
    private JComboBox<String> statusComboBox;
    private JButton addButton, updateButton, deleteButton, refreshButton;
    private Connection connection;

    // Database connection details - update these for your environment
    private static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:ORCL";
    private static final String DB_USER = "your_username";
    private static final String DB_PASSWORD = "your_password";

    public TodoApp() {
        initializeUI();
        connectToDatabase();
        loadTodos();
    }

    private void initializeUI() {
        setTitle("To-Do List Application");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input panel
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputPanel.add(new JLabel("Task:"));
        taskField = new JTextField();
        inputPanel.add(taskField);

        inputPanel.add(new JLabel("Description:"));
        descriptionField = new JTextField();
        inputPanel.add(descriptionField);

        inputPanel.add(new JLabel("Due Date (yyyy-MM-dd):"));
        dueDateField = new JTextField();
        inputPanel.add(dueDateField);

        inputPanel.add(new JLabel("Priority:"));
        priorityField = new JTextField();
        inputPanel.add(priorityField);

        inputPanel.add(new JLabel("Status:"));
        statusComboBox = new JComboBox<>(new String[]{"Pending", "In Progress", "Completed"});
        inputPanel.add(statusComboBox);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        addButton = new JButton("Add");
        updateButton = new JButton("Update");
        deleteButton = new JButton("Delete");
        refreshButton = new JButton("Refresh");

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        // Table setup
        String[] columnNames = {"ID", "Task", "Description", "Due Date", "Priority", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table cells non-editable
            }
        };
        todoTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(todoTable);

        // Add components to frame
        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add event listeners
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addTodo();
            }
        });

        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateTodo();
            }
        });

        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteTodo();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadTodos();
            }
        });

        // Add selection listener to populate fields when a row is selected
        todoTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && todoTable.getSelectedRow() != -1) {
                int selectedRow = todoTable.getSelectedRow();
                taskField.setText(tableModel.getValueAt(selectedRow, 1).toString());
                descriptionField.setText(tableModel.getValueAt(selectedRow, 2).toString());
                dueDateField.setText(tableModel.getValueAt(selectedRow, 3).toString());
                priorityField.setText(tableModel.getValueAt(selectedRow, 4).toString());
                statusComboBox.setSelectedItem(tableModel.getValueAt(selectedRow, 5).toString());
            }
        });
    }

    private void connectToDatabase() {
        try {
            // Load Oracle JDBC driver
            Class.forName("oracle.jdbc.driver.OracleDriver");
            
            // Establish connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to Oracle database successfully.");
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Oracle JDBC driver not found.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void loadTodos() {
        try {
            // Clear existing data
            tableModel.setRowCount(0);

            // Execute query
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM todos ORDER BY due_date, priority");

            // Populate table
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            while (resultSet.next()) {
                Object[] row = new Object[6];
                row[0] = resultSet.getInt("id");
                row[1] = resultSet.getString("task");
                row[2] = resultSet.getString("description");
                
                Date dueDate = resultSet.getDate("due_date");
                row[3] = dueDate != null ? dateFormat.format(dueDate) : "";
                
                row[4] = resultSet.getString("priority");
                row[5] = resultSet.getString("status");
                tableModel.addRow(row);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading todos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addTodo() {
        String task = taskField.getText().trim();
        String description = descriptionField.getText().trim();
        String dueDateStr = dueDateField.getText().trim();
        String priority = priorityField.getText().trim();
        String status = statusComboBox.getSelectedItem().toString();

        if (task.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Task cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO todos (task, description, due_date, priority, status) VALUES (?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?)");
            
            ps.setString(1, task);
            ps.setString(2, description.isEmpty() ? null : description);
            ps.setString(3, dueDateStr.isEmpty() ? null : dueDateStr);
            ps.setString(4, priority.isEmpty() ? "Medium" : priority);
            ps.setString(5, status);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Task added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                loadTodos();
            }
            ps.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding task: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateTodo() {
        int selectedRow = todoTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task to update", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String task = taskField.getText().trim();
        String description = descriptionField.getText().trim();
        String dueDateStr = dueDateField.getText().trim();
        String priority = priorityField.getText().trim();
        String status = statusComboBox.getSelectedItem().toString();

        if (task.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Task cannot be empty", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            PreparedStatement ps = connection.prepareStatement(
                    "UPDATE todos SET task = ?, description = ?, due_date = TO_DATE(?, 'YYYY-MM-DD'), priority = ?, status = ? WHERE id = ?");
            
            ps.setString(1, task);
            ps.setString(2, description.isEmpty() ? null : description);
            ps.setString(3, dueDateStr.isEmpty() ? null : dueDateStr);
            ps.setString(4, priority.isEmpty() ? "Medium" : priority);
            ps.setString(5, status);
            ps.setInt(6, id);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Task updated successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                clearFields();
                loadTodos();
            }
            ps.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating task: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteTodo() {
        int selectedRow = todoTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task to delete", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete this task?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                PreparedStatement ps = connection.prepareStatement("DELETE FROM todos WHERE id = ?");
                ps.setInt(1, id);
                
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(this, "Task deleted successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearFields();
                    loadTodos();
                }
                ps.close();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting task: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearFields() {
        taskField.setText("");
        descriptionField.setText("");
        dueDateField.setText("");
        priorityField.setText("");
        statusComboBox.setSelectedIndex(0);
        todoTable.clearSelection();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TodoApp app = new TodoApp();
            app.setVisible(true);
        });
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.finalize();
    }
}
