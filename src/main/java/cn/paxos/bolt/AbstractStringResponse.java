package cn.paxos.bolt;

import static cn.paxos.bolt.util.IOUtils.writeCompletely;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

public class AbstractStringResponse implements StringResponse
{

  private static final DateFormat COOKIE_DATE_FORMAT = new SimpleDateFormat("d MMM yyyy HH:mm:ss z");

  private final AsynchronousSocketChannel asynchronousSocketChannel;
  
  private String status;
  private String contentType;
  private String encoding;
  private List<String> cookies;
  private byte[] content;
  
  static
  {
    COOKIE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  public AbstractStringResponse(
      AsynchronousSocketChannel asynchronousSocketChannel)
  {
    this.asynchronousSocketChannel = asynchronousSocketChannel;
    this.status = "200 OK";
    this.contentType = "text/html";
    this.encoding = "utf-8";
    this.cookies = new LinkedList<String>();
    this.content = new byte[0];
  }

  @Override
  public void setStatus(String status)
  {
    this.status = status;
  }

  @Override
  public void setContentType(String contentType)
  {
    this.contentType = contentType;
  }

  @Override
  public void setEncoding(String encoding)
  {
    this.encoding = encoding;
  }

  @Override
  public void addCookie(String key, String value, Date expire)
  {
    cookies.add(key + "=" + value + "; expires=" + COOKIE_DATE_FORMAT.format(expire) + "; path=/");
  }

  @Override
  public void setContent(byte[] content)
  {
    this.content = content;
  }

  @Override
  public void flush()
  {
    String head = "HTTP/1.1 ";
    head += status + "\r\n";
    head += "Content-Type: " + contentType;
    if (encoding != null)
    {
      head += "; charset=" + encoding;
    }
    head += "\r\n";
    for (String cookie : cookies)
    {
      head += "Set-Cookie: " + cookie + "\r\n";
    }
    int contentLength = content.length;
    head += "Content-Length: " + contentLength + "\r\n\r\n";
    byte[] headBytes = head.getBytes();
    if (contentLength < 1)
    {
      ByteBuffer buffer = ByteBuffer.allocate(headBytes.length);
      buffer.put(headBytes);
      buffer.rewind();
      asynchronousSocketChannel.write(buffer);
      return;
    }
    if (content != null)
    {
      // TODO Write head then body ?
      byte[] headAndBody = new byte[headBytes.length + content.length];
      System.arraycopy(headBytes, 0, headAndBody, 0, headBytes.length);
      System.arraycopy(content, 0, headAndBody, headBytes.length, content.length);
      writeCompletely(asynchronousSocketChannel, headAndBody);
      return;
    }
  }

  public static void main(String[] args)
  {
    System.out.println(COOKIE_DATE_FORMAT.format(new Date()));
  }

}
