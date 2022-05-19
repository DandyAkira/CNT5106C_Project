package main;

import java.util.*;
import java.io.*;

public class Log {

	String LogName;
	
	public Log(int peerID) {
		this.LogName ="log_peer_"+ peerID + ".log";
	}
	
	public String Time() {//time getting function
		return new Date().toString();
	}
	public void Write(String s) throws IOException{//write
		FileWriter writer = new FileWriter(LogName, true);
		writer.write(s);
		writer.close();
	}
	public String BasicInfo(){
		return ("["+Time()+"] ");
	}

	public void LogConnection(int ServerID) throws IOException {
		String s = BasicInfo() +
				" Connects " + ServerID + ".\n";
		Write(s);
	}
	
	public void LogBegin(int ClientID) throws IOException {
		String s = BasicInfo() +
				" Connected	from " + ClientID + ".\n";
		
		Write(s);
	}
	
	public void OptimisticallyUnchokedNeighborLog(int distantID) throws IOException {
		String s = BasicInfo() +
				" Send UnChoke to " + distantID + ".\n";
		Write(s);
	}
	
	public void UnchokingLog(int distantID) throws IOException {
		String s = BasicInfo() +
				" Receive UnChoke from " + distantID + ".\n";
		
		Write(s);
	}
	public void PrefeerredNeighborsLog(int PeerID, List<Integer> PreferredList) throws IOException {
		StringBuilder s = new StringBuilder("[" + Time() + "] " + PeerID +
				" adds preferred neighbors:");
		for (Integer id : PreferredList)
			s.append(" ").append(id).append(", ");
		s = new StringBuilder(s.substring(0, s.length() - 1));
		s.append(".\n");

		Write(s.toString());
	}
	
	// Choking
	public void ChokingLog(int distantID) throws IOException {
		String s = BasicInfo() +
				" Receive Choke from " + distantID + ".\n";
		
		Write(s);
	}
	
	// Receiving have message
	public void ReceiveHaveMessageLog(int distantID, int PieceIndex) throws IOException {
		String s = BasicInfo() +
				" Receive Have piece from " + distantID + ".\n";
		
		Write(s);
	}
	
	// Receiving interested message
	public void ReceiveInterestedMessageLog(int distantID) throws IOException {
		String s = BasicInfo() +
				" Receive Interested from " + distantID + ".\n";
		
		Write(s);
	}
	
	// Receiving not interested message
	public void ReceiveNotInterestedMessageLog(int distantID) throws IOException {
		String s = BasicInfo() +
				" Received Not Interested from " + distantID + ".\n";
		
		Write(s);
	}
	
	// Downloading a piece
	public void DownloadPieceLog(int distantID, int PieceIndex, int PieceNum) throws IOException {
		String s = BasicInfo() +
				" Downloaded the piece " + PieceIndex + " from " + distantID + " Now it has " + PieceNum + " pieces .\n";
		
		Write(s);
	}
	
	// Completion of download
	public void CompletionLog() throws IOException {
		String s = BasicInfo() +  " File Downloading Complete.\n";
		Write(s);
	}
}
