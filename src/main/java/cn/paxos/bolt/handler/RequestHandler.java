package cn.paxos.bolt.handler;

import java.nio.channels.AsynchronousSocketChannel;

import cn.paxos.jam.preset.http.request.Request;

public interface RequestHandler
{
  
  boolean canHandle(Request request);
  
  void handle(Request request, AsynchronousSocketChannel asynchronousSocketChannel);

}
