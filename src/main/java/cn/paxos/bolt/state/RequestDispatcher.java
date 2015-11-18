package cn.paxos.bolt.state;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;

import cn.paxos.bolt.Response;
import cn.paxos.bolt.responder.Responder;
import cn.paxos.bolt.responder.preset.DefaultResponder;
import cn.paxos.jam.Event;
import cn.paxos.jam.State;
import cn.paxos.jam.StateContext;
import cn.paxos.jam.Trigger;
import cn.paxos.jam.preset.http.request.Request;
import cn.paxos.jam.preset.http.request.event.RequestCompletedEvent;

public class RequestDispatcher implements Trigger, State
{
  
  private final AsynchronousSocketChannel asynchronousSocketChannel;
  private final List<Responder> responders;

  public RequestDispatcher(AsynchronousSocketChannel asynchronousSocketChannel,
      List<Responder> responders)
  {
    this.asynchronousSocketChannel = asynchronousSocketChannel;
    this.responders = responders;
  }

  @Override
  public State trigger(Event event)
  {
    if (event instanceof RequestCompletedEvent)
    {
      return this;
    }
    return null;
  }

  @Override
  public State onEvent(Event event, StateContext stateContext)
  {
    if (!(event instanceof RequestCompletedEvent))
    {
      return this;
    }
    Request request = ((RequestCompletedEvent) event).getRequest();
    System.out.println("path = " + request.getPath());
    Responder selectedResponder = new DefaultResponder();
    for (Responder responder : responders)
    {
      if (!responder.canHandle(request))
      {
        continue;
      }
      selectedResponder = responder;
      break;
    }
    Response response = selectedResponder.handle(request);
    response.write(asynchronousSocketChannel);
    return null;
  }
  
}