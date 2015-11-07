package cn.paxos.bolt.responder.preset;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;

import cn.paxos.bolt.BodyWriter;
import cn.paxos.bolt.Response;
import cn.paxos.bolt.responder.Responder;
import cn.paxos.jam.preset.http.request.Request;
import cn.paxos.jam.util.LightByteArrayOutputStream;

public class FileTransferer implements Responder
{
  
  private static final Map<String, String> mimeTypes;

  private final String base;
  private final CookieAppender[] cookieAppenders;
  
  static
  {
    mimeTypes = new HashMap<String, String>();
    mimeTypes.put("htm", "text/html");
    mimeTypes.put("html", "text/html");
    mimeTypes.put("css", "text/css");
    mimeTypes.put("js", "application/x-javascript");
    mimeTypes.put("jpg", "image/jpeg");
    mimeTypes.put("png", "image/png");
    mimeTypes.put("gif", "image/gif");
    mimeTypes.put("ico", "image/x-icon");
  }

  public FileTransferer(String base, CookieAppender[] cookieAppenders)
  {
    this.base = base;
    this.cookieAppenders = cookieAppenders;
  }

  @Override
  public boolean canHandle(Request request)
  {
    if (!request.getMethod().toLowerCase().equals("get"))
    {
      return false;
    }
    String path = request.getPath();
    if (path.equals("/"))
    {
      return true;
    }
    int lastIndexOfDot = path.lastIndexOf('.');
    if (lastIndexOfDot < 0)
    {
      return false;
    }
    String ext = path.substring(lastIndexOfDot + 1);
    return ",htm,html,css,js,jpg,png,gif,ico,".indexOf("," + ext + ",") > -1;
  }

  @Override
  public Response handle(Request request)
  {
    System.out.println("handle");
    String path = request.getPath();
    System.out.println("path = " + path);
    if (path.equals("/"))
    {
      path = "/index.html";
    }
    String filePath = base + path;
    final File file = new File(filePath);
    if (!file.exists())
    {
      return Response.newErrorResponse(404, "Not Found");
    }
    System.out.println("file = " + file);
    final long fileLength = file.length();
    int lastIndexOfDot = path.lastIndexOf('.');
    String ext = path.substring(lastIndexOfDot + 1);
    final Response response;
    boolean gzip = true;
    if (gzip)
    {
      System.out.println("gzip");
      final LightByteArrayOutputStream baos = new LightByteArrayOutputStream();
      try
      {
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos);
        FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        for (long transfered = 0; transfered < fileLength; )
        {
          System.out.println("transfered = " + transfered);
          transfered += fileChannel.transferTo(transfered, fileLength - transfered, new WritableByteChannel() {
            @Override
            public boolean isOpen()
            {
              return true;
            }
            @Override
            public void close() throws IOException
            {
              throw new UnsupportedOperationException();
            }
            @Override
            public int write(ByteBuffer src) throws IOException
            {
              byte[] b = new byte[src.limit()];
              src.get(b);
              gzipOutputStream.write(b, 0, b.length);
              System.out.println("fileChannel write " + b.length);
              return b.length;
            }
          });
        }
        gzipOutputStream.close();
      } catch (IOException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
        return Response.newErrorResponse(500, "Internal Server Error");
      }
      final byte[] gzipped = baos.toByteArray();
      response = Response.newWritingResponse(true, gzipped.length, mimeTypes.get(ext), new BodyWriter()
      {
        @Override
        public void write(final AsynchronousSocketChannel asynchronousSocketChannel)
            throws IOException
        {
          asynchronousSocketChannel.write(ByteBuffer.wrap(gzipped));
        }
      });
    } else
    {
      System.out.println("non-gzip");
      response = Response.newWritingResponse(false, fileLength, mimeTypes.get(ext), new BodyWriter()
      {
        @Override
        public void write(final AsynchronousSocketChannel asynchronousSocketChannel)
            throws IOException
        {
          System.out.println("BodyWriter write");
          FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
          for (long transfered = 0; transfered < fileLength; )
          {
            System.out.println("transfered = " + transfered);
            transfered += fileChannel.transferTo(transfered, fileLength - transfered, new WritableByteChannel() {
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
          }
        }
      });
    }
    if (cookieAppenders != null)
    {
      for (CookieAppender cookieAppender : cookieAppenders)
      {
        if (cookieAppender.canHandle(request))
        {
          Map<String, String> newCookies = cookieAppender.appendCookies(request);
          if (newCookies != null)
          {
            for (String key : newCookies.keySet())
            {
              String value = newCookies.get(key);
              response.addCookie(key, value, null);
            }
          }
        }
      }
    }
    return response;
  }
  
  public static interface CookieAppender
  {
    
    boolean canHandle(Request request);

    Map<String, String> appendCookies(Request request);

  }

}
