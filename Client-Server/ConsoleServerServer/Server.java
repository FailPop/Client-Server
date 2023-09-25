import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.logging.*;

public class Server {
    private static final String DB_URL = "jdbc:postgresql://194.169.163.175:5432/mvas";
    private static final String DB_USER = "mvas";
    private static final String DB_PASSWORD = "qwe123";
    private static final String TOKEN = "34928389310292351";
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
        int port = 8082;

        // Настройка обработчика логов для записи в файл
        try {
            FileHandler fileHandler = new FileHandler("logserver.txt");
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            LOGGER.addHandler(fileHandler);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error setting up file handler", e);
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOGGER.info("Server is listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("Client connected from " + clientSocket.getRemoteSocketAddress());

                // Создаем отдельный поток для каждого клиента
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error in server main", e);
        }
    }

    private record ClientHandler(Socket clientSocket) implements Runnable {

        @Override
            public void run() {
                try (InputStream in = clientSocket.getInputStream();
                     DataInputStream dataIn = new DataInputStream(in);
                     OutputStream out = clientSocket.getOutputStream();
                     DataOutputStream dataOut = new DataOutputStream(out);

                     Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

                    // Чтение токена от клиента
                    String clientToken = dataIn.readUTF();
                    LOGGER.info("Received token from client: " + clientToken);

                    // Проверка токена
                    if (!clientToken.equals(TOKEN)) {
                        LOGGER.warning("Invalid token. Closing connection.");
                        return;
                    }

                    // Чтение запроса от клиента
                    String clientQuery = dataIn.readUTF();
                    LOGGER.info("Received query from client: " + clientQuery);

                    int clientId;
                    try {
                        clientId = Integer.parseInt(clientQuery);
                    } catch (NumberFormatException e) {
                        LOGGER.warning("Invalid clientQuery. It should be an integer.");
                        return;
                    }

                    // Выполнение запроса к базе данных
                    String query = "SELECT text FROM jbd_tst WHERE id =?";

                    try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                        preparedStatement.setInt(1, clientId);
                        ResultSet rs = preparedStatement.executeQuery();

                        // Отправка результата клиенту
                        if (rs.next()) {
                            String result = rs.getString("text");
                            dataOut.writeUTF(result);
                            LOGGER.info("Sent response to client: " + result);
                        } else {
                            String noData = "No data found.";
                            dataOut.writeUTF(noData);
                            LOGGER.info("Sent response to client: " + noData);
                        }
                    } catch (SQLException e) {
                        LOGGER.log(Level.SEVERE, "Error executing database query", e);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error sending response to client", e);
                    }


                } catch (IOException | SQLException e) {
                    LOGGER.log(Level.SEVERE, "Error in client handler", e);
                } finally {
                    try {
                        clientSocket.close();
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error while closing client socket", e);
                    }
                }
            }
        }
}
