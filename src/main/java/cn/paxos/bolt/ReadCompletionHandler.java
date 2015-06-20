package cn.paxos.bolt;

import java.io.IOException;
import java.nio.channels.CompletionHandler;

import cn.paxos.jam.event.BytesEvent;

public class ReadCompletionHandler implements CompletionHandler<Integer, Session>
{

  @Override
  public void completed(Integer read, Session session)
  {
    if (read < 0)
    {
      try
      {
        session.getAsynchronousSocketChannel().close();
      } catch (IOException e)
      {
        System.err.println("Close failed");
        e.printStackTrace();
      }
      return;
    }
    if (read > 0)
    {
      session.getBuffer().flip();
      byte[] b = new byte[session.getBuffer().remaining()];
      session.getBuffer().get(b);
      session.getStateContext().publish(new BytesEvent(b));
      session.getBuffer().rewind();
    }
    if (session.getAsynchronousSocketChannel().isOpen())
    {
      session.getAsynchronousSocketChannel().read(session.getBuffer(), session, new ReadCompletionHandler());
    }
  }

  @Override
  public void failed(Throwable exc, Session session)
  {
    System.err.println("Read failed");
    exc.printStackTrace();
    try
    {
      session.getAsynchronousSocketChannel().close();
    } catch (IOException e)
    {
      System.err.println("Unreadable connection close failed");
      e.printStackTrace();
    }
  }
  
}