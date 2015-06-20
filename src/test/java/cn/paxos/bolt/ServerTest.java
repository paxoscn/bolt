package cn.paxos.bolt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.junit.Test;

public class ServerTest
{

  @Test
  public void test() throws UnknownHostException, IOException
  {
    Socket s = new Socket("127.0.0.1", 8080);
    OutputStream os = s.getOutputStream();
    InputStream is = s.getInputStream();
    os.write("GET /abc?k=apple HTTP/1.1\r\nHost: 127.0.0.1:8080\r\nConnection: close\r\n\r\n".getBytes());
    byte[] buffer = new byte[4096];
    ByteArrayOutputStream o = new ByteArrayOutputStream();
    for (int read = -1; (read = is.read(buffer)) > 0;)
    {
      o.write(buffer, 0, read);
    }
    System.out.println(new String(o.toByteArray(), "UTF-8"));
  }

}
