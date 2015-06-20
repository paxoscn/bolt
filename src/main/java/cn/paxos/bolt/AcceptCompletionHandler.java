package cn.paxos.bolt;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.List;

import cn.paxos.bolt.handler.RequestHandler;

public class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, Object>
{

  private final int bufferSize;
  private final AsynchronousServerSocketChannel serverSocketChannel;
  private final List<RequestHandler> requestHandlers;

  public AcceptCompletionHandler(int bufferSize,
      AsynchronousServerSocketChannel serverSocketChannel,
      List<RequestHandler> requestHandlers)
  {
    this.bufferSize = bufferSize;
    this.serverSocketChannel = serverSocketChannel;
    this.requestHandlers = requestHandlers;
  }

  @Override
  public void completed(AsynchronousSocketChannel asynchronousSocketChannel, Object attachment)
  {
    Session session = new Session(asynchronousSocketChannel, ByteBuffer.allocate(bufferSize), requestHandlers);
    asynchronousSocketChannel.read(session.getBuffer(), session, new ReadCompletionHandler());
    serverSocketChannel.accept(null, this);
  }

  @Override
  public void failed(Throwable exc, Object attachment)
  {
    System.err.println("Accept failed");
    exc.printStackTrace();
  }
  
}