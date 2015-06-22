package cn.paxos.bolt;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.LinkedList;
import java.util.List;

import cn.paxos.bolt.responder.Responder;
import cn.paxos.bolt.state.RequestDispatcher;
import cn.paxos.jam.Event;
import cn.paxos.jam.State;
import cn.paxos.jam.StateContext;
import cn.paxos.jam.Trigger;
import cn.paxos.jam.preset.http.request.event.RequestCompletedEvent;
import cn.paxos.jam.preset.http.request.state.MethodState;
import cn.paxos.jam.state.BytesState;

public class Session implements State
{

  private final AsynchronousSocketChannel asynchronousSocketChannel;
  private final ByteBuffer buffer;
  private final StateContext stateContext;
  
  private boolean invalidated;

  public Session(AsynchronousSocketChannel asynchronousSocketChannel, ByteBuffer buffer, List<Responder> responders)
  {
    this.asynchronousSocketChannel = asynchronousSocketChannel;
    this.buffer = buffer;
    this.stateContext = new StateContext();
    List<Trigger> triggers = new LinkedList<Trigger>();
    triggers.add(new RequestDispatcher(asynchronousSocketChannel, responders));
    stateContext.setTriggers(triggers);
    stateContext.addState(new BytesState());
    stateContext.addState(new MethodState());
    stateContext.addState(this);
    stateContext.start();
    invalidated = false;
  }

  @Override
  public State onEvent(Event event, StateContext stateContext)
  {
    if (!(event instanceof RequestCompletedEvent))
    {
      return this;
    }
    this.invalidated = true;
    return null;
  }

  public AsynchronousSocketChannel getAsynchronousSocketChannel()
  {
    return asynchronousSocketChannel;
  }
  public ByteBuffer getBuffer()
  {
    return buffer;
  }
  public StateContext getStateContext()
  {
    return stateContext;
  }
  public boolean isInvalidated()
  {
    return invalidated;
  }

}
