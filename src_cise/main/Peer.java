package main;
import java.util.*;

import Message.*;

public class Peer {
	public int peerID;
	public String hostName;
	public int PortNum;

	public BitField bitField;
	public int initSeqence;
	public boolean hasFile;
	
	public boolean initBefore(Peer otherSide) {
		return CompareSequence(otherSide); //there was no this
	}

	private boolean CompareSequence(Peer otherSide) {
		return initSeqence < otherSide.initSeqence;
	}

	public void updateBitField(int pieceIndex) {
		synchronized(bitField) {
			bitField.bitUpdate(pieceIndex);
		}
	}
	
	public boolean checkInterested(Peer otherSide) {
		boolean interested;
		interested = LoopCheck(bitField.checkInterested(otherSide.bitField));
		return interested;
	}

	private boolean LoopCheck(boolean b) {
		boolean interested;
		synchronized (bitField) {
			interested = b;
		}
		return interested;
	}

	public boolean checkInterested(Peer otherSide, Set<Integer> inFlightSet) {
		boolean interested;
		interested = LoopCheckinFlight(bitField.checkInterested(otherSide.bitField, inFlightSet));
		return interested;
	}

	private boolean LoopCheckinFlight(boolean b) {
		boolean interested;
		synchronized (bitField) {
			interested = b;
		}
		return interested;
	}


	public String PeerInfo() {
		return "peerID: " + peerID + "\n"
				+ "Host: " + hostName + "\n"
				+ "Port: " + PortNum + "\n";
	}
	
}
