package cn.paxos.bolt.state;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;

import cn.paxos.bolt.handler.RequestHandler;
import cn.paxos.jam.Event;
import cn.paxos.jam.State;
import cn.paxos.jam.StateContext;
import cn.paxos.jam.Trigger;
import cn.paxos.jam.preset.http.request.Request;
import cn.paxos.jam.preset.http.request.event.RequestCompletedEvent;

public class RequestDispatcher implements Trigger, State
{
  
  private final AsynchronousSocketChannel asynchronousSocketChannel;
  private final List<RequestHandler> requestHandlers;

  public RequestDispatcher(AsynchronousSocketChannel asynchronousSocketChannel,
      List<RequestHandler> requestHandlers)
  {
    this.asynchronousSocketChannel = asynchronousSocketChannel;
    this.requestHandlers = requestHandlers;
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
    for (RequestHandler requestHandler : requestHandlers)
    {
      if (!requestHandler.canHandle(request))
      {
        continue;
      }
      requestHandler.handle(request, asynchronousSocketChannel);
      break;
    }
    return null;
  }
  
}