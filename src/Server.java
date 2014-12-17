import java.io.*;
import java.net.*;
import java.util.*;

public class Server
{

    private ServerSocket serverSoc;
    private ServerThread serverTrd;
    private ArrayList<ClientThread> clients;
    private HashMap<String, String> account = new HashMap<String, String>();
    private HashMap<String, Integer> currentOnline = new HashMap<String, Integer>();
    private ArrayList<String> deniedUser = new ArrayList<String>();
    private ArrayList<String> deniedIp = new ArrayList<String>();

    private HashMap<String, String> blockList = new HashMap<String, String>();

    boolean isStart = false;

    public static void main(String[] args)
    {
        Server chatServer = new Server(args[0]);
    }

    private void LoadUserPass(HashMap<String, String> account)
    {
        try
        {
            FileInputStream fstream = new FileInputStream("../src/user_pass.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            while ((strLine = br.readLine()) != null)
            {
                if (strLine.split(" ").length == 2)
                {
                    account.put(strLine.split(" ")[0], strLine.split(" ")[1]);
                }
            }
            br.close();
        }
        catch (Exception e)
        {
            System.err.println("Error: " + e.getMessage());
        }
    }

    public Server(String port)
    {
        this.isStart = false;
        LoadUserPass(account);
        ServerStart(Integer.parseInt(port));

        BufferedReader serverCmd = new BufferedReader(new InputStreamReader(System.in));
        try
        {
            while (true)
            {
                String command = serverCmd.readLine();
                if (command.equals("shutdown"))
                {
                    ServerClose();
                    System.exit(0);
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void ServerStart(int port)
    {
        try
        {
            clients = new ArrayList<ClientThread>();
            serverSoc = new ServerSocket(port);
            serverTrd = new ServerThread(serverSoc);
            serverTrd.start();
            isStart = true;
        }
        catch (Exception e)
        {
            isStart = false;
            System.err.println("Error: " + e.getMessage());
            System.out.println("Start server failed.");
        }
    }

    @SuppressWarnings("deprecation")
    private void ServerClose()
    {
        try
        {
            if (serverTrd != null)
            {
                serverTrd.stop();
            }
            for (int i = clients.size() - 1; i >= 0; i--)
            {
                clients.get(i).GetWriter().println("kickout");
                clients.get(i).GetWriter().flush();
                clients.get(i).stop();
                clients.get(i).br.close();
                clients.get(i).pw.close();
                clients.get(i).clientSoc.close();
                clients.remove(i);
            }
            if (serverSoc != null)
            {
                serverSoc.close();
            }
            account.clear();
            blockList.clear();
            currentOnline.clear();
            deniedIp.clear();
            deniedUser.clear();
            isStart = false;
        }
        catch (IOException e)
        {
            System.err.println("Error: " + e.getMessage());
            isStart = true;
        }
    }

    class RefreshBlockList extends TimerTask
    {

        String block;

        public RefreshBlockList(String username)
        {
            block = username;
        }

        public void run()
        {
            blockList.remove(block);
        }
    }

    class ServerThread extends Thread
    {

        long BLOCK_TIME = 60 * 1000;
        private ServerSocket serverSoc;

        public ServerThread(ServerSocket serverSocket)
        {
            this.serverSoc = serverSocket;
        }

        @SuppressWarnings("deprecation")
        @Override
        public void run()
        {
            System.out.println("Server starts successfully. Listen for clients.");
            int isAuthrized;

            while (true)
            {
                try
                {
                    Socket loginRequire = serverSoc.accept();
                    BufferedReader loginBr = new BufferedReader(new InputStreamReader(loginRequire.getInputStream()));
                    PrintWriter loginPw = new PrintWriter(loginRequire.getOutputStream());
                    String loginInfo = loginBr.readLine();
                    StringTokenizer st = new StringTokenizer(loginInfo, "/");
                    String username = st.nextToken();
                    String password = st.nextToken();
                    String ip = st.nextToken();
                    User loginUser = new User(username, password, ip);

                    isAuthrized = Authentication(loginUser);

                    if (isAuthrized == 0)
                    {
                        loginPw.println("accepted");
                        loginPw.flush();
                        ClientThread clientTrd = new ClientThread(loginRequire, loginUser);
                        clientTrd.start();
                        clients.add(clientTrd);
                    }
                    else if (isAuthrized == -1)
                    {
                        loginPw.println("blocked");
                        loginPw.flush();
                        loginBr.close();
                        loginPw.close();
                        loginRequire.close();
                        System.out.println("User " + username + " has been blocked.");
                    }
                    else if (isAuthrized == 1)
                    {
                        loginPw.println("denied");
                        loginPw.flush();
                        loginBr.close();
                        loginPw.close();
                        loginRequire.close();
                        System.out.println("User " + username + " login failed.");
                    }
                    else if (isAuthrized == 2)
                    {
                        loginPw.println("duped");
                        loginPw.flush();
                        loginBr.close();
                        loginPw.close();
                        loginRequire.close();
                        System.out.println("User " + username + " is already online.");
                    }
                }
                catch (IOException e)
                {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }

        public int Authentication(User loginUser)
        {
            String username = loginUser.GetUsername();
            String password = loginUser.GetPassword();
            String ip = loginUser.GetIp();

            System.out.println("User " + username + " from " + ip + " tries to login.");
            if (deniedUser.size() >= 3)
            {
                if (deniedUser.get(0).equals(deniedUser.get(1)) && deniedUser.get(1).equals(deniedUser.get(2)))
                {
                    if (deniedIp.get(0).equals(deniedIp.get(1)) && deniedIp.get(1).equals(deniedIp.get(2)))
                    {

                        if (deniedUser.get(0).equals(username) && deniedIp.get(0).equals(ip))
                        {
                            blockList.put(username, ip);
                            Timer blockListTimer = new Timer();
                            RefreshBlockList Refresh = new RefreshBlockList(username);
                            blockListTimer.schedule(Refresh, BLOCK_TIME);
                        }
                        else
                        {
                            deniedUser.clear();
                            deniedIp.clear();
                        }
                    }
                    else
                    {
                        deniedUser.clear();
                        deniedIp.clear();
                    }
                }
                else
                {
                    deniedUser.clear();
                    deniedIp.clear();
                }
            }

            if (account.containsKey(username) == true)
            {
                if (blockList.containsKey(username) == false)
                {
                    if (currentOnline.containsKey(username) == false)
                    {
                        if (account.get(username).equals(password) == true)
                        {
                            deniedUser.clear();
                            deniedIp.clear();
                            currentOnline.put(username, 0);
                            return 0;
                        }
                        else
                        {
                            deniedUser.add(username);
                            deniedIp.add(ip);
                            return 1;
                        }
                    }
                    else
                    {
                        return 2;
                    }
                }
                else if (blockList.get(username).equals(ip) == false)
                {
                    if (currentOnline.containsKey(username) == false)
                    {
                        if (account.get(username).equals(password) == true)
                        {
                            deniedUser.clear();
                            deniedIp.clear();
                            currentOnline.put(username, 0);
                            return 0;
                        }
                        else
                        {
                            deniedUser.add(username);
                            deniedIp.add(ip);
                            return 1;
                        }
                    }
                    else
                    {
                        return 2;
                    }
                }
                else
                {
                    return -1;
                }
            }
            else
            {
                return 1;
            }
        }

    }

    class ClientThread extends Thread
    {

        private Socket clientSoc;
        private BufferedReader br;
        private PrintWriter pw;
        private User user;

        public ClientThread(Socket socket, User loginUser)
        {
            try
            {
                this.clientSoc = socket;
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pw = new PrintWriter(socket.getOutputStream());
                this.user = loginUser;
                pw.println("accepted");
                pw.flush();
                System.out.println("User " + user.GetUsername() + " login successful.");

            }
            catch (IOException e)
            {
                System.err.println("Error: " + e.getMessage());
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void run()
        {
            String message = "";

            while (true)
            {
                try
                {
                    message = br.readLine();
                    StringTokenizer st = new StringTokenizer(message);
                    String command = st.nextToken();
                    if (command.equals("whoelse"))
                    {
                        this.WhoElse();
                    }
                    else if (command.equals("wholasthr"))
                    {
                        this.WhoLastHour();
                    }
                    else if (command.equals("logout"))
                    {
                        pw.println("kickout");
                        pw.flush();

                        br.close();
                        pw.close();
                        clientSoc.close();
                        currentOnline.remove(user.GetUsername());
                        for (int i = clients.size() - 1; i >= 0; i--)
                        {
                            if (clients.get(i).GetUser() == user)
                            {

                                ClientThread temp = clients.get(i);
                                System.out.println("User " + clients.get(i).GetUser().GetUsername() + " logout");
                                clients.remove(i);
                                temp.stop();
                                return;
                            }
                        }
                    }
                    else if (command.equals("broadcast"))
                    {
                        String temp = "";
                        while (st.hasMoreTokens())
                        {
                            temp = temp + st.nextToken() + " ";
                        }
                        this.BroadcastMessage(temp);
                    }
                    else if (command.equals("message"))
                    {
                        String re = st.nextToken();
                        String temp = "";
                        while (st.hasMoreTokens())
                        {
                            temp = temp + st.nextToken() + " ";
                        }
                        this.WhisperMessage(re, temp);
                    }
                    else
                    {
                        pw.println("Invaild command.");
                        pw.flush();
                    }
                }
                catch (Exception e)
                {
                    System.err.println("Error: " + e.getMessage());
                    currentOnline.remove(user.GetUsername());
                    for (int i = clients.size() - 1; i >= 0; i--)
                    {
                        if (clients.get(i).GetUser() == user)
                        {

                            ClientThread temp = clients.get(i);
                            System.out.println("User " + clients.get(i).GetUser().GetUsername() + " disconnect");
                            clients.remove(i);
                            temp.stop();
                            return;
                        }
                    }
                }
            }
        }

        public void BroadcastMessage(String message)
        {
            pw.println("You broadcast: " + message);
            pw.flush();
            for (int i = clients.size() - 1; i >= 0; i--)
            {
                if (clients.get(i).GetUser().GetUsername().equals(user.GetUsername()) != true)
                {
                    clients.get(i).GetWriter().println(user.GetUsername() + " broadcasts: " + message);
                    clients.get(i).GetWriter().flush();
                }
            }
        }

        public void WhisperMessage(String receiver, String message)
        {
            for (int i = clients.size() - 1; i >= 0; i--)
            {
                if (clients.get(i).GetUser().GetUsername().equals(receiver))
                {
                    pw.println("You whisper to " + receiver + ": " + message);
                    pw.flush();
                    clients.get(i).GetWriter().println(user.GetUsername() + " whispers to you: " + message);
                    clients.get(i).GetWriter().flush();
                }
            }
        }

        public void WhoElse()
        {
            if (clients.size() > 1)
            {
                String temp = "";
                for (int i = clients.size() - 1; i >= 0; i--)
                {
                    if (clients.get(i).GetUser().GetUsername().equals(user.GetUsername()) != true)
                    {
                        temp += (clients.get(i).GetUser().GetUsername() + " ");
                    }
                }
                pw.println("Current online: " + temp);
                pw.flush();
            }
            else
            {
                pw.println("Only you are online.");
                pw.flush();
            }
        }

        public void WhoLastHour()
        {
            long LAST_HOUR = 3600;
            int count = 0;

            String temp = "";
            for (int i = clients.size() - 1; i >= 0; i--)
            {
                if (clients.get(i).GetUser().GetLoginPeriod() <= LAST_HOUR)
                {
                    temp += (clients.get(i).GetUser().GetUsername() + " ");
                    count++;
                }
            }

            if (count == 0)
            {
                pw.println("No user login within 1 hour.");
                pw.flush();
            }
            else
            {
                pw.println("Recent login user: " + temp);
                pw.flush();
            }
        }

        public BufferedReader GetReader()
        {
            return br;
        }

        public PrintWriter GetWriter()
        {
            return pw;
        }

        public User GetUser()
        {
            return user;
        }
    }

}
