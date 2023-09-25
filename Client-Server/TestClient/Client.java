import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class Client extends JFrame {
    private JButton sentserver;
    private JTextField textquery;
    private JButton close;
    private JLabel serverout;
    private JPanel contentpane;
    private final String SERVER_IP = "194.169.163.175";
    private final int SERVER_PORT = 8082;
    private final String TOKEN = "34928389310292351";

    public Client() {
        setContentPane(contentpane);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("JaVa");
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        sentserver.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openServer();
            }
        });

        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeApp();
            }
        });
    }

    private void openServer() {
        try (Socket clientSocket = new Socket(SERVER_IP, SERVER_PORT);

             OutputStream out = clientSocket.getOutputStream();
             DataOutputStream dataOut = new DataOutputStream(out);

             InputStream in = clientSocket.getInputStream();

             DataInputStream dataIn = new DataInputStream(in)) {

            // Отправка токена на сервер для аутентификации
            dataOut.writeUTF(TOKEN);

            // Отправка запроса на сервер
            String queryText = textquery.getText();
            dataOut.writeUTF(queryText);

            TimeUnit.MILLISECONDS.sleep(2000);

            // Получение ответа от сервера
            if (dataIn.available() > 0) {
                String response = dataIn.readUTF();
                serverout.setText(response);
            } else {
                // Обработка случая, когда данных нет
                serverout.setText("Сервер не отправил ответ.");
            }
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
        }
    }

    private void closeApp() {
        dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Client().setVisible(true);
            }
        });
    }
}
