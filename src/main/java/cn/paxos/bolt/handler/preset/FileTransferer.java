package cn.paxos.bolt.handler.preset;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import cn.paxos.bolt.handler.RequestHandler;
import cn.paxos.jam.preset.http.request.Request;

public class FileTransferer implements RequestHandler
{
  
  private final String base;

  public FileTransferer(String base)
  {
    this.base = base;
  }

  @Override
  public boolean canHandle(Request request)
  {
    if (!request.getMethod().toLowerCase().equals("get"))
    {
      return false;
    }
    String path = request.getPath();
    int lastIndexOfDot = path.lastIndexOf('.');
    if (lastIndexOfDot < 0)
    {
      return false;
    }
    String ext = path.substring(lastIndexOfDot + 1);
    return ",htm,html,css,js,jpg,png,gif,".indexOf("," + ext + ",") > -1;
  }

  @Override
  public void handle(Request request,
      AsynchronousSocketChannel asynchronousSocketChannel)
  {
    String path = request.getPath();
    String filePath = base + path;
    final File file = new File(filePath);
    if (!file.exists())
    {
      System.err.println("UTF-8 Supported only");
      byte[] res = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".getBytes();
      ByteBuffer buffer = ByteBuffer.allocate(res.length);
      buffer.put(res);
      buffer.rewind();
      asynchronousSocketChannel.write(buffer);
      return;
    }
    final long fileLength = file.length();
    byte[] head = ("HTTP/1.1 200 OK\r\nContent-Length: " + fileLength + "\r\nConnection: close\r\n\r\n").getBytes();
    ByteBuffer buffer = ByteBuffer.allocate(head.length);
    buffer.put(head);
    buffer.rewind();
    asynchronousSocketChannel.write(buffer, asynchronousSocketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>()
    {
      @Override
      public void completed(Integer result, final AsynchronousSocketChannel asynchronousSocketChannel)
      {
        try
        {
          FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
          fileChannel.transferTo(0, fileLength, new WritableByteChannel() {
            @Override
            public boolean isOpen()
            {
              return asynchronousSocketChannel.isOpen();
            }
            @Override
            public void close() throws IOException
            {
              asynchronousSocketChannel.close();
            }
            @Override
            public int write(ByteBuffer src) throws IOException
            {
              Future<Integer> future = asynchronousSocketChannel.write(src);
              try
              {
                return future.get();
              } catch (InterruptedException e)
              {
                throw new IOException(e);
              } catch (ExecutionException e)
              {
                throw new IOException(e);
              }
            }
          });
        } catch (IOException e)
        {
          System.err.println("File Read failed");
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
        System.err.println("Write failed");
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
