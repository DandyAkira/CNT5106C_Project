package Message;

import java.util.ArrayList;
import java.util.Set;

import Message.BitField;

public class BitField {
//maybe bug here
    public final byte[] havePieces;
    public static int pieceNum;
    
    public BitField(byte[] filePieces){
        this.havePieces = filePieces;
    }
    public BitField(int pieceNum){
    	this(pieceNum, false);
    }
    public BitField(int pNum, boolean hasFile){
    	pieceNum = pNum;
		int offset = pieceNum % 8;
		offset = (offset == 0) ? 8 : offset;
		int size = pieceNum/8 + (offset==8 ? 0 : 1);
		havePieces = new byte[size];
		byte tag;
		if(!hasFile) {
			//Initialize the bit field array to 0
			tag=(byte)0;
		}else {
			//Initialize the bit field array to 1
			tag=(byte)-1;
		}
		for(int i = 0; i < size - 1; i++) {
			havePieces[i] = tag;
		}
		
		//Set the value of last byte
		setHavePiece(hasFile, offset, size);
    }
	
    
    public boolean checkInterested(BitField server) {
		
		String stringserver = server.toString();
		String stringclient = this.toString();
		for(int i=0; i<stringclient.length(); i++) {
			//check interested sets
			if( stringserver.charAt(i) == '1' )
				if(stringclient.charAt(i) == '0') {
					return true;
				}
				
		}
		return false;
	}
	
	public boolean checkInterested(BitField server, Set<Integer> inFlight) {
		
		String stringserver = server.toString();
		String stringclient = this.toString();
		
		for(int i=0; i<stringclient.length(); i++) {//add a check
			//check inflight sets
			if( stringserver.charAt(i) == '1'  )
				if(stringclient.charAt(i) == '0') {
					if(!inFlight.contains(i)) {
						return true;
					}
				}
				
		}
		return false;
	}
    
	public int askForRequest(BitField server) {
		String stringserver = server.toString();
		String stringclient = this.toString();
		
		ArrayList<Integer> bitArray = new ArrayList<>();
		
		for(int i = 0; i < stringclient.length(); i++) {
			if(stringclient.charAt(i) == '0' ) {
				if(stringserver.charAt(i) == '1') {
					bitArray.add(i);
				}
			}
				
		}
		if(bitArray.size() == 0) {
			return -1;
		}
		//else return
		return bitArray.get((int)(Math.random()*(bitArray.size())));
	}
	

    public boolean IfComplete(){
        int have = 0;
        for (byte havePiece : havePieces) {
            if (havePiece == 1) {
                have++;
            }
        }
        if(have == havePieces.length) {
        	return true;
        }
        else {
        	return false;
        }
        
    }

    public int[] Compare(byte[] receiveBitFiled, byte[] clientBitField){
        int[] compare = new int[receiveBitFiled.length];
        for(int i=0; i<receiveBitFiled.length; i++){
            if(receiveBitFiled[i] == 1 && clientBitField[i] == 0){
                compare[i] = 1;
            }
        }
        return compare;
    }

    public boolean IfInterested(int[] compare){
        for(int each : compare){
            if(each == 1){
                return true;
            }
        }
        return false;
    }
    private void setHavePiece(boolean hasFile, int offset, int size) {
		for(int i = 0; i < 8; i++) {
			if(hasFile) {
				havePieces[size-1] = (byte)((int)havePieces[size-1] * 2 + (i < offset ? 1 : 0));
			}else {
				havePieces[size-1] = 0;
			}
		}
	}
    public void bitUpdate(int update) {
		
		int yushu = update % 8;
		int i = update / 8;
		havePieces[i] += (int)Math.pow(2, (7-yushu));
	}
	
	public boolean bitCheck(int offset) {
		int index = offset / 8;
		int remain = offset % 8;
		String s = byteToString(havePieces[index]);
		return s.charAt(remain) == '1';
	}
	public boolean checkCompleted() {
		for(int i = 0; i < havePieces.length - 1; i++) {
			if(havePieces[i] != -1)
				return false;
		}
		if(havePieces.length > 0) {
			int offset = pieceNum % 8;
			offset = (offset == 0) ? 8 : offset;
			int checkSum = 0;
			for(int i = 0; i < 8; i++) {
				checkSum = (byte)(checkSum * 2 + (i < offset ? 1 : 0));
			}
			if(havePieces[havePieces.length - 1] != checkSum)
				return false;
		}
		return true;
	}
	
	public String ByteArray2String(byte[] bytes){
        StringBuilder result = new StringBuilder();
        for(byte each: bytes){
            result.append(each);
        }
        return  result.toString();
    }
	
	
	@Override
	public String toString() {
		String bitString = new String();
		for(byte b : havePieces)
			bitString += byteToString(b);
		return bitString;
	}
	
	private String byteToString(byte b) {
		if(b>=0) {
			char[] chars = Integer.toBinaryString(b ^ 0x00FF).toCharArray();
			handleChar(chars);
			return new String(chars);
		}else {
			return Integer.toBinaryString(b & 0x00FF);
		}
	}
	private void handleChar(char[] chars) {
		for(int i=0;i<chars.length;i++) {
			if(chars[i]=='0') {
				chars[i]='1';
			}
			else {
				chars[i]='0';
			}
		}
	}
}
