package cn.paxos.bolt;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

public interface BodyWriter
{
  
  void write(AsynchronousSocketChannel asynchronousSocketChannel) throws IOException;

}
