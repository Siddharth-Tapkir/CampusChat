import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ChatServer
{
    private static final String HOST = "0.0.0.0";
    private static final int PORT = 5000;
    private static final int MAX_MESSAGE_LENGTH = 500;

    private final Map<Socket, ClientInfo> clients = new HashMap<>();
    private final Object lock = new Object();
    private final Gson gson = new Gson();

    private ServerSocket serverSocket;
    private volatile boolean bRunning = false;

    private static class ClientInfo
    {
        String username;
        String address;

        ClientInfo(String username, String address)
        {
            this.username = username;
            this.address = address;
        }
    }

    private String getTime()
    {
        return LocalTime.now().format(
            DateTimeFormatter.ofPattern("HH:mm:ss")
        );
    }

    private void sendJson(PrintWriter out, Map<String, Object> payload)
    {
        out.println(gson.toJson(payload));
    }

    private Map<String, Object> createPacket(String type)
    {
        Map<String, Object> packet = new HashMap<>();
        packet.put("type", type);
        packet.put("time", getTime());
        return packet;
    }

    private void sendError(PrintWriter out, String message)
    {
        Map<String, Object> packet = createPacket("error");
        packet.put("message", message);
        sendJson(out, packet);
    }

    private void broadcast(Map<String, Object> packet, Socket exclude)
    {
        List<Socket> objTargets = new ArrayList<>();

        synchronized (lock)
        {
            for (Socket temp : clients.keySet())
            {
                if (temp != exclude)
                {
                    objTargets.add(temp);
                }
            }
        }

        List<Socket> objDead = new ArrayList<>();

        for (Socket temp : objTargets)
        {
            try
            {
                PrintWriter out = new PrintWriter(temp.getOutputStream(), true);
                sendJson(out, packet);

                if (out.checkError())
                {
                    objDead.add(temp);
                }
            }
            catch (IOException e)
            {
                objDead.add(temp);
            }
        }

        for (Socket temp : objDead)
        {
            removeClient(temp, false);
        }
    }

    private void removeClient(Socket clientSocket, boolean bAnnounce)
    {
        ClientInfo objInfo;

        synchronized (lock)
        {
            objInfo = clients.remove(clientSocket);
        }

        try
        {
            clientSocket.close();
        }
        catch (IOException e)
        {
            // Socket is already closed.
        }

        if (objInfo != null && bAnnounce)
        {
            Map<String, Object> packet = createPacket("system");
            packet.put("message", objInfo.username + " left the chat.");
            broadcast(packet, null);
        }
    }

    private List<String> getUserList()
    {
        List<String> objUsers = new ArrayList<>();

        synchronized (lock)
        {
            for (ClientInfo temp : clients.values())
            {
                objUsers.add(temp.username);
            }
        }

        Collections.sort(objUsers, String.CASE_INSENSITIVE_ORDER);
        return objUsers;
    }

    private void broadcastUsers()
    {
        Map<String, Object> packet = createPacket("users");
        packet.put("users", getUserList());
        broadcast(packet, null);
    }

    private void handleClient(Socket clientSocket)
    {
        String username = null;

        try
        {
            BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    clientSocket.getInputStream(),
                    java.nio.charset.StandardCharsets.UTF_8
                )
            );

            PrintWriter out = new PrintWriter(
                clientSocket.getOutputStream(),
                true,
                java.nio.charset.StandardCharsets.UTF_8
            );

            String strLine;

            // First valid message must identify the user.
            while (bRunning && username == null)
            {
                strLine = br.readLine();

                if (strLine == null)
                {
                    return;
                }

                if (strLine.isBlank())
                {
                    continue;
                }

                JsonObject objPacket;

                try
                {
                    objPacket = JsonParser.parseString(strLine).getAsJsonObject();
                }
                catch (RuntimeException e)
                {
                    sendError(out, "Invalid message format.");
                    continue;
                }

                if (!objPacket.has("type")
                    || !objPacket.get("type").isJsonPrimitive()
                    || !"join".equals(objPacket.get("type").getAsString()))
                {
                    sendError(out, "Join before sending messages.");
                    continue;
                }

                String requested = "";

                if (objPacket.has("username")
                    && !objPacket.get("username").isJsonNull())
                {
                    requested = objPacket.get("username").getAsString().trim();
                }

                boolean bValid = !requested.isEmpty()
                    && requested.length() <= 20;

                for (int iCnt = 0; iCnt < requested.length(); iCnt++)
                {
                    if (Character.isISOControl(requested.charAt(iCnt)))
                    {
                        bValid = false;
                        break;
                    }
                }

                if (!bValid)
                {
                    sendError(out, "Username must be 1-20 characters.");
                    continue;
                }

                boolean bTaken = false;

                synchronized (lock)
                {
                    for (ClientInfo temp : clients.values())
                    {
                        if (temp.username.equalsIgnoreCase(requested))
                        {
                            bTaken = true;
                            break;
                        }
                    }

                    if (!bTaken)
                    {
                        clients.put(
                            clientSocket,
                            new ClientInfo(
                                requested,
                                clientSocket.getInetAddress().getHostAddress()
                            )
                        );
                    }
                }

                if (bTaken)
                {
                    sendError(out,
                        "That username is already in use. Choose another.");
                    continue;
                }

                username = requested;

                Map<String, Object> packet = createPacket("joined");
                packet.put("username", username);
                packet.put("message", "Welcome, " + username + "!");
                sendJson(out, packet);

                packet = createPacket("system");
                packet.put("message", username + " joined the chat.");
                broadcast(packet, clientSocket);

                broadcastUsers();
            }

            // Handle chat messages after joining.
            while (bRunning && username != null)
            {
                strLine = br.readLine();

                if (strLine == null)
                {
                    break;
                }

                if (strLine.isBlank())
                {
                    continue;
                }

                JsonObject objPacket;

                try
                {
                    objPacket = JsonParser.parseString(strLine).getAsJsonObject();
                }
                catch (RuntimeException e)
                {
                    sendError(out, "Invalid message format.");
                    continue;
                }

                if (!objPacket.has("type")
                    || !objPacket.get("type").isJsonPrimitive()
                    || !"chat".equals(objPacket.get("type").getAsString()))
                {
                    continue;
                }

                String strMessage = "";

                if (objPacket.has("message")
                    && !objPacket.get("message").isJsonNull())
                {
                    strMessage = objPacket.get("message").getAsString().trim();
                }

                if (strMessage.isEmpty())
                {
                    continue;
                }

                if (strMessage.length() > MAX_MESSAGE_LENGTH)
                {
                    sendError(out,
                        "Messages must be " + MAX_MESSAGE_LENGTH
                        + " characters or fewer.");
                    continue;
                }
                
                Map<String, Object> packet = createPacket("chat");
                packet.put("username", username);
                packet.put("message", strMessage);

                broadcast(packet, null);
            }
        }
        catch (SocketException e)
        {
            // Client disconnected or server is shutting down.
        }
        catch (IOException e)
        {
            // Connection error.
        }
        finally
        {
            boolean bWasMember = username != null;

            removeClient(clientSocket, bWasMember);

            if (bWasMember)
            {
                broadcastUsers();
            }
        }
    }

    public void serveForever(int iPort)
    {
        try
        {
            serverSocket = new ServerSocket(iPort);
            bRunning = true;

            System.out.println(
                "Campus Chat server listening on " + HOST + ":" + iPort
            );
            System.out.println("Press Ctrl+C to stop.");

            while (bRunning)
            {
                Socket clientSocket = serverSocket.accept();

                System.out.println(
                    "Connection from "
                    + clientSocket.getInetAddress().getHostAddress()
                    + ":" + clientSocket.getPort()
                );

                Thread objThread = new Thread(
                    () -> handleClient(clientSocket)
                );

                objThread.setDaemon(true);
                objThread.start();
            }
        }
        catch (SocketException e)
        {
            if (bRunning)
            {
                System.out.println("Socket error: " + e.getMessage());
            }
        }
        catch (IOException e)
        {
            System.out.println("Could not start server: " + e.getMessage());
            System.out.println(
                "Check whether the port is already in use."
            );
        }
        finally
        {
            bRunning = false;

            try
            {
                if (serverSocket != null && !serverSocket.isClosed())
                {
                    serverSocket.close();
                }
            }
            catch (IOException e)
            {
                // Server socket already closed.
            }

            List<Socket> objSockets;

            synchronized (lock)
            {
                objSockets = new ArrayList<>(clients.keySet());
            }

            for (Socket temp : objSockets)
            {
                removeClient(temp, false);
            }

            System.out.println("Server stopped.");
        }
    }

    public static void main(String[] args)
    {
        int iPort = PORT;

        if (args.length == 2 && args[0].equals("--port"))
        {
            try
            {
                iPort = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Invalid port number.");
                return;
            }
        }
        else if (args.length != 0)
        {
            System.out.println("Usage: java ChatServer [--port PORT]");
            return;
        }

        if (iPort < 1 || iPort > 65535)
        {
            System.out.println("Port must be between 1 and 65535.");
            return;
        }

        ChatServer objServer = new ChatServer();
        objServer.serveForever(iPort);
    }
}
