package main;

import java.net.*;
import java.util.*;
import java.io.*;

import Message.*;
import file.*;

public class Server implements Runnable {

	public static final int SLEEPTIME = 1000;

	Socket responseSocket;           //doesn't seem very usful,socket for server
	
	OutputStream out;         //stream write to the socket
 	InputStream in;           //stream read from the socket
	
	Peer thisPeer;		//Info of this side
	Peer clientPeer;	//Info of the other side
	BlockReaderWriter serverReadWrite;
	TransmissionStatus Status;
	
	List<buildConnect> connectionList;
	
	Set<Integer> interestedSet;  //set of peer that are interested to messages you have
	neighborsUnchoke unchoked;
	
	public Server(buildConnect connect) {
		in = connect.input;
		out = connect.output;
		thisPeer = connect.thisPeer;
		clientPeer = connect.distantPeer;
		Status = connect.Status;
		unchoked = connect.unchoked;
		serverReadWrite = connect.readWrite;
		connectionList = connect.connectionList;
		interestedSet = connect.interested;
		responseSocket = connect.socket;
	}
	
	@Override
	public void run() {
		try {
			System.out.println("Server: " + thisPeer.peerID + " on");
			//while(true) {//while(!checkUploadCompleted()) {
			while(!Status.checkCompleted()) {
				Thread.sleep(SLEEPTIME);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	

	public void initialize() throws ClassNotFoundException, IOException, InterruptedException {

		WriteBeginLog();
		ServerConnected();
	}

	private void ServerConnected() throws IOException, ClassNotFoundException, InterruptedException {
		System.out.println("Connected by: " + clientPeer.peerID);
		ShakeHands();
	}

	private void WriteBeginLog() throws IOException {
		(new Log(thisPeer.peerID)).LogBegin(clientPeer.peerID);
	}

	//check hand shake message from client
	private void ShakeHands() throws IOException, ClassNotFoundException, InterruptedException {
		HandShake ReceiveHandShake = new HandShake();
		HandShake SendHandShake = new HandShake(thisPeer.peerID);

		ReceiveHandShake.ReceiveHandShake(in);
		SendHandShake.SendHandShake(out);

		System.out.println("shake hands with: " + clientPeer.peerID);
		
	}
	//might be problem with hanshake
	public void DealwithInterestedMessage() throws IOException {
		CheckIfInterested();
		WriteInterestedLog();
		//put a log after every operation to record data flow
	}

	private void CheckIfInterested() {
		if(!interestedSet.contains(clientPeer.peerID)) {
			synchronized(interestedSet) {
				interestedSet.add(clientPeer.peerID);
			}
		}
	}

	private void WriteInterestedLog() throws IOException {
		(new Log(thisPeer.peerID)).ReceiveInterestedMessageLog(clientPeer.peerID);
	}

	public void DealwithNotInterestedMessage() throws IOException {
		RemoveFromInterested();
		if(unchoked.optimalNeighbor == clientPeer.peerID) {
			UnChokeMinus();
		}else if(unchoked.preferredNeighborsSet.contains(clientPeer.peerID)) {
			RemoveFromUnchoke();
		}
		WriteNotInterestedLog();
	}

	private void WriteNotInterestedLog() throws IOException {
		(new Log(thisPeer.peerID)).ReceiveNotInterestedMessageLog(clientPeer.peerID);
	}

	private void RemoveFromUnchoke() {
		synchronized(unchoked) {
			unchoked.preferredNeighborsSet.remove(clientPeer.peerID);
		}
	}

	private void UnChokeMinus() {
		synchronized(unchoked) {
			unchoked.optimalNeighbor = -1;
		}
	}

	private void RemoveFromInterested() {
		if(interestedSet.contains(clientPeer.peerID)) {
			synchronized(interestedSet) {
				interestedSet.remove(clientPeer.peerID);
			}
		}
	}

	public void DealwithPieceMessage(int pieceIndex) throws IOException {
		sendHaveMessage(pieceIndex);
	}
	
	private void sendHaveMessage(int pieceIndex) throws IOException {
		PayLoad have = new PayLoad(pieceIndex);
		ActualMessage haveMessage = new ActualMessage(4*2 + 1, (byte)4, have);
		haveMessage.WriteMSG(out);
	}
	
	public void DealwithRequestMessage(ActualMessage requestMessage) throws IOException {
		
		int pieceIndex = requestMessage.payLoad.pieceIndex;
		PrepareAndSendPiece(pieceIndex);
	}

	private void PrepareAndSendPiece(int pieceIndex) throws IOException {
		byte[] content;
		synchronized(serverReadWrite) {
			content = serverReadWrite.getPiece(pieceIndex);
		}
		if(content != null) {
			sendPieceMessage(pieceIndex, content);
		}
	}

	private void sendPieceMessage(int pieceIndex, byte[] content) throws IOException {
		PayLoad piecePayload = new PayLoad(pieceIndex, content);
		ActualMessage pieceMessage = new ActualMessage(4*2 + 1 + content.length, (byte)7, piecePayload); //msgtype piece
		//System.out.println("Send a piece message(" + pieceIndex + ") to peer " + clientInfo.peerID+content.length+" "+content );
		pieceMessage.WriteMSG(out);
	}
	
	public void sendChokeMessage() throws IOException {
		ActualMessage pieceMessage = new ActualMessage(4 + 1, (byte)0);
		pieceMessage.WriteMSG(out);
		System.out.println("Send Msg to " + clientPeer.peerID + ", Type: Choke");
	}
	
	public void sendUnchokeMessage() throws IOException {
		ActualMessage pieceMessage = new ActualMessage(4 + 1, (byte)1);
		pieceMessage.WriteMSG(out);
		System.out.println("Send Msg to " + clientPeer.peerID + ", Type: UnChoke");
	}
	
}