package cn.paxos.bolt.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class IOUtils
{
  
  public static void writeCompletely(
      AsynchronousSocketChannel asynchronousSocketChannel, byte[] bytes)
  {
    writeLeft(asynchronousSocketChannel, bytes, 0);
  }
  
  private static void writeLeft(
      AsynchronousSocketChannel asynchronousSocketChannel, final byte[] bytes, final int offset)
  {
    final int toWrite = bytes.length - offset;
    ByteBuffer buffer = ByteBuffer.allocate(toWrite);
    buffer.put(bytes, offset, toWrite);
    buffer.rewind();
    asynchronousSocketChannel.write(buffer, asynchronousSocketChannel, new CompletionHandler<Integer, AsynchronousSocketChannel>()
    {
      @Override
      public void completed(Integer result, final AsynchronousSocketChannel asynchronousSocketChannel)
      {
        if (result < toWrite)
        {
          System.out.println("Left: " + (toWrite - result));
          writeLeft(asynchronousSocketChannel, bytes, offset + result);
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
