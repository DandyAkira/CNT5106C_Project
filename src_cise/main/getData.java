package main;

import java.io.*;
import java.util.*;

import Message.*;

public class getData {
	
	public static int neighbourNum;
	public static int unChokeTime;
	public static int optim;
	public static int fileSize;
	public static int pieceSize;
	public static String fileName;
	public List<Peer> peerList = new ArrayList<>();
	public Peer thisPeer;
	getData(int peerId) {
		try {
			File common = new File("Common.cfg"); //file must be at AbsolutePath
			if (common.isFile() && common.exists()) {
				InputStreamReader read = new InputStreamReader(new FileInputStream(common));
				BufferedReader bufferedReader = new BufferedReader(read);
				ReadCommon(bufferedReader);
				bufferedReader.close();
				read.close();
			}
			else {
				System.out.println("common.cfg not found!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		PeerInfo(peerId);
	}



	private void ReadCommon(BufferedReader bufferedReader) throws IOException {
		String lineTxt;
		int i = 0;
		while ((lineTxt = bufferedReader.readLine()) != null) {
			String[] val = lineTxt.split(" ");
			switch(i) {
				case 0:
					DoingChoke(val);
					break;
				case 1:
					DoingUnChoke(val);
					break;
				case 2:
					DoingInterested(val);
					break;
				case 3:
					DoingUninterested(val);
					break;
				case 4:
					DoingHave(val);
					break;
				case 5:
					DoingBitField(val);
					break;
				default:
					break;
			}
			i++;
		}
	}

	private void DoingBitField(String[] val) {
		pieceSize = Integer.parseInt(val[1]);
	}

	private void DoingHave(String[] val) {
		fileSize = Integer.parseInt(val[1]);
	}

	private void DoingUninterested(String[] val) {
		fileName = val[1];
	}

	private void DoingInterested(String[] val) {
		optim = Integer.parseInt(val[1]);
	}

	private void DoingUnChoke(String[] val) {
		unChokeTime = Integer.parseInt(val[1]);
	}

	private void DoingChoke(String[] val) {
		neighbourNum = Integer.parseInt(val[1]);
	}


	//Read Peer Info
	private void PeerInfo(int peerId) {
		try {
			//String encoding = "UTF-8";
            File file = new File("PeerInfo.cfg");//这里必须输入绝对路径
            if (file.isFile() && file.exists()) {
				ReadFile(peerId, file);
			}
            else {
                System.out.println("peerInfo.cfg not found.");
            }
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void ReadFile(int peerId, File file) throws IOException {
		InputStreamReader read = new InputStreamReader(new FileInputStream(file));
		ReadBuffer(peerId, read);
		read.close();
	}

	private void ReadBuffer(int peerId, InputStreamReader read) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(read);

		ReadPeerInfo(peerId, bufferedReader);
		bufferedReader.close();
	}

	private void ReadPeerInfo(int peerId, BufferedReader bufferedReader) throws IOException {
		String lineTxt;
		int initSequence = 0;
		while ((lineTxt = bufferedReader.readLine()) != null) {
			String[] val = lineTxt.split(" ");
			Peer peer = new Peer();
			GetPeerSet(val, peer);

			int pieceNum = fileSize/pieceSize;
			if(!(fileSize%pieceSize == 0)){
				pieceNum ++;
			}

			initSequence = getInitSequence(initSequence, val, peer, pieceNum);

			if (peer.peerID == peerId) {
				thisPeer = peer;
			}

			peerList.add(peer);
		}
	}

	private int getInitSequence(int initSequence, String[] val, Peer peer, int pieceNum) {
		peer.hasFile = (Integer.parseInt(val[3]) == 1);
		peer.bitField = new BitField(pieceNum, peer.hasFile);
		peer.initSeqence = initSequence++;
		return initSequence;
	}

	private void GetPeerSet(String[] val, Peer peer) {
		peer.peerID = Integer.parseInt(val[0]);
		peer.hostName = val[1];
		peer.PortNum = Integer.parseInt(val[2]);
	}
}
