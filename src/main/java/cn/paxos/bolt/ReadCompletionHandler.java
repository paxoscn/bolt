package cn.paxos.bolt;

import java.io.IOException;
import java.nio.channels.CompletionHandler;
import java.util.List;

import cn.paxos.bolt.responder.Responder;
import cn.paxos.jam.event.BytesEvent;

public class ReadCompletionHandler implements CompletionHandler<Integer, Session>
{
  private final int bufferSize;
  private final List<Responder> responders;

  public ReadCompletionHandler(int bufferSize, List<Responder> responders)
  {
    this.bufferSize = bufferSize;
    this.responders = responders;
  }

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
    if (session.isInvalidated())
    {
      // Kept alive
      session = new Session(session.getAsynchronousSocketChannel(), session.getBuffer(), responders);
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
      session.getAsynchronousSocketChannel().read(session.getBuffer(), session, new ReadCompletionHandler(bufferSize, responders));
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