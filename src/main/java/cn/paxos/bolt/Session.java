package cn.paxos.bolt;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.LinkedList;
import java.util.List;

import cn.paxos.bolt.handler.RequestHandler;
import cn.paxos.bolt.state.RequestDispatcher;
import cn.paxos.jam.StateContext;
import cn.paxos.jam.Trigger;
import cn.paxos.jam.preset.http.request.state.MethodState;
import cn.paxos.jam.state.BytesState;

public class Session
{

  private final AsynchronousSocketChannel asynchronousSocketChannel;
  private final ByteBuffer buffer;
  private final StateContext stateContext;

  public Session(AsynchronousSocketChannel asynchronousSocketChannel, ByteBuffer buffer, List<RequestHandler> requestHandlers)
  {
    this.asynchronousSocketChannel = asynchronousSocketChannel;
    this.buffer = buffer;
    this.stateContext = new StateContext();
    List<Trigger> triggers = new LinkedList<Trigger>();
    triggers.add(new RequestDispatcher(asynchronousSocketChannel, requestHandlers));
    stateContext.setTriggers(triggers);
    stateContext.addState(new BytesState());
    stateContext.addState(new MethodState());
    stateContext.start();
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

}
