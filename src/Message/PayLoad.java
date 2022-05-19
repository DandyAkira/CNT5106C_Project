package Message;

import java.util.Arrays;

public class PayLoad {
    public byte[] content;
    public BitField bitField;
    public int pieceIndex;

    public PayLoad(){}

    public PayLoad(BitField bitField){
        this.bitField = bitField;
    }

    public PayLoad(int piece) {
		this.pieceIndex = piece;
	}
    public PayLoad(int piece, byte[] content){
        this.pieceIndex = piece;
        this.content = content;
    }

    public String PayLoadInfo(){
        return "file piece ID: " + pieceIndex + "\n"
                + "content: " + Arrays.toString(content) + "\n";
    }
}
