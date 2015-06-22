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
  
  private static final int BUFFER_SIZE = 16 * 1024;
  private static final int threadPoolSize = Runtime.getRuntime().availableProcessors();

  private int port;
  private List<Responder> responders;
  
  public void start() throws IOException
  {
    AsynchronousChannelGroup asynchronousChannelGroup = AsynchronousChannelGroup.withCachedThreadPool(Executors.newCachedThreadPool(), threadPoolSize);
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
