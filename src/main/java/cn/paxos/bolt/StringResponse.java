package cn.paxos.bolt;

import java.util.Date;

public interface StringResponse
{
  
  void setStatus(String status);
  
  void setContentType(String contentType);
  
  void setEncoding(String encoding);
  
  void addCookie(String key, String value, Date expire);
  
  void setContent(byte[] content);
  
  void flush();

}
