package cn.paxos.bolt;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import cn.paxos.bolt.BodyWriter;

public final class Response
{

  private final String status;
  private final String contentType;
  private final long contentLength;
  private final String encoding;
  private final byte[] content;
  private final BodyWriter bodyWriter;

  private Response(int code, String message)
  {
    this.status = code + " " + message;
    this.contentType = "text/html";
    this.contentLength = 0;
    this.encoding = "utf-8";
    this.content = null;
    this.bodyWriter = null;
  }

  private Response(String content, String encoding)
  {
    this.status = "200 OK";
    this.contentType = "text/html";
    this.contentLength = 0;
    this.encoding = encoding;
    try
    {
      this.content = content.getBytes(encoding);
    } catch (UnsupportedEncodingException e)
    {
      throw new IllegalArgumentException(e);
    }
    this.bodyWriter = null;
  }

  private Response(long contentLength, String contentType, BodyWriter bodyWriter)
  {
    this.status = "200 OK";
    this.contentType = contentType;
    this.contentLength = contentLength;
    this.encoding = null;
    this.content = null;
    this.bodyWriter = bodyWriter;
  }
  
  public static Response newErrorResponse(int code, String message)
  {
    return new Response(code, message);
  }
  
  public static Response newStringResponse(String content, String encoding)
  {
    return new Response(content, encoding);
  }
  
  public static Response newWritingResponse(long contentLength, String contentType, BodyWriter bodyWriter)
  {
    return new Response(contentLength, contentType, bodyWriter);
  }
  
  public void write(AsynchronousSocketChannel asynchronousSocketChannel)
  {
    String head = "HTTP/1.1 ";
    head += status + "\r\n";
    head += "Content-Type: " + contentType;
    if (encoding != null)
    {
      head += "; charset=" + encoding;
    }
    head += "\r\n";
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
      ByteBuffer buffer = ByteBuffer.allocate(headAndBody.length);
      buffer.put(headAndBody);
      buffer.rewind();
      asynchronousSocketChannel.write(buffer);
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
