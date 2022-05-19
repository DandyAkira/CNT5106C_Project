package main;

import Message.HandShake;
import Message.ActualMessage;
import Message.PayLoad;

import file.BlockReaderWriter;

import java.util.Map;
import java.util.Set;
import java.io.*;

public class Client implements Runnable {
	private int status;     //for local variable
	//choke,				0
	//unchoke,				1
	//interested,			2
	//not_interested,		3
	//have,					4
	//bitfield,				5
	//request,				6
	//piece					7
	//completed             8
	//not_initialized,      -1
	public static final int SLEEPTIME = 1500;
	
	private static int pieceGetNum = 0;
	
	OutputStream out;         //stream write to the socket
 	InputStream in;           //stream read from the socket
	
	Peer thisInfo;		//Info of this side
	Peer serverInfo;	//Info of the other side
	BlockReaderWriter clientReaderWriter;
	TransmissionStatus TStatus;
	
	Map<Integer, Integer> downloadMap;  //to record time to calculate download speed
	Map<Integer, Long> startTimeMap;
	Set<Integer> inFlightSet;
	
	
	
	public Client(buildConnect connect) {
		in = connect.input;
		out = connect.output;
		thisInfo=connect.thisPeer;
		serverInfo=connect.distantPeer;
		clientReaderWriter = connect.readWrite;
		startTimeMap = connect.startTimeMap;
		downloadMap = connect.downloadMap;
		inFlightSet = connect.inFlight;
		TStatus = connect.Status;
		status = -1;
	}
	
	@Override
	public void run() {

		System.out.println("Client: " + serverInfo.peerID + " on");

		GetStartMapSet();

		try {
			while(!TStatus.IfDownloadComplete()) {
				Thread.sleep(SLEEPTIME);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void GetStartMapSet() {

		long currentTime = System.currentTimeMillis();
		startTimeMap.put(serverInfo.peerID, currentTime);
	}

	public void initialize() throws IOException, ClassNotFoundException, InterruptedException {
		//handshake
		System.out.println("Connecting " + serverInfo.peerID);
		ShakeHands();

		SendBitfield();
		
		System.out.println("Connection initialized: " + thisInfo.peerID + " --- " + serverInfo.peerID);
		(new Log(thisInfo.peerID)).LogConnection(serverInfo.peerID);
		//put a log after every operation to record data flow
	}

	private void ShakeHands() throws IOException, InterruptedException {
		HandShake sendHandshake = new HandShake(thisInfo.peerID);
		HandShake receiveHandshake = new HandShake();

		sendHandshake.SendHandShake(out);
		receiveHandshake.ReceiveHandShake(in);

		System.out.println("shake hands with " + serverInfo.peerID);
	}

	private void SendBitfield() throws IOException, ClassNotFoundException {
		//sending bitfield
		PayLoad Payload = new PayLoad(thisInfo.bitField);
		ActualMessage message = new ActualMessage(4 + 1 +
				Payload.bitField.havePieces.length, (byte)5, Payload);//MsgType.bitfield is bitfield
		//System.out.println(message.Type());
		message.WriteMSG(out);
		System.out.println("Send message to: " + serverInfo.peerID +", Type: Bit Field");
	}
	
	public void DealwithBitFieldMessage(ActualMessage bitfieldMessage) throws ClassNotFoundException, IOException {
		
		synchronized(serverInfo.bitField) {
			serverInfo.bitField = bitfieldMessage.payLoad.bitField;
		}
		
		if(thisInfo.initBefore(serverInfo)) {
			SendBitfield();
		}
		boolean interested = thisInfo.checkInterested(serverInfo);
		status = interested ? 2 : 3;
		SendInterested(interested);
	}
	
	private void SendInterested(boolean interested) throws IOException {
		ActualMessage clientInterestedMessage = new ActualMessage(4 + 1,
				interested ? (byte)2 : (byte)3);//MsgType.interested : MsgType.not_interested);//(byte)2 : (byte)3 is Interested or not
		clientInterestedMessage.WriteMSG(out);
		if(interested){
			System.out.println("Send message to " + serverInfo.peerID + ", Type: Interested");
		}
		else{
			System.out.println("Send message to " + serverInfo.peerID + ", Type: Not Interested");
		}
		if(!interested) {
			status = 3;
			if(thisInfo.bitField.checkCompleted()) {
				status = 8 ;// as ClientStatus.completed;
				TStatus.setDownloadCompleted();
				(new Log(thisInfo.peerID)).CompletionLog();
			}
		}
	}
	
	
	public void DealwithHaveMessage(ActualMessage haveMessage) throws IOException {
		
//		System.out.println("Get a have message(" + haveMessage.payload.pieceIndex + ") from peer " + serverInfo.peerID);
		WriteReceiveHave(haveMessage);

		serverInfo.bitField.bitUpdate(haveMessage.payLoad.pieceIndex);
		synchronized(downloadMap) {
			//System.out.println("downloadMap"+downloadMap);
			downloadMap.replace(serverInfo.peerID, downloadMap.get(serverInfo.peerID), downloadMap.get(serverInfo.peerID) + 1);
			//System.out.println("downloadMap"+downloadMap);
		}
		if(status == 3) {//while not interested
			boolean interested = thisInfo.checkInterested(serverInfo);
			if(interested) {
				SendInterested(true);
			}
		}
	}

	private void WriteReceiveHave(ActualMessage haveMessage) throws IOException {
		(new Log(thisInfo.peerID)).ReceiveHaveMessageLog(serverInfo.peerID, haveMessage.payLoad.pieceIndex);
	}

	public void DealwithChokeMessage(ActualMessage chokeMessage) throws IOException {//change please
		if(!thisInfo.checkInterested(serverInfo)) {
			SendInterested(false);
			return;
		}
		status = 0;
		(new Log(thisInfo.peerID)).ChokingLog(serverInfo.peerID);
	}
	//if able, check and change here
	public void DealwithUnchokeMessage(ActualMessage chokeMessage) throws IOException, InterruptedException {
		status = 1;

		sendRequestMessage();
		WriteUnchokeLog();
	}

	private void WriteUnchokeLog() throws IOException {
		(new Log(thisInfo.peerID)).UnchokingLog(serverInfo.peerID);
	}

	private void sendRequestMessage() throws IOException {
		if(!thisInfo.checkInterested(serverInfo, inFlightSet)) {
			SendInterested(false);
			return;
		}
		int piece = WritePiece();
		PayLoad payload = new PayLoad(piece);
		ActualMessage requestMessage = new ActualMessage(2*4 + 1, (byte)6, payload);//MsgType.request
		requestMessage.WriteMSG(out);

		synchronized (inFlightSet) {
			inFlightSet.add(piece);
		}
		System.out.println("Send Msg to: " + serverInfo.peerID + " , Type: Request");
	}

	private int WritePiece() {
		int piece = thisInfo.bitField.askForRequest(serverInfo.bitField);
		while (inFlightSet.contains(piece)) {

			piece = thisInfo.bitField.askForRequest(serverInfo.bitField);
		}
		return piece;
	}

	public int DealwithPieceMessage(ActualMessage pieceMessage) throws IOException, InterruptedException {
		int piece;
		synchronized(clientReaderWriter) {
			piece = clientReaderWriter.insertPiece(pieceMessage.payLoad.pieceIndex, pieceMessage.payLoad.content);
		}
		IfNeedPiece(pieceMessage, piece);
		IfInterested();


		return piece;
	}

	private void WriteDownloadLog(ActualMessage pieceMessage) throws IOException {
		(new Log(thisInfo.peerID)).DownloadPieceLog(serverInfo.peerID,
				pieceMessage.payLoad.pieceIndex, pieceGetNum);
	}

	private void IfInterested() throws IOException, InterruptedException {
		boolean interested = thisInfo.checkInterested(serverInfo, inFlightSet);
		if(interested) {
			sendRequestMessage();
		}else {
			SendInterested(false);
		}
	}

	private void IfNeedPiece(ActualMessage pieceMessage, int piece) throws IOException {
		if(piece != -1) {
			synchronized (inFlightSet) {
				if(inFlightSet.contains(piece)){
					inFlightSet.remove(piece);
					pieceGetNum++;
					WriteDownloadLog(pieceMessage);
				}
			}
			UpdateAndCheck(pieceMessage);
		}
	}

	private void UpdateAndCheck(ActualMessage pieceMessage) {
		thisInfo.updateBitField(pieceMessage.payLoad.pieceIndex);
		if (thisInfo.bitField.checkCompleted()) {
			TStatus.setDownloadCompleted();
		}
	}

}