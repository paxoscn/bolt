package cn.paxos.bolt.responder.preset;

import cn.paxos.bolt.Response;
import cn.paxos.bolt.responder.Responder;
import cn.paxos.jam.preset.http.request.Request;

public class DefaultResponder implements Responder
{

  @Override
  public boolean canHandle(Request request)
  {
    return true;
  }

  @Override
  public Response handle(Request request)
  {
    return Response.newErrorResponse(404, "Not Found");
  }

}
