package cn.paxos.bolt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.List;
import java.util.concurrent.Executors;

import cn.paxos.bolt.responder.Responder;

public class Server
{
  
  public static final int BUFFER_SIZE = 16 * 1024;
  private static final int threadPoolSize = Runtime.getRuntime().availableProcessors();
  public static final AsynchronousChannelGroup asynchronousChannelGroup;
  
  private int port;
  private List<Responder> responders;
  
  static
  {
    try
    {
      asynchronousChannelGroup = AsynchronousChannelGroup.withCachedThreadPool(Executors.newCachedThreadPool(), threadPoolSize);
    } catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }
  
  public void start() throws IOException
  {
    AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel.open(asynchronousChannelGroup);
    serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
    serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, BUFFER_SIZE);
    serverSocketChannel.bind(new InetSocketAddress(port), 0);
    AcceptCompletionHandler acceptCompletionHandler = new AcceptCompletionHandler(BUFFER_SIZE, serverSocketChannel, responders);
    serverSocketChannel.accept(null, acceptCompletionHandler);
  }

  public void setPort(int port)
  {
    this.port = port;
  }

  public void setResponders(List<Responder> responders)
  {
    this.responders = responders;
  }

}
