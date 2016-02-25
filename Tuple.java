/*
* Ross Bryan &  Nate Lentz
*a Tuple class because Java doesnt support tuples lol
*/

public class Tuple {

  private String ip;
  private long ttl;

  public Tuple(String i, long t) {
    ip = i;
    ttl = t;
  }
  //@return the IP as a string
  public String getIP(){
    return ip;
  }
 //@ return the time to live as a long
  public long getTTL() {
    return ttl;
  }
//@return T/F if ip been cached > ttl
  public boolean checkTTL() {
    return (System.currentTimeMillis() > ttl);
  }
}
