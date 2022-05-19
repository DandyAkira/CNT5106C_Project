package main;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.List;
import java.util.Set;

import Message.ActualMessage;
import file.BlockReaderWriter;

public class buildConnect implements Runnable{
	Socket socket;
	Client client;
	Server server;
	InputStream input;                   //where data from socket is put
	OutputStream output;         				

	Peer thisPeer;						//The peer information of this side
	Peer distantPeer;						//The peer information of the other side
	BlockReaderWriter readWrite;
	TransmissionStatus Status;
	Set<Integer> interested;			//Updated in server thread when receiving interest message
	Set<Integer> inFlight;
	List<buildConnect> connectionList;		//Save the info of other connection
	Map<Integer, Integer> downloadMap;		//Updated in client thread when receiving piece message
	Map<Integer, Long> startTimeMap;		//Set by each client thread
	neighborsUnchoke unchoked;

	
	public buildConnect(peerProcess local, Peer peer) {
		thisPeer =local.info;
		distantPeer =peer;
		GetPeerSet(local);
		GetListSet(local);
	}

	private void GetListSet(peerProcess local) {
		Status = local.Status;
		unchoked = local.unchoked;
		interested = local.interested;
		inFlight = local.inFlight;
		readWrite = local.blockReaderWriter;
	}

	private void GetPeerSet(peerProcess local) {
		downloadMap = local.downloadMap;
		startTimeMap = local.startTimeMap;
		connectionList = local.connectionList;
	}

	public void init() throws IOException {
		CheckIfInited();

		input = socket.getInputStream();
		output = socket.getOutputStream();
		output.flush();
		
	}

	private void CheckIfInited() throws IOException {
		if(!thisPeer.initBefore(distantPeer)) {

			socket = new Socket(distantPeer.hostName, distantPeer.PortNum);
		}else {
			StartListening();
		}
	}

	private void StartListening() throws IOException {
		ServerSocket listener = new ServerSocket(thisPeer.PortNum);
		socket = listener.accept();
		listener.close();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			System.out.println("Connection to peer " + distantPeer.peerID + " is running");

			client = new Client(this);
			server = new Server(this);

			if(thisPeer.initBefore(distantPeer)) {
				server.initialize();
			}else {
				client.initialize();
			}

			StartClientAndServerThread();
			//Dealwith actual message followed
			while(!Status.checkCompleted()) {
				ActualMessage actualMessage = new ActualMessage();

				if (isCompeleted(false)) {
					break;
				}
				//System.out.println("checkpoint1");	
				actualMessage.ReceiveMSG(input);
				String type = "";
				//System.out.println("checkpoint2");	
				//read and Dealwith the recieved message here
				switch(actualMessage.msgType) {
					case 0: {
						//Receiving choke message, send it to the client to Dealwith it
						ReceiveChoke(actualMessage);
						type = "Bit Field";
					}
					break;

					case 1: {
						//Receiving unchoke message, send it to the client to Dealwith it
						ReceiveUnChoke(actualMessage);
						type = "Unchoke";
					}
					break;

					case 2: {
						//Receiving interested message, send it to the server to Dealwith it
						ReceiveInterested();
						type = "Interested";
					}
					break;

					case 3: {
						//Receiving not interested message, send it to the server to Dealwith it
						ReceiveNotInterested();
						type = "not Interested";
					}
					break;

					case 4: {
						//Receiving have message, send it to the client to Dealwith it
						ReceiveHave(actualMessage);
						type = "Have";
					}
					break;

					case 5: {
						//Receiving bit field message, send it to the client to Dealwith it
						ReceiveBitField(actualMessage);
						type = "Bit Field";
					}
					break;

					case 6: {
						//Receiving request message, send it to the server to Dealwith it
						ReceiveRequest(actualMessage);
						type = "Request";
					}
					break;

					case 7: {
						//Receiving piece message, send it to the client to Dealwith it
						ReceivePiece(actualMessage);
						type = "Piece";
					}
					break;
			}
			System.out.println("Receive Msg from " + distantPeer.peerID +", Type: " + type);
			}

		}
		catch (ConnectException e) {
    		System.err.println("Server not Init!");
		} 
		catch (ClassNotFoundException e ) {
        	System.err.println("Class not found");
    	} 
		catch(UnknownHostException unknownHost) {
			System.err.println("Host Unknown!");
		}
		catch(IOException | InterruptedException ioException){
			ioException.printStackTrace();
		}
		System.out.println("Connection with " + distantPeer.peerID + " is over.");
	}

	private void StartClientAndServerThread() throws InterruptedException {
		new Thread(client).start();
		new Thread(server).start();
		Thread.sleep(3000);
	}

	private boolean isCompeleted(boolean completed) throws IOException, InterruptedException {
		while (input.available() == 0) {
			if (Status.checkCompleted()) {
				completed = true;
				Thread.sleep(10000);
				break;
			}
			Thread.sleep(100);
		}
		return completed;
	}

	private void ReceiveBitField(ActualMessage actualMessage) throws IOException, ClassNotFoundException {
		client.DealwithBitFieldMessage(actualMessage);
	}

	private void ReceiveChoke(ActualMessage actualMessage) throws IOException {
		client.DealwithChokeMessage(actualMessage);
	}

	private void ReceiveUnChoke(ActualMessage actualMessage) throws IOException, InterruptedException {
		client.DealwithUnchokeMessage(actualMessage);
	}

	private void ReceiveInterested() throws IOException {
		server.DealwithInterestedMessage();
	}

	private void ReceiveNotInterested() throws IOException {
		server.DealwithNotInterestedMessage();
	}

	private void ReceiveHave(ActualMessage actualMessage) throws IOException {
		client.DealwithHaveMessage(actualMessage);
	}

	private void ReceiveRequest(ActualMessage actualMessage) throws IOException {
		server.DealwithRequestMessage(actualMessage);
	}

	private void ReceivePiece(ActualMessage actualMessage) throws IOException, InterruptedException {
		//The client will set the bit field and update the file
		//All the servers in this peer will send a have message to the other side
		if(client.DealwithPieceMessage(actualMessage) != -1) {
			for(buildConnect connection : connectionList) {
				connection.server.DealwithPieceMessage(client.DealwithPieceMessage(actualMessage));
			}
		}
	}

}
