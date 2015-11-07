package cn.paxos.bolt;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.List;

import cn.paxos.bolt.responder.Responder;

public class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, Object>
{

  private final int bufferSize;
  private final AsynchronousServerSocketChannel serverSocketChannel;
  private final List<Responder> responders;

  public AcceptCompletionHandler(int bufferSize,
      AsynchronousServerSocketChannel serverSocketChannel,
      List<Responder> responders)
  {
    this.bufferSize = bufferSize;
    this.serverSocketChannel = serverSocketChannel;
    this.responders = responders;
  }

  @Override
  public void completed(AsynchronousSocketChannel asynchronousSocketChannel, Object attachment)
  {
    Session session = new Session(asynchronousSocketChannel, ByteBuffer.allocate(bufferSize), responders);
    asynchronousSocketChannel.read(session.getBuffer(), session, new ReadCompletionHandler(bufferSize, responders));
    serverSocketChannel.accept(null, this);
  }

  @Override
  public void failed(Throwable exc, Object attachment)
  {
    System.err.println("Accept failed");
    exc.printStackTrace();
  }
  
}