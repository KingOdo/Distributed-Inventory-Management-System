import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class InventoryClient extends JFrame{
    private static final int DEFAULT_PORT = 5122;

    private JTextField serverIpField;
    private JTextField portField;
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton verifyButton;
    private JButton overviewButton;
    private JLabel statusLabel;

    private JPanel fileButtonPanel;
    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextArea overviewArea;

    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;

    private void buildGUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        serverIpField = new JTextField("127.0.0.1", 12);
        portField = new JTextField(String.valueOf(DEFAULT_PORT), 6);

        connectButton = new JButton("Connect");
        disconnectButton = new JButton("Disconnect");
        verifyButton = new JButton("Verify");

        disconnectButton.setEnabled(false);
        verifyButton.setEnabled(false);

        statusLabel = new JLabel("Disconnected");
        statusLabel.setForeground(Color.RED);

        connectionPanel.add(new JLabel("Server IP:"));
        connectionPanel.add(serverIpField);
        connectionPanel.add(new JLabel("Port:"));
        connectionPanel.add(portField);
        connectionPanel.add(connectButton);
        connectionPanel.add(disconnectButton);
        connectionPanel.add(verifyButton);
        connectionPanel.add(new JLabel("Status:"));
        connectionPanel.add(statusLabel);

        fileButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fileButtonPanel.setBorder(BorderFactory.createTitledBorder("Available Inventory Files"));

        overviewButton = new JButton("Overview");
        overviewButton.setEnabled(false);

        fileButtonPanel.add(overviewButton);

        tableModel = new DefaultTableModel(new String[]{"Product ID", "Product Name", "Price"}, 0);
        productTable = new JTable(tableModel);

        JScrollPane tableScrollPane = new JScrollPane(productTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Product Data"));

        overviewArea = new JTextArea();
        overviewArea.setEditable(false);

        JScrollPane overviewScrollPane = new JScrollPane(overviewArea);
        overviewScrollPane.setBorder(BorderFactory.createTitledBorder("Overview Result"));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                tableScrollPane,
                overviewScrollPane
        );

        splitPane.setResizeWeight(0.65);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.add(connectionPanel, BorderLayout.NORTH);
        topSection.add(fileButtonPanel, BorderLayout.SOUTH);

        mainPanel.add(topSection, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel);

        connectButton.addActionListener(e -> connectToServer());
        disconnectButton.addActionListener(e -> disconnectFromServer());
        verifyButton.addActionListener(e -> requestVerification());
        overviewButton.addActionListener(e -> requestOverview());
    }

    public InventoryClient() {
        setTitle("Inventory Client - CMPE412 Project");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildGUI();
    }

    private void connectToServer() {
        try {
            String serverIp = serverIpField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());

            socket = new Socket(serverIp, port);

            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            statusLabel.setText("Connected");
            statusLabel.setForeground(Color.GREEN);

            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            verifyButton.setEnabled(true);
            overviewButton.setEnabled(true);

            requestFileList();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not connect to server: " + e.getMessage());
        }
    }

    private void requestFileList() {
        try {
            output.println("LIST");

            String response = input.readLine();

            if (response == null) {
                JOptionPane.showMessageDialog(this, "No response from ther server. Try again");
                return;
            }

            if (response.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(this, response);
                return;
            }

            if (response.equals("FILE_LIST_START")) {
                fileButtonPanel.removeAll();
                fileButtonPanel.add(overviewButton);

                String fileName;

                while ((fileName = input.readLine()) != null) {
                    if (fileName.equals("FILE_LIST_END")) {
                        break;
                    }

                    JButton fileButton = new JButton(fileName);

                    String selectedFile = fileName;
                    fileButton.addActionListener(e -> requestFileContent(selectedFile));

                    fileButtonPanel.add(fileButton);
                }

                fileButtonPanel.revalidate();
                fileButtonPanel.repaint();
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error receiving file list: " + e.getMessage());
        }
    }

    private void requestFileContent(String fileName) {
        try {
            output.println("GET_FILE:" + fileName);

            String response = input.readLine();

            if (response == null) {
                JOptionPane.showMessageDialog(this, "No response from server.");
                return;
            }

            if (response.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(this, response);
                return;
            }

            if (response.equals("FILE_CONTENT_START")) {
                tableModel.setRowCount(0);
                overviewArea.setText("");

                String line;

                while ((line = input.readLine()) != null) {
                    if (line.equals("FILE_CONTENT_END")) {
                        break;
                    }

                    addProductLineToTable(line);
                }
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error receiving file content: " + e.getMessage());
        }
    }

    private void addProductLineToTable(String line) {
    String[] parts = line.split(",");

        if (parts.length == 3) {
        String productId = parts[0].trim();
        String productName = parts[1].trim();
        String price = parts[2].trim();

        tableModel.addRow(new Object[]{productId, productName, price});
      }
    }

    private void requestOverview() {
        try {
            output.println("OVERVIEW");

            String response = input.readLine();

            if (response == null) {
                JOptionPane.showMessageDialog(this, "No response from server.");
                return;
            }

            if (response.startsWith("ERROR")) {
                JOptionPane.showMessageDialog(this, response);
                return;
            }

            if (response.equals("OVERVIEW_START")) {
                overviewArea.setText("");

                String line;

                while ((line = input.readLine()) != null) {
                    if (line.equals("OVERVIEW_END")) {
                        break;
                    }

                    overviewArea.append(line + "\n");
                }
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error receiving overview: " + e.getMessage());
        }
    }

    private void requestVerification() {
        try {
            output.println("VERIFY");

            String response = input.readLine();

            if (response != null && response.startsWith("VERIFY_RESULT:")) {
                String hash = response.substring("VERIFY_RESULT:".length());

                JOptionPane.showMessageDialog(
                        this,
                        "Verification Hash: " + hash,
                        "Student Verification",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(this, "Invalid verification response from server.");
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error during verification: " + e.getMessage());
        }
    }

    private void disconnectFromServer() {
        try {
            if (output != null) {
                output.println("DISCONNECT");
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            statusLabel.setText("Disconnected");
            statusLabel.setForeground(Color.RED);

            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            verifyButton.setEnabled(false);
            overviewButton.setEnabled(false);

            tableModel.setRowCount(0);
            overviewArea.setText("");

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error disconnecting: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InventoryClient clientGUI = new InventoryClient();
            clientGUI.setVisible(true);
        });
    }

}
