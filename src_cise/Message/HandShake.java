package Message;

import java.io.*;
import java.util.Arrays;

public class HandShake {
    public String handshakeHeader;
    public byte[] zero_bits;
    public int peer_ID;

    public HandShake(){
        zero_bits = new byte[10];
    }

    public HandShake(int ID){
        handshakeHeader = "P2PFILESHARINGPROJ";
        zero_bits = new byte[10];
        peer_ID = ID;
    }

    public int ByteArray2Int(byte[] bytes){
        int result = 0;
        for(int i = 0; i < 4; i++){
            result += Math.pow(10, 4-i-1)*(bytes[i]);
        }
        return result;
    }

    public String ByteArray2String(byte[] bytes){
        StringBuilder result = new StringBuilder();
        for(byte each: bytes){
            result.append(each);
        }
        return  result.toString();
    }

    public String HandShakeInfo(){
        return(handshakeHeader + " " + ByteArray2String(zero_bits) + " " + peer_ID);
    }
    public void SendHandShake(OutputStream outputStream) throws IOException {
        ByteArrayOutputStream bytes_out = new ByteArrayOutputStream();
        DataOutputStream data_out = new DataOutputStream(bytes_out);

        data_out.writeBytes(handshakeHeader);
        data_out.write(zero_bits);
        data_out.writeInt(peer_ID);

        byte[] bytes_sent = bytes_out.toByteArray();
        synchronized (outputStream){
            outputStream.write(bytes_sent);
        }
        bytes_out.close();
        data_out.close();
    }
    
    public void ReceiveHandShake(InputStream inputStream) throws IOException, InterruptedException {
        byte[] receive = new byte[32];
        while(inputStream.available() != 32){
            Thread.sleep(50);
        }
        synchronized (inputStream){
            int readMsg = inputStream.read(receive, 0, 32);
        }
        InfoFromReceive(receive);
    }
    
    public void InfoFromReceive(byte[] receive){
        byte[] header = Arrays.copyOfRange(receive, 0, 18);
        zero_bits = Arrays.copyOfRange(receive, 18, 28);
        byte[] ID = Arrays.copyOfRange(receive, 28, 32);
        StringBuilder stringBuilder = new StringBuilder();
        for(byte each:header){
            stringBuilder.append(each);
        }
        handshakeHeader = stringBuilder.toString();
        peer_ID = ByteArray2Int(ID);
    }

}
