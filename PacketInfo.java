import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.lang.*;
import java.util.ArrayList;

/**********************************************************************************
* Ross Bryan NAte Lentz
* CIS 457
* Project 2
*
* This class is a helper class that extracts info from Datagram Packets
*
**********************************************************************************/

class PacketInfo {

  /** bit masks */
  private static final int QR_FLAG = 0x8000;
  private static final int AA_FLAG = 0x0400;
  private static final int TC_FLAG = 0x0200;
  private static final int RD_FLAG = 0x0100;
  private static final int RA_FLAG = 0x0080;
  private static final int RCODE_FLAG = 0x000F;
  private static final short OK_RESPONSE_FLAGS = (short) 0x8500;

  /** packet information */
  private byte[] data;
  private int offset;
  private int length;

  /** header variables */
  private int id;

  private int flags;
  private int qr;
  private int opcode;
  private int aa;
  private int tc;
  private int rd;
  private int ra;
  private int z;
  private int rcode;

  private int qdcount;
  private int ancount;
  private int nscount;
  private int arcount;

  private boolean validQuestion = true;
  private String ipAddress = "";
  private ArrayList<String> authority = new ArrayList<>();
  private String nameRequested = "";
  private ArrayList<String> answerRecords = new ArrayList<>();
  private ArrayList<String> aNames = new ArrayList<>();
  private ArrayList<Long> ttls = new ArrayList<>();

  /*********************************************************************
  * Constructor that populates all instance variable fields with
  * information about the packet that was sent as a parameter
  *********************************************************************/
  public PacketInfo(DatagramPacket packet) {
    data = packet.getData();
    offset = packet.getOffset();
    length = packet.getLength();
    try {
      //read headers
      id = readUnsignedShort();
      flags = readUnsignedShort();
      qdcount = readUnsignedShort();
      ancount = readUnsignedShort();
      nscount = readUnsignedShort();
      arcount = readUnsignedShort();

      //apply masks to flag values
      if((flags & QR_FLAG) == QR_FLAG)
	qr = 1;
      else qr = 0;

      if((flags & AA_FLAG) == AA_FLAG)
	aa = 1;
      else aa = 0;

      if((flags & TC_FLAG) == TC_FLAG)
	tc = 1;
      else tc = 0;

      if((flags & RD_FLAG) == RD_FLAG)
	rd = 1;
      else rd = 0;

      if((flags & RA_FLAG) == RA_FLAG)
	ra = 1;
      else ra = 0;
    }
    catch(IOException e) {
      e.printStackTrace();
    }
  }

  /*****************************************************************
  * @returns 2 concatenated bytes as a 16bit int
  *****************************************************************/
  private int readUnsignedShort() throws IOException {
    return (get(offset++) << 8) + get(offset++);
  }

  /***********************************************************
  * @returns  1 byte as an unsigned int
  ***********************************************************/
  private int get(int offset) throws IOException {
        if ((offset < 0) || (offset >= length))
        {
            throw new IOException("offset out of range error: offset=" + offset);
        }
        return data[offset] & 0xFF;
  }

  /******************************************************************
  * @retruns a string of the parsed packet
  ******************************************************************/
  public String getQuestions() throws IOException {
    String qString = "";

    if(qdcount == 0)
      return "Packet contains no questions\n";

    //loop for every question record
    for(int i = 0; i < qdcount; i++) {
      qString += ("Question " + i + " record\n" + "Name: ");
      int count = data[offset++];
      qString += count;
      nameRequested += count;
      //loop through characters in qname record
      while(count != 0) {
	for(int j = 0; j < count; j++) {
	  char nextLetter = (char)data[offset++];
	  qString += nextLetter;
	  nameRequested += nextLetter;
	}
	count = data[offset++];
	qString += count;
	nameRequested += count;
      }

      //read qtype record
      int qtype = readUnsignedShort();
      qString += ("\n" + "Type: " + qtype);
      if(qtype != 1)
	     validQuestion = false;

      //read qclass record
      int qclass = readUnsignedShort();
      qString += ("\n" + "Class: " + qclass);
      if(qclass != 1)
	     validQuestion = false;
    }
    return qString;
  }

  /****************************************************************************************
  * @returns a string of a parsed packet
  ****************************************************************************************/
  String getResponse(int typeCount, boolean ansFlag, boolean fillAnswer) throws IOException{

    String aString = "";
    boolean pointerFlag = false;
    int pointerStart = 0;
    int count;

    //loop for every answer record
    for(int i = 0; i < typeCount; i++) {

      aString += ("\n\nRecord " + i + "\n" + "Name: ");

      //Check if name record contains a pointer instead of actual data
      if(((int)(data[offset] & 0xC0) == 0xC0)) {
	       count = 1;
      }
      else {
	       count = data[offset++];
	        aString += count;
      }

      /** loop through characters in qname record */
      String n = "";
      while(count != 0) {
	       for(int j = 0; j < count; j++) {

	          //Before each character read, check to see if next byte is pointer
	           if((int)(data[offset] & 0xC0) == 0xC0) {
	              pointerStart = readUnsignedShort();
	              pointerStart = (pointerStart ^ 0xC000);
	              aString += readPointer(pointerStart);
	              n += readPointer(pointerStart);
	           }

	            else {
	               char nextLetter;
	               nextLetter = (char)data[offset++];
	               aString += nextLetter;
	               n += nextLetter;
	            }
	       }
	       if((int)(data[offset] & 0xC0) == 0xC0)
	         count = 1;
	       else if(aString.charAt(aString.length() -1) == '0') {
	          count = 0;
	       } else {
	          count = data[offset++];
	          aString += count;
	          n += count;
	       }

    }
      //reset pointer flag
      pointerFlag = false;
      /** read type record */
      int qtype = readUnsignedShort();
      aString += ("\n" + "Type: " + qtype);

      /** read class record */
      int qclass = readUnsignedShort();
      aString += ("\n" + "Class: " + qclass);

      /** read ttl record */
      int firstHalfTTL = readUnsignedShort();
      int secondHalfTTL = readUnsignedShort();
      int ttl = ((firstHalfTTL << 16) | secondHalfTTL);
      aString += ("\n" + "TTL: " + ttl);
    //  System.out.println("TTL: " + ttl); seems to work?

      /** read rdlength record */
      int rdlength = readUnsignedShort();
      aString += ("\n" + "RDLENGTH: " + rdlength);

      /** read rdata record */
      aString += ("\n" + "RDATA: ");
      if(qtype == 1){
	       ipAddress = "";
         if(ansFlag){
		       aNames.add(n);
           ttls.add((long)ttl);
         }
      }

      if(ansFlag)
	count = rdlength;
      else {
	count = data[offset++];
	aString += count;
      }

      while(count != 0) {
	for(int j = 0; j < count; j++) {
	  if(ansFlag && qtype == 1) {
	    int nextNum;
	    nextNum = data[offset++];
	    nextNum = (nextNum & 0xFF);
	    aString += (nextNum + " ");
	    ipAddress += nextNum;
	    if(j < (rdlength -1))
	      ipAddress += ".";
	    else {
	      if(fillAnswer)
		      answerRecords.add(ipAddress);
	      authority.add(ipAddress);
	    }
	  }

	  else {
	    char nextLetter;
	    nextLetter = (char)data[offset++];
	    aString += nextLetter;
	  }

	  //check to see if next byte is pointer
	  if(!ansFlag && ((int)(data[offset] & 0xC0) == 0xC0)) {
	    pointerStart = readUnsignedShort();
	    pointerStart = (pointerStart ^ 0xC000);
	    aString += readPointer(pointerStart);
	    pointerFlag = true;
	  }
	}

	if(pointerFlag || ansFlag) {
	  count = 0;
	  pointerFlag = false;
	}
	else {
	  count = data[offset++];
	  aString += count;
	}
      }

    }
    aString += "\n";
    return aString;

  }

  /********************************************************************
  * @returns a string of info from ptr
  ********************************************************************/
  private String readPointer(int start) throws IOException{

    int startVal = start;
    String retVal = "";
    char nextLetter;
    int count = 0;
    int pointerStart;

    //Check if record contains a pointer instead of actual data
    if(((int)(data[startVal] & 0xC0) == 0xC0)) {
      int temp = offset;
      offset = startVal;
      pointerStart = readUnsignedShort();
      pointerStart = (pointerStart ^ 0xC000);
      offset = temp;
      retVal += readPointer(pointerStart);
    }
    else
      count = data[startVal++];
    retVal += count;

    //read record contained in pointer
    while(count != 0) {
	for(int j = 0; j < count; j++) {

	  nextLetter = (char)data[startVal++];
	  retVal += nextLetter;

	  //After each character read, check to see if next byte is pointer
	  if((int)(data[startVal] & 0xC0) == 0xC0) {
	    int temp2 = offset;
	    offset = startVal;
	    pointerStart = readUnsignedShort();
	    pointerStart = (pointerStart ^ 0xC000);
	    offset = temp2;
	    retVal += readPointer(pointerStart);
	  }
	}

	if(retVal.charAt(retVal.length() -1) == '0') {
	  count = 0;
	}
	else {
	  count = data[startVal++];
	  retVal += count;
	}
    }
    return retVal;
  }

  /******************************************************************
  *@returns a string of answers packet contained
  ******************************************************************/
  public String getAnswers() throws IOException{
    if(ancount == 0)
      return "Packet contains no answer records\n";
    else {
      return ("Answer\n" + "*********\n" + getResponse(ancount, true, true));
    }
  }

  /***********************************************************************
  * @returns a string of authority records
  ***********************************************************************/
  public String getAuthority() throws IOException{

    if(nscount == 0)
      return "Packet contains no authority records\n";
    else
      return ("Authority\n" + "*********\n" + getResponse(nscount, false, false));
  }

  /******************************************************************
  * @returns a string of additional records
  ******************************************************************/
  public String getAdditional() throws IOException{

    if(arcount == 0)
      return "Packet contains no Additional records\n";
    else
      return ("Additional\n" + "*********\n" + getResponse(arcount, true, false));
  }

  /******************************************************************
  * @return T/F is packet has an answer
  ******************************************************************/
  public boolean isAnswer() {
    return(ancount >= 1);
  }

  /*****************************************************************
  * @return T/F if there is an error
  *****************************************************************/
  public boolean isError() {
    return(rcode >= 1);
  }

  /*****************************************************************
  *@return IP address as string
  *****************************************************************/
  public String nextIP() {
    return ipAddress;
  }

  /****************************************************************
  * @returns an ArrayList of ip addresses we found
  ****************************************************************/
  public ArrayList<String> getResults() {
    return authority;
  }

  /****************************************************************
  *@return an arraylist of the nameservers
  ****************************************************************/
  public ArrayList<String> getNames() {
    return aNames;
  }

  /***************************************************************
  * @return an arraylist of the time to live
  ***************************************************************/
  public ArrayList<Long> getTTL() {
    return ttls;
  }

  /***************************************************************
  * @return the website requested
  ***************************************************************/
  public String getNameRequested() {
    return nameRequested;
  }

  /**************************************************************
  * @return ArrayList of the answers
  ***************************************************************/
  public ArrayList<String> getAnswerRecords() {
    return answerRecords;
  }

  /**************************************************
  * @return T/F if valid question
  **************************************************/
  public boolean getValidQuestion() {

    return validQuestion;
  }

  /******************************************************
  * @return the byte array
  ******************************************************/
  public byte[] getByteArray() {

    return Arrays.copyOfRange(data, 0, offset);
  }

  /******************************************************
  * Unsets recursion desired bit flag in data array
  ******************************************************/
  public void unsetRecursion() {

    data[2] = (byte)(data[2] ^ 0x01);
    rd = 0;
  }

  /*****************************************************
  * Sets rcode error flag to show unsupported query
  *****************************************************/
  public void setErrorCode() {
    data[3] = (byte)(data[3] ^ 0x04);
    rcode = 4;
  }

  /******************************************************************
  *@returns packet information as a string.
  *******************************************************************/
  public String getValues() {
    return ("" + "ID: " + id + "\nFlags: " + flags +
      "\n\tQR: " + qr + "\n\tAA: " + aa + "\n\tTC: " + tc + "\n\tRD: "
      + rd + "\n\tRA: " + ra + "\n\tRCODE: " + rcode + "\nQDCOUNT: " + qdcount +
      "\nANCOUNT: " + ancount + "\nNSCOUNT: " + nscount + "\nARCOUNT: " +
      arcount + "\nCurrent Offset: " + offset + "\n");
  }

  /******************************************************************
  * Sets Answers
  *******************************************************************/
  public void setAnswer(String name, InetAddress ip) {
    setResponseFlags(OK_RESPONSE_FLAGS);
    setAnsCount((short)1);
    int answerPos = length;
    int typePos = insertString(name, answerPos);
    //set type
    setShort(typePos, (short) 0x0001);
    //set class
    setShort(typePos + 2, (short) 0x0001);
    //set TTL
    setTTL(typePos, 0x00015180);
    //set response data length
    setShort(typePos + 8, (short) 0x0004);
    //set response data
    setData(typePos, ip);

    //update Length
    length = typePos + 13 + 1;
  }

  /******************************************************************
  * Changes ith byte in buffer to val.
  *******************************************************************/
  private void setIByte(int i, byte val) {
      data[i] = val;
  }

  /******************************************************************
  * Changes ith and i+1 byte to short
  *******************************************************************/
  private void setShort(int i, short s) {
    setIByte(i, getHighByte(s));
    setIByte(i + 1, getLowByte(s));
  }

  /******************************************************************
  * Returns low byte in short
  *******************************************************************/
  private byte getLowByte(short s) {

    return (byte) (s & 0xFF);
  }

  /******************************************************************
  * @returns high byte in short
  *******************************************************************/
  private byte getHighByte(short s) {
    return (byte) ((s >>> 8) & 0xFF);
  }

  /******************************************************************
  * Sets resopnse flags.
  *******************************************************************/
  private void setResponseFlags(short s) {
    setShort(2, s);
  }

  /******************************************************************
  * Sets Answer Count
  *******************************************************************/
  private void setAnsCount(short s) {
    setShort(6, s);
  }

  /******************************************************************
  * @returns the position of the next byte after the copy.
  *******************************************************************/
  private int insertString(String s, int pos) {

    byte[] b = s.getBytes();

    for(int i = 0; i < b.length; i++)
      setIByte(pos + i, b[i]);

    return (pos + b.length + 1);
  }

  /******************************************************************
  * Copies the string into the buffer at the given position, returns the
  * position of the next byte after the copy.
  *******************************************************************/
  private void setTTL(int pos, int ttl) {
    setShort(pos + 4, (short) ((ttl >>> 16) & 0xFFFF));
    setShort(pos + 6, (short) (ttl & 0xFFFF));
  }

  /******************************************************************
  * Copies the ip address
  *******************************************************************/
  private void setData(int pos, InetAddress ip) {
    byte[] addr = new byte[4];
    addr = ip.getAddress();
    for(int i = 0; i < addr.length; i++)
      setIByte(pos + 10 + i, addr[i]);
  }


}
