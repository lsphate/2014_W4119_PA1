import java.util.*;

public class User
{
    private String username;
    private String password;
    private String ip;
    private Timer autoLogoutTimer;
    private long loginTime;
    private int loginTries = 0;

    public User(String username, String password, String ip)
    {
        this.username = username;
        this.password = password;
        this.ip = ip;
        this.autoLogoutTimer = new Timer();
        this.loginTime = System.currentTimeMillis();
    }

    public void AddLoginTries()
    {
        loginTries++;
    }
    
    public int GetLoginTries()
    {
        return loginTries;
    }
    
    public void ResetLoginTries()
    {
        loginTries = 0;
    }
    
    public long GetLoginPeriod()
    {
        long currentTime = System.currentTimeMillis();
        long loginPeriod = (currentTime - loginTime) / 1000;
        return loginPeriod;
    }
    
    public void ResetLogoutTimer()
    {
        autoLogoutTimer.cancel();
        this.autoLogoutTimer = new Timer();
    }
    
    public String GetUsername()
    {
        return username;
    }

    public void SetUsername(String username)
    {
        this.username = username;
    }

    public String GetPassword()
    {
        return password;
    }

    public void SetPassword(String username)
    {
        this.username = username;
    }

    public String GetIp()
    {
        return ip;
    }

    public void SetIp(String ip)
    {
        this.ip = ip;
    }
}
