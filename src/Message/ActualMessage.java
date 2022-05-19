package Message;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import Message.BitField;
import Message.PayLoad;
//import Message.ActualMessage.MessageType;




public class ActualMessage {

    public int msgLength;
    public byte msgType;
    public PayLoad payLoad;
    
    
    public ActualMessage() {}
    
    public ActualMessage(int length, byte type){//MsgType type) {//
        msgLength = length;
        msgType = type;//Type = type;//
    }

    public ActualMessage(int length, byte type, PayLoad load){//MsgType type, PayLoad load){//
        msgLength = length;
        msgType = type;//Type = type;//
        payLoad = load;
    }

    public String Type(){
        String type = "";
        
        switch (msgType) {
            case 0 :
            	type = " choke";
            	break;
            case 1 :
            	type = " unchoke";
            	break;
            case 2 :
            	type = " interested";
            	break;
            case 3 :
            	type = " not interested";
            	break;
            case 4 :
            	type = " have";
            	break;
            case 5 :
            	type = " bitfield";
            	break;
            case 6 :
            	type = " request";
            	break;
            case 7 :
            	type = " piece";
            	break;
          
        }
        return type;
    }

    public String MsgInfo(){
        return "Length: " + msgLength + " Type: " + msgType + Type() + "\n" + "Payload:\n" + payLoad.PayLoadInfo();
    }

    public void WriteMSG(OutputStream outputStream) throws IOException {
        ByteArrayOutputStream bytesout = new ByteArrayOutputStream();
        DataOutputStream dataout = new DataOutputStream(bytesout);
        dataout.writeInt(msgLength);
        dataout.writeByte(msgType);//(byte)Type.ordinal());
        switch (msgType) {
            case 4:
            case 6:
                dataout.writeInt(payLoad.pieceIndex);
            	break;
            case 5:
            	dataout.write(payLoad.bitField.havePieces);
            	break;
            case 7:
            	{
            	//System.out.println("pieceIndex"+payLoad.pieceIndex);
                dataout.writeInt(payLoad.pieceIndex);
                //System.out.println("content"+payLoad.content);
                dataout.write(payLoad.content);
                
            	}
            	break;
            
        }
        synchronized(outputStream){
        	//System.out.println(bytesout.toByteArray());
            outputStream.write(bytesout.toByteArray());
        }
        bytesout.close();
        dataout.close();
    }

    public void ReceiveMSG(InputStream inputStream) throws InterruptedException, IOException {
        while(inputStream.available() < 5){
            Thread.sleep(500);
        }
        byte[] receive = new byte[4];//Integer.BYTES];
        inputStream.read(receive, 0, 4);//Integer.BYTES);
        
        msgLength = ByteBuffer.wrap(receive).getInt();
       // System.out.println(inputStream);
        //System.out.println("msglen"+msgLength);
        msgType = (byte)inputStream.read();
        //Type = MsgType.values()[inputStream.read()];
        //System.out.println("msgType"+inputStream.read());
        //System.out.println("msgType"+msgType);
		// Not Finished  tring to finish
		//System.out.print(Type.ordinal() + " ");
		
		switch(msgType) {
			case 4: case 6: {//have: case request:{// 
				payLoad = new PayLoad();
				byte[] indexArray = new byte[4];
				inputStream.read(indexArray, 0, 4);

				payLoad.pieceIndex = ByteBuffer.wrap(indexArray).getInt();
				//System.out.println("pieceIndex46 "+payLoad.pieceIndex);
			}
			break;
			case 5: {//bitfield: {//
				payLoad = new PayLoad();
				byte[] bitFieldArray = new byte[msgLength - 5];
				inputStream.read(bitFieldArray, 0, bitFieldArray.length);
				BitField bitField = new BitField(bitFieldArray);
				payLoad.bitField = bitField;
			}
			break;
			case 7: {//piece: {//
				payLoad = new PayLoad();
				byte[] indexArray = new byte[4];
				inputStream.read(indexArray, 0, 4);
				
				payLoad.pieceIndex = ByteBuffer.wrap(indexArray).getInt();
				//System.out.println("pieceIndex7 "+payLoad.pieceIndex);
				byte[] Content = new byte[msgLength - 4 - 5];
				while (inputStream.available() < Content.length) {
					Thread.sleep(10);
				}
				inputStream.read(Content, 0, Content.length);
				payLoad.content = Content;
			}
			break;
			default: {
				//for choke, unchoke, interested, not_interested message
				//skip checking the payload field
			}
			break;
		}
    }

}
