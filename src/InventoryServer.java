import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class InventoryServer extends JFrame {
    private static final String NAME = "King-Michael Odo";
    private static final String STUDENT_ID = "22205122";

    // Port formula:
    // 5000 + (last 4 digits of student ID) % 1000
    // Student ID = 22205122
    // Last 4 digits = 5122
    // 5122 % 1000 = 122
    // 5000 + 122 = 5122
    private static final int DEFAULT_PORT = 5122;

    private static final String FOLDER_NAME = "InventoryFolder";
    private JTextField ipField;
    private JTextField portField;
    private JButton startButton;
    private JButton stopButton;
    private JButton clearLogButton;
    private JLabel statusLabel;
    private JTextArea logArea;

    private ServerSocket serverSocket;
    private boolean serverRunning = false;

    public InventoryServer()
    {
        setTitle("Inventory Server Project");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        buildGUI();
    }

    private void startServer()
    {
        try {
            int port = Integer.parseInt(portField.getText().trim());

            serverSocket = new ServerSocket(port);
            serverRunning = true;

            statusLabel.setText("Server running");
            statusLabel.setForeground(Color.GREEN);

            startButton.setEnabled(false);
            stopButton.setEnabled(true);

            log("Server started by " + NAME + " (ID: " + STUDENT_ID + ")");
            log("Server started on port " + port);

            Thread serverThread = new Thread(() -> listenForClients());
            serverThread.start();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid port number.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not start server: " + e.getMessage());
            log("Socket error: " + e.getMessage());
        }
    }

    public void buildGUI(){
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new GridLayout(3, 2, 10, 10));

        ipField = new JTextField(getLocalIPAddress());
        ipField.setEditable(false);

        portField = new JTextField(String.valueOf(DEFAULT_PORT));

        statusLabel = new JLabel("Server stopped");
        statusLabel.setForeground(Color.RED);

        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);

        topPanel.add(new JLabel("Server IP Address:"));
        topPanel.add(ipField);

        topPanel.add(new JLabel("Port:"));
        topPanel.add(portField);

        topPanel.add(new JLabel("Status:"));
        topPanel.add(statusLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout());

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);

        logArea = new JTextArea();
        logArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(logArea);

        clearLogButton = new JButton("Clear Log");

        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.WEST);
        mainPanel.add(clearLogButton, BorderLayout.SOUTH);

        add(mainPanel);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        clearLogButton.addActionListener(e -> logArea.setText(""));
    }

    private void listenForClients() {
        while (serverRunning) {
            try {
                Socket clientSocket = serverSocket.accept();

                log("Client connected from IP Address: " + clientSocket.getInetAddress().getHostAddress());

                Thread clientThread = new Thread(() -> handleClient(clientSocket));
                clientThread.start();

            } catch (IOException e) {
                if (serverRunning) {
                    log("Socket error while accepting client: " + e.getMessage());
                }
            }
        }
    }

    private void handleClient(Socket clientSocket)
    {
        try (
                BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String command;

            while ((command = input.readLine()) != null) {

                if (command.equals("LIST")) {
                    sendFileList(output);
                } else if (command.startsWith("GET_FILE:")) {
                    String fileName = command.substring("GET_FILE:".length());
                    sendFileContent(fileName, output);
                } else if (command.equals("OVERVIEW")) {
                    sendOverview(output);
                } else if (command.equals("VERIFY")) {
                    sendVerificationHash(output);
                } else if (command.equals("DISCONNECT")) {
                    log("Client disconnected.");
                    break;
                } else {
                    output.println("ERROR: Invalid request.");
                    log("Invalid request received: " + command);
                }
            }

        } catch (IOException e) {
            log("Client connection error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void sendFileList(PrintWriter output) {
        File inventoryFolder = new File(FOLDER_NAME);

        if (!inventoryFolder.exists()) {
            output.println("ERROR: Inventory folder was not found.");
            return;
        }

        File[] files = inventoryFolder.listFiles((folder, name) ->
                name.endsWith(".txt") && !name.equals("MergedInventory.txt")
        );

        if (files == null || files.length == 0) {
            output.println("ERROR: No inventory files found.");
            return;
        }

        log("Sending file list.");

        output.println("FILE_LIST_START");

        Arrays.sort(files);

        for (File file : files) {
            output.println(file.getName());
        }

        output.println("FILE_LIST_END");
    }
    private void sendFileContent(String fileName, PrintWriter output) {
        File file = new File(FOLDER_NAME, fileName);

        if (!file.exists()) {
            output.println("ERROR: File not found.");
            log("File not found: " + fileName);
            return;
        }

        log("Client requested file: " + fileName);

        output.println("FILE_CONTENT_START");

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                output.println(line);
            }

        } catch (IOException e) {
            output.println("ERROR: Could not read file.");
            log("Error reading file: " + fileName);
        }

        output.println("FILE_CONTENT_END");
    }
    private void sendOverview(PrintWriter output) {
        log("Client requested inventory overview.");

        String overview = MergeInventory.generateOverview();

        output.println("OVERVIEW_START");

        String[] lines = overview.split("\n");

        for (String line : lines) {
            output.println(line);
        }

        output.println("OVERVIEW_END");
    }

    private void sendVerificationHash(PrintWriter output) {
        int digitSum = 0;

        for (char digit : STUDENT_ID.toCharArray()) {
            digitSum += Character.getNumericValue(digit);
        }

        int activePort = Integer.parseInt(portField.getText().trim());

        int hash = (digitSum * activePort) % 1000;

        output.println("VERIFY_RESULT:" + hash);

        log("Verification hash sent to client: " + hash);
    }
    private void stopServer() {
        try {
            serverRunning = false;

            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }

            statusLabel.setText("Server stopped");
            statusLabel.setForeground(Color.RED);

            startButton.setEnabled(true);
            stopButton.setEnabled(false);

            log("Server stopped.");

        } catch (IOException e) {
            log("Error stopping server: " + e.getMessage());
        }
    }
    private String getLocalIPAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "Unable to detect IP";
        }
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            InventoryServer serverGUI = new InventoryServer();
            serverGUI.setVisible(true);
        });
    }

}

