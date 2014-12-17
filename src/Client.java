import java.io.*;
import java.net.*;
import java.util.*;

public class Client
{

    String serverIP;
    int port;
    String username;
    String password;
    private boolean isConnected = false;
    private Socket clientSoc;
    private PrintWriter pw;
    private BufferedReader br;
    private ReceiveThread receiveTrd;
    private SendThread sendTrd;
    Timer timer = new Timer();
    long TIME_OUT = 30 * 60000;

    public static void main(String[] args)
    {
        new Client(args[0], args[1]);
    }

    public Client(String serverIP, String port)
    {
        this.serverIP = serverIP;
        this.port = Integer.parseInt(port);
        Login();

    }

    private void Login()
    {
        Scanner login = new Scanner(System.in);
        while (isConnected == false)
        {
            do
            {
                System.out.print("Username: ");
                this.username = login.nextLine();
            }
            while (username.isEmpty());
            do
            {
                System.out.print("Password: ");
                this.password = login.nextLine();
            }
            while (password.isEmpty());
            isConnected = ConnectServer(serverIP, port, username, password);
            if (isConnected == true)
            {
                receiveTrd = new ReceiveThread(br);
                receiveTrd.start();
                sendTrd = new SendThread();
                sendTrd.start();
            }
        }
    }

    public boolean ConnectServer(String serverIP, int port, String username, String password)
    {
        try
        {
            clientSoc = new Socket(serverIP, port);
            pw = new PrintWriter(clientSoc.getOutputStream());
            br = new BufferedReader(new InputStreamReader(clientSoc.getInputStream()));
            //Send username, password, and user IP to server.
            SendMessage(username + "/" + password + clientSoc.getLocalAddress().toString());

            String response = br.readLine();
            if (response.equals("accepted"))
            {
                return true;
            }
            else if (response.equals("denied"))
            {
                System.out.println("Invaild username or password.");
                return false;
            }
            else if (response.equals("blocked"))
            {
                System.out.println("Blocked by the remote server.");
                return false;
            }
            else if (response.equals("duped"))
            {
                System.out.println("This username is already online.");
                return false;
            }
        }
        catch (Exception e)
        {
            System.out.println("Unable to connect to：" + serverIP + " Port：" + port);
            return false;
        }
        return false;
    }

    public void SendMessage(String message)
    {
        pw.println(message);
        pw.flush();

    }

    class SendThread extends Thread
    {

        AutoLogout autoLogout;

        public SendThread()
        {
            autoLogout = new AutoLogout();
            timer.schedule(autoLogout, TIME_OUT);
        }

        public void run()
        {
            String sendMessage = "";
            while (true)
            {
                BufferedReader sendBuff = new BufferedReader(new InputStreamReader(System.in));
                try
                {
                    while (true)
                    {
                        sendMessage = sendBuff.readLine();
                        SendMessage(sendMessage);
                        autoLogout.cancel();
                        autoLogout = new AutoLogout();
                        timer.schedule(autoLogout, TIME_OUT);
                        sleep(500);
                    }
                }
                catch (Exception e)
                {
                    System.err.println("Error: " + e.getMessage());
                }
            }

        }
    }

    public class AutoLogout extends TimerTask
    {

        public void run()
        {
            SendMessage("logout");
        }
    }

    class ReceiveThread extends Thread
    {

        private BufferedReader trdBr;

        public ReceiveThread(BufferedReader br)
        {
            this.trdBr = br;
        }

        public void KickOut() throws Exception
        {
            if (trdBr != null)
            {
                trdBr.close();
            }
            if (pw != null)
            {
                pw.close();
            }
            if (clientSoc != null)
            {
                clientSoc.close();
            }
            isConnected = false;
        }

        public void run()
        {
            String receiveMessage = "";
            while (true)
            {
                try
                {
                    receiveMessage = trdBr.readLine();

                    if (receiveMessage.equals("kickout"))
                    {
                        KickOut();
                        System.exit(0);
                    }
                    else if (receiveMessage.equals("accepted"))
                    {
                        System.out.println("Welcome to simple chat server!");
                    }
                    else
                    {
                        System.out.println(receiveMessage);
                    }
                }
                catch (Exception e)
                {
                    System.out.println("Remote server disconnected.");
                    System.exit(0);
                }
            }
        }
    }
}
