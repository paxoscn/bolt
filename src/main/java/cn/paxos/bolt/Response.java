package cn.paxos.bolt;

import static cn.paxos.bolt.util.IOUtils.writeCompletely;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import cn.paxos.bolt.BodyWriter;
import cn.paxos.jam.util.LightByteArrayOutputStream;

public final class Response
{

  private static final DateFormat COOKIE_DATE_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH);

  private final String status;
  private final String contentType;
  private final long contentLength;
  private final String encoding;
  private final String location;
  private final boolean gzip;
  private final List<String> cookies;
  private final byte[] content;
  private final StringProvider stringProvider;
  private final BodyWriter bodyWriter;
  
  static
  {
    COOKIE_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  private Response(int code, String message)
  {
    this.status = code + " " + message;
    this.contentType = "text/html";
    this.contentLength = 0;
    this.encoding = "utf-8";
    this.location = null;
    this.gzip = false;
    this.cookies = new LinkedList<String>();
    this.content = null;
    this.stringProvider = null;
    this.bodyWriter = null;
  }

  private Response(String content, String encoding)
  {
    this.status = "200 OK";
    this.contentType = "text/html";
    this.encoding = encoding;
    this.location = null;
    this.gzip = false;
    this.cookies = new LinkedList<String>();
    try
    {
      this.content = content.getBytes(encoding);
    } catch (UnsupportedEncodingException e)
    {
      throw new IllegalArgumentException(e);
    }
    this.contentLength = this.content.length;
    this.stringProvider = null;
    this.bodyWriter = null;
  }

  private Response(StringProvider stringProvider)
  {
    this.status = "200 OK";
    this.contentType = null;
    this.contentLength = 0;
    this.encoding = null;
    this.location = null;
    this.gzip = false;
    this.cookies = new LinkedList<String>();
    this.content = null;
    this.stringProvider = stringProvider;
    this.bodyWriter = null;
  }

  private Response(boolean gzip, long contentLength, String contentType, BodyWriter bodyWriter)
  {
    this.status = "200 OK";
    this.contentType = contentType;
    this.contentLength = contentLength;
    this.encoding = null;
    this.location = null;
    this.gzip = gzip;
    this.cookies = new LinkedList<String>();
    this.content = null;
    this.stringProvider = null;
    this.bodyWriter = bodyWriter;
  }
  
  public Response(byte[] content, String contentType)
  {
    final LightByteArrayOutputStream baos = new LightByteArrayOutputStream();
    try
    {
      final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos);
      gzipOutputStream.write(content);
      gzipOutputStream.close();
    } catch (IOException e)
    {
      // TODO Auto-generated catch block
      throw new RuntimeException(e);
    }
    content = baos.toByteArray();
    this.status = "200 OK";
    this.contentType = contentType;
    this.contentLength = content.length;
    this.encoding = null;
    this.location = null;
    this.gzip = true;
    this.cookies = new LinkedList<String>();
    this.content = content;
    this.stringProvider = null;
    this.bodyWriter = null;
  }

  public Response(String path)
  {
    this.status = "301 Moved Permanently";
    this.contentType = "text/html";
    this.contentLength = 0;
    this.encoding = null;
    this.location = path;
    this.gzip = false;
    this.cookies = new LinkedList<String>();
    this.content = null;
    this.stringProvider = null;
    this.bodyWriter = null;
  }

  public static Response newErrorResponse(int code, String message)
  {
    return new Response(code, message);
  }
  
  public static Response newStringResponse(String content, String encoding)
  {
    return new Response(content, encoding);
  }
  
  public static Response newDelayedStringResponse(StringProvider stringProvider)
  {
    return new Response(stringProvider);
  }
  
  public static Response newWritingResponse(boolean gzip, long contentLength, String contentType, BodyWriter bodyWriter)
  {
    return new Response(gzip, contentLength, contentType, bodyWriter);
  }

  public static Response newBytesResponse(byte[] bytes, String contentType)
  {
    return new Response(bytes, contentType);
  }

  public static Response newRedirectResponse(String path)
  {
    return new Response(path);
  }
  
  public void addCookie(String key, String value, Date expire)
  {
    cookies.add(key + "=" + value + (expire == null ? "" : ("; expires=" + COOKIE_DATE_FORMAT.format(expire))) + "; path=/");
  }
  
  public void write(AsynchronousSocketChannel asynchronousSocketChannel)
  {
    if (stringProvider != null)
    {
      stringProvider.write(new AbstractStringResponse(asynchronousSocketChannel));
      return;
    }
    String head = "HTTP/1.1 ";
    head += status + "\r\n";
    head += "Content-Type: " + contentType;
    if (encoding != null)
    {
      head += "; charset=" + encoding;
    }
    head += "\r\n";
    if (location != null)
    {
      head += "Location: " + location + "\r\n";
    }
    if (gzip)
    {
      head += "Content-Encoding: gzip\r\n";
    }
    for (String cookie : cookies)
    {
      head += "Set-Cookie: " + cookie + "\r\n";
    }
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
      byte[] headAndBody = new byte[headBytes.length + content.length];
      System.arraycopy(headBytes, 0, headAndBody, 0, headBytes.length);
      System.arraycopy(content, 0, headAndBody, headBytes.length, content.length);
      writeCompletely(asynchronousSocketChannel, headAndBody);
      return;
    }
    ByteBuffer buffer = ByteBuffer.allocate(headBytes.length);
    buffer.put(headBytes);
    buffer.rewind();
    asynchronousSocketChannel.write(buffer, asynchronousSocketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>()
    {
      @Override
      public void completed(Integer result, final AsynchronousSocketChannel asynchronousSocketChannel)
      {
        try
        {
          bodyWriter.write(asynchronousSocketChannel);
        } catch (IOException e)
        {
          System.err.println("Writing error");
          e.printStackTrace();
          try
          {
            asynchronousSocketChannel.close();
          } catch (IOException e1)
          {
            System.err.println("Unwritable connection close failed");
            e1.printStackTrace();
          }
        }
      }
      @Override
      public void failed(Throwable exc, AsynchronousSocketChannel asynchronousSocketChannel)
      {
        System.err.println("Writing failed");
        exc.printStackTrace();
        try
        {
          asynchronousSocketChannel.close();
        } catch (IOException e)
        {
          System.err.println("Unwritable connection close failed");
          e.printStackTrace();
        }
      }
    });
  }

}
