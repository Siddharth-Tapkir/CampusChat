
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ChatClient extends JFrame
{
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final String DEFAULT_PORT = "5000";
    private static final int MAX_MESSAGE_LENGTH = 500;

    private JTextField hostField;
    private JTextField portField;
    private JTextField usernameField;
    private JTextField messageField;

    private JButton connectButton;
    private JButton sendButton;

    private JTextArea chatArea;
    private DefaultListModel<String> userModel;
    private JList<String> usersList;

    private JLabel userCount;
    private JLabel statusLabel;

    private Socket clientSocket;
    private BufferedReader br;
    private PrintWriter pw;

    private volatile boolean bConnected = false;
    private volatile boolean bClosing = false;

    private String strUsername = "";

    private final Gson gson = new Gson();
    private final Object sendLock = new Object();

    public ChatClient()
    {
        setTitle("Campus Chat");
        setSize(800, 570);
        setMinimumSize(new Dimension(650, 450));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        buildUI();

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                closeClient();
            }
        });
    }



private void buildUI()
{
    Color objBackground = new Color(15, 23, 42);
    Color objWhite = new Color(30, 41, 59);
    Color objNavy = new Color(15, 23, 42);
    Color objBlue = new Color(99, 102, 241);
    Color objText = new Color(241, 245, 249);
    Color objMuted = new Color(148, 163, 184);
    Color objBorder = new Color(51, 65, 85);
    Color objInput = new Color(17, 24, 39);

    getContentPane().setBackground(objBackground);
    setLayout(new BorderLayout(0, 0));

    // ================= HEADER =================

    JPanel objHeader = new JPanel(new BorderLayout(15, 0));
    objHeader.setBackground(objNavy);
    objHeader.setBorder(new EmptyBorder(18, 24, 18, 24));

    JPanel objBrand = new JPanel(new GridLayout(2, 1, 0, 4));
    objBrand.setOpaque(false);

    JLabel lblTitle = new JLabel("Campus Chat");
    lblTitle.setForeground(Color.WHITE);
    lblTitle.setFont(new Font("SansSerif", Font.BOLD, 23));

    JLabel lblSubtitle = new JLabel(
        "Connect. Communicate. Collaborate."
    );
    lblSubtitle.setForeground(new Color(203, 213, 225));
    lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));

    objBrand.add(lblTitle);
    objBrand.add(lblSubtitle);

    JLabel lblProtocol = new JLabel("  TCP  •  LIVE CHAT  ");
    lblProtocol.setForeground(new Color(224, 231, 255));
    lblProtocol.setFont(new Font("SansSerif", Font.BOLD, 11));
    lblProtocol.setOpaque(true);
    lblProtocol.setBackground(new Color(49, 46, 129));
    lblProtocol.setBorder(new EmptyBorder(9, 10, 9, 10));

    objHeader.add(objBrand, BorderLayout.WEST);
    objHeader.add(lblProtocol, BorderLayout.EAST);

    add(objHeader, BorderLayout.NORTH);

    // ================= CONNECTION PANEL =================

    JPanel objConnection = new JPanel(
        new FlowLayout(FlowLayout.LEFT, 10, 10)
    );
    objConnection.setBackground(objWhite);
    objConnection.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(objBorder),
            new EmptyBorder(4, 10, 4, 10)
        )
    );

    hostField = new JTextField(DEFAULT_HOST, 11);
    portField = new JTextField(DEFAULT_PORT, 5);
    usernameField = new JTextField(11);

    styleField(hostField);
    styleField(portField);
    styleField(usernameField);

    connectButton = new JButton("Connect");
    styleButton(connectButton, objBlue);

    JLabel lblServer = createLabel("SERVER IP", objMuted);
    JLabel lblPort = createLabel("PORT", objMuted);
    JLabel lblUsername = createLabel("USERNAME", objMuted);

    objConnection.add(lblServer);
    objConnection.add(hostField);
    objConnection.add(lblPort);
    objConnection.add(portField);
    objConnection.add(lblUsername);
    objConnection.add(usernameField);
    objConnection.add(connectButton);

    // ================= CHAT AREA =================

    JPanel objMain = new JPanel(new BorderLayout(12, 0));
    objMain.setOpaque(false);
    objMain.setBorder(new EmptyBorder(14, 18, 10, 18));

    // Messages panel
    JPanel objMessages = new JPanel(new BorderLayout(0, 8));
    objMessages.setBackground(objWhite);
    objMessages.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(objBorder),
            new EmptyBorder(14, 14, 14, 14)
        )
    );

    JLabel lblMessages = new JLabel("Messages");
    lblMessages.setFont(new Font("SansSerif", Font.BOLD, 16));
    lblMessages.setForeground(objText);

    chatArea = new JTextArea();
    chatArea.setEditable(false);
    chatArea.setLineWrap(true);
    chatArea.setWrapStyleWord(true);
    chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
    chatArea.setBackground(objInput);
    chatArea.setForeground(objText);
    chatArea.setSelectedTextColor(Color.WHITE);
    chatArea.setSelectionColor(new Color(67, 56, 202));
    chatArea.setBorder(new EmptyBorder(12, 12, 12, 12));
    chatArea.setCaretColor(objBlue);

    JScrollPane objScroll = new JScrollPane(chatArea);
    objScroll.setBorder(
        BorderFactory.createLineBorder(objBorder)
    );
    objScroll.getViewport().setBackground(objInput);
    objScroll.getVerticalScrollBar().setUnitIncrement(16);

    objMessages.add(lblMessages, BorderLayout.NORTH);
    objMessages.add(objScroll, BorderLayout.CENTER);

    // ================= ONLINE USERS SIDEBAR =================

    JPanel objUsers = new JPanel(new BorderLayout(0, 10));
    objUsers.setBackground(objWhite);
    objUsers.setPreferredSize(new Dimension(185, 0));
    objUsers.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(objBorder),
            new EmptyBorder(14, 12, 12, 12)
        )
    );

    JLabel lblUsers = new JLabel("Online users");
    lblUsers.setFont(new Font("SansSerif", Font.BOLD, 15));
    lblUsers.setForeground(objText);

    userModel = new DefaultListModel<>();
    usersList = new JList<>(userModel);
    usersList.setFont(new Font("SansSerif", Font.PLAIN, 13));
    usersList.setForeground(objText);
    usersList.setBackground(objInput);
    usersList.setSelectionBackground(new Color(67, 56, 202));
    usersList.setSelectionForeground(Color.WHITE);
    usersList.setFixedCellHeight(32);
    usersList.setBorder(new EmptyBorder(4, 4, 4, 4));

    JScrollPane objUserScroll = new JScrollPane(usersList);
    objUserScroll.setBorder(
        BorderFactory.createLineBorder(objBorder)
    );
    objUserScroll.getViewport().setBackground(objInput);

    userCount = new JLabel("0 users online");
    userCount.setFont(new Font("SansSerif", Font.PLAIN, 12));
    userCount.setForeground(objMuted);
    userCount.setBorder(new EmptyBorder(5, 2, 0, 0));

    objUsers.add(lblUsers, BorderLayout.NORTH);
    objUsers.add(objUserScroll, BorderLayout.CENTER);
    objUsers.add(userCount, BorderLayout.SOUTH);

    objMain.add(objMessages, BorderLayout.CENTER);
    objMain.add(objUsers, BorderLayout.EAST);

    // ================= MESSAGE COMPOSER =================

    JPanel objCompose = new JPanel(new BorderLayout(10, 0));
    objCompose.setBackground(objWhite);
    objCompose.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(objBorder),
            new EmptyBorder(12, 12, 12, 12)
        )
    );

    messageField = new JTextField();
    messageField.setFont(new Font("SansSerif", Font.PLAIN, 14));
    messageField.setToolTipText(
        "Type your message and press Enter"
    );
    styleField(messageField);

    sendButton = new JButton("Send  ➤");
    styleButton(sendButton, objNavy);
    sendButton.setPreferredSize(new Dimension(105, 42));

    objCompose.add(messageField, BorderLayout.CENTER);
    objCompose.add(sendButton, BorderLayout.EAST);

    JPanel objBottom = new JPanel(new BorderLayout(0, 8));
    objBottom.setOpaque(false);
    objBottom.setBorder(new EmptyBorder(0, 18, 12, 18));
    objBottom.add(objCompose, BorderLayout.CENTER);

    // ================= STATUS BAR =================

    statusLabel = new JLabel("  ●  Disconnected");
    statusLabel.setOpaque(true);
    statusLabel.setBackground(new Color(30, 41, 59));
    statusLabel.setForeground(objMuted);
    statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
    statusLabel.setBorder(new EmptyBorder(9, 14, 9, 14));

    JPanel objCenter = new JPanel(new BorderLayout(0, 0));
    objCenter.setOpaque(false);
    objCenter.add(objConnection, BorderLayout.NORTH);
    objCenter.add(objMain, BorderLayout.CENTER);
    objCenter.add(objBottom, BorderLayout.SOUTH);

    add(objCenter, BorderLayout.CENTER);
    add(statusLabel, BorderLayout.SOUTH);

    // ================= EVENT HANDLERS =================

    connectButton.addActionListener(e -> connect());
    sendButton.addActionListener(e -> sendMessage());
    messageField.addActionListener(e -> sendMessage());

    setConnectedControls(false);
}

private JLabel createLabel(String strText, Color objColor)
{
    JLabel objLabel = new JLabel(strText);
    objLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
    objLabel.setForeground(objColor);
    return objLabel;
}

private void styleField(JTextField objField)
{
    objField.setFont(new Font("SansSerif", Font.PLAIN, 13));
    objField.setBackground(new Color(17, 24, 39));
    objField.setForeground(new Color(241, 245, 249));
    objField.setCaretColor(new Color(129, 140, 248));
    objField.setSelectionColor(new Color(67, 56, 202));
    objField.setSelectedTextColor(Color.WHITE);
    objField.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(71, 85, 105)),
            new EmptyBorder(8, 9, 8, 9)
        )
    );
}

private void styleButton(JButton objButton, Color objColor)
{
    objButton.setFont(new Font("SansSerif", Font.BOLD, 12));
    objButton.setForeground(Color.WHITE);
    objButton.setBackground(objColor);
    objButton.setFocusPainted(false);
    objButton.setCursor(
        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    );
    objButton.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(objColor),
            new EmptyBorder(8, 13, 8, 13)
        )
    );
    objButton.setPreferredSize(new Dimension(100, 38));
}

    private void appendMessage(String strMessage, Color objColor)
    {
        chatArea.setForeground(objColor);
        chatArea.append(strMessage + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void connect()
{
    if (bConnected)
    {
        return;
    }

    String strHost = hostField.getText().trim();
    String strPort = portField.getText().trim();
    String strName = usernameField.getText().trim();

    int iPort;

    try
    {
        iPort = Integer.parseInt(strPort);

        if (iPort < 1 || iPort > 65535)
        {
            throw new NumberFormatException();
        }
    }
    catch (NumberFormatException e)
    {
        JOptionPane.showMessageDialog(
            this,
            "Enter a port number between 1 and 65535.",
            "Invalid port",
            JOptionPane.ERROR_MESSAGE
        );
        return;
    }

    if (strHost.isEmpty())
    {
        JOptionPane.showMessageDialog(
            this, "Enter the server IP address."
        );
        return;
    }

    if (strName.isEmpty() || strName.length() > 20)
    {
        JOptionPane.showMessageDialog(
            this,
            "Username must be 1-20 characters.",
            "Invalid username",
            JOptionPane.ERROR_MESSAGE
        );
        return;
    }

    for (int iCnt = 0; iCnt < strName.length(); iCnt++)
    {
        if (Character.isISOControl(strName.charAt(iCnt)))
        {
            JOptionPane.showMessageDialog(
                this, "Username contains invalid characters."
            );
            return;
        }
    }

    connectButton.setEnabled(false);

    statusLabel.setText("  ●  Connecting...");
    statusLabel.setForeground(new Color(37, 99, 235));

    strUsername = strName;

    Thread objThread = new Thread(() ->
    {
        try
        {
            Socket objSocket = new Socket();

            objSocket.connect(
                new InetSocketAddress(strHost, iPort), 5000
            );

            BufferedReader objReader = new BufferedReader(
                new InputStreamReader(
                    objSocket.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );

            PrintWriter objWriter = new PrintWriter(
                new OutputStreamWriter(
                    objSocket.getOutputStream(),
                    StandardCharsets.UTF_8
                ),
                true
            );

            synchronized (sendLock)
            {
                if (bClosing)
                {
                    objSocket.close();
                    return;
                }

                clientSocket = objSocket;
                br = objReader;
                pw = objWriter;

                bConnected = true;

                Map<String, Object> objPacket = new HashMap<>();
                objPacket.put("type", "join");
                objPacket.put("username", strName);

                sendPacket(objPacket);
            }

            SwingUtilities.invokeLater(() ->
            {
                if (bClosing)
                {
                    return;
                }

                setConnectedControls(true);

                statusLabel.setText(
                    "  ●  Connected to " + strHost + ":" + iPort
                    + "  |  " + strName
                );

                statusLabel.setBackground(new Color(220, 252, 231));
                statusLabel.setForeground(new Color(22, 101, 52));

                appendMessage(
                    "Connecting to " + strHost + ":" + iPort + "...",
                    new Color(100, 116, 139)
                );

                messageField.requestFocusInWindow();
            });

            receiveLoop(objReader);
        }
        catch (IOException e)
        {
            SwingUtilities.invokeLater(() ->
            {
                if (!bClosing)
                {
                    disconnectUI();

                    JOptionPane.showMessageDialog(
                        this,
                        "Could not connect to the server.\n\n"
                        + e.getMessage(),
                        "Connection failed",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            });
        }
    });

    objThread.setDaemon(true);
    objThread.start();
}

    private void sendPacket(Map<String, Object> objPacket)
    {
        synchronized (sendLock)
        {
            if (pw == null || clientSocket == null
                || clientSocket.isClosed())
            {
                throw new IllegalStateException("Not connected.");
            }

            pw.println(gson.toJson(objPacket));

            if (pw.checkError())
            {
                throw new IllegalStateException("Could not send packet.");
            }
        }
    }

    private void sendMessage()
    {
        String strMessage = messageField.getText().trim();

        if (strMessage.isEmpty())
        {
            return;
        }

        if (!bConnected)
        {
            JOptionPane.showMessageDialog(
                this,
                "Connect to a chat server first.",
                "Not connected",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if (strMessage.length() > MAX_MESSAGE_LENGTH)
        {
            JOptionPane.showMessageDialog(
                this,
                "Messages can contain at most 500 characters.",
                "Message too long",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Map<String, Object> objPacket = new HashMap<>();
        objPacket.put("type", "chat");
        objPacket.put("message", strMessage);

        try
        {
            sendPacket(objPacket);
            messageField.setText("");
        }
        catch (RuntimeException e)
        {
            appendMessage(
                "Could not send message. Connection lost.",
                new Color(185, 28, 28)
            );

            disconnectUI();
        }
    }

    private void receiveLoop(BufferedReader objReader)
    {
        try
        {
            String strLine;

            while ((strLine = objReader.readLine()) != null)
            {
                if (strLine.isBlank())
                {
                    continue;
                }

                try
                {
                    JsonObject objJson =
                        JsonParser.parseString(strLine).getAsJsonObject();

                    Map<String, Object> objPacket =
                        gson.fromJson(
                            objJson,
                            new com.google.gson.reflect.TypeToken<
                                Map<String, Object>>() {}.getType()
                        );

                    SwingUtilities.invokeLater(
                        () -> processPacket(objPacket)
                    );
                }
                catch (RuntimeException e)
                {
                    Map<String, Object> objPacket = new HashMap<>();
                    objPacket.put("type", "error");
                    objPacket.put(
                        "message",
                        "Received an invalid server response."
                    );

                    SwingUtilities.invokeLater(
                        () -> processPacket(objPacket)
                    );
                }
            }
        }
        catch (IOException e)
        {
            // Connection closed.
        }
        finally
        {
            SwingUtilities.invokeLater(this::disconnectUI);
        }
    }

    private void processPacket(Map<String, Object> objPacket)
    {
        String strType = String.valueOf(
            objPacket.getOrDefault("type", "")
        );

        String strTime = String.valueOf(
            objPacket.getOrDefault("time", "")
        );

        String strMessage = String.valueOf(
            objPacket.getOrDefault("message", "")
        );

        switch (strType)
        {
            case "joined":
                appendMessage(
                    "[" + strTime + "] " + strMessage,
                    new Color(100, 116, 139)
                );
                break;

            case "chat":
            {
                String strName = String.valueOf(
                    objPacket.getOrDefault("username", "Unknown")
                );

                Color objColor = strName.equals(strUsername)
                    ? new Color(29, 78, 216)
                    : new Color(15, 118, 110);

                appendMessage(
                    "[" + strTime + "] " + strName + ": "
                    + strMessage,
                    objColor
                );
                break;
            }

            case "system":
                appendMessage(
                    "[" + strTime + "] * " + strMessage,
                    new Color(100, 116, 139)
                );
                break;

            case "error":
                appendMessage(
                    "[Server error] " + strMessage,
                    new Color(185, 28, 28)
                );
                break;

            case "users":
            {
                userModel.clear();

                Object objUsers = objPacket.get("users");

                if (objUsers instanceof List<?>)
                {
                    for (Object temp : (List<?>) objUsers)
                    {
                        userModel.addElement(String.valueOf(temp));
                    }
                }

                int iCount = userModel.size();

                userCount.setText(
                    iCount + (iCount == 1
                    ? " user online"
                    : " users online")
                );
                break;
            }

            default:
                break;
        }
    }

    private void setConnectedControls(boolean bState)
    {
        hostField.setEnabled(!bState);
        portField.setEnabled(!bState);
        usernameField.setEnabled(!bState);

        connectButton.setEnabled(!bState);
        connectButton.setText(bState ? "Connected" : "Connect");

        sendButton.setEnabled(bState);
        messageField.setEnabled(bState);
    }

    private void disconnectUI()
{
    bConnected = false;

    setConnectedControls(false);

    statusLabel.setText("  ●  Disconnected");
    statusLabel.setBackground(new Color(254, 226, 226));
    statusLabel.setForeground(new Color(153, 27, 27));

    appendMessage(
        "Disconnected from server.",
        new Color(153, 27, 27)
    );

    messageField.setText("");
}

    private void closeClient()
    {
        bClosing = true;
        bConnected = false;

        Socket objSocket = clientSocket;

        if (objSocket != null)
        {
            try
            {
                objSocket.close();
            }
            catch (IOException e)
            {
                // Socket already closed.
            }
        }

        dispose();
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() ->
        {
            ChatClient objClient = new ChatClient();
            objClient.setVisible(true);
        });
    }
}
