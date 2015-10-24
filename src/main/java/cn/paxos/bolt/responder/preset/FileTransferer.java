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

import cn.paxos.bolt.BodyWriter;
import cn.paxos.bolt.Response;
import cn.paxos.bolt.responder.Responder;
import cn.paxos.jam.preset.http.request.Request;

public class FileTransferer implements Responder
{
  
  private static final Map<String, String> mimeTypes;
  
  private final String base;
  
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
    String path = request.getPath();
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
    final long fileLength = file.length();
    int lastIndexOfDot = path.lastIndexOf('.');
    String ext = path.substring(lastIndexOfDot + 1);
    return Response.newWritingResponse(fileLength, mimeTypes.get(ext), new BodyWriter()
    {
      @Override
      public void write(final AsynchronousSocketChannel asynchronousSocketChannel)
          throws IOException
      {
        FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.READ);
        for (long transfered = 0; transfered < fileLength; )
        {
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

}
