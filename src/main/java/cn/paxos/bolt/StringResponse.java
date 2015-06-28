package cn.paxos.bolt;

public interface StringResponse
{
  
  void setStatus(String status);
  
  void setContentType(String contentType);
  
  void setEncoding(String encoding);
  
  void setContent(byte[] content);
  
  void flush();

}
