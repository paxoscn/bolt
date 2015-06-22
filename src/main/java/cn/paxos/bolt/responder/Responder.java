package cn.paxos.bolt.responder;

import cn.paxos.bolt.Response;
import cn.paxos.jam.preset.http.request.Request;

public interface Responder
{
  
  boolean canHandle(Request request);

  Response handle(Request request);

}
