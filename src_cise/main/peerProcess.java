package main;

import file.*;

import java.io.*;
import java.util.*;

public class peerProcess {
	
	getData data;
	Peer info;        //information for local host
	BlockReaderWriter blockReaderWriter;
	TransmissionStatus Status;  //whether transmission is finished
	
	List<buildConnect> connectionList;
	neighborsUnchoke unchoked;
	Map<Integer, Integer> downloadMap;  //operation time mapping
	Map<Integer, Long> startTimeMap;
	OptimisticallyUnchokedNeighbor optimisticallyUnchokedNeighbor;
	PreferredNeighbors preferredNeighbors;
	
	Set<Integer> interested;
	Set<Integer> inFlight;
	Set<Integer> hasFileSet;
	
	
	public peerProcess(int peerIndex) {
		//set data using data from common.cfg
		data = new getData(peerIndex);
		info = data.thisPeer;
		InitBlocksAndStatus();

	}


	public void init() {
		GetListSet();

		for(Peer otherInfo : data.peerList) { //building multiple connections between hosts
			if (otherInfo.hasFile) {
				IfHaveFile(otherInfo);
			}
			if(!downloadMap.containsKey(otherInfo.peerID)) {
				PutDownLoadMap(otherInfo);
			}
			
			if(otherInfo != info) {
				buildConnect newConnection = new buildConnect(this, otherInfo);
				InitNewConnection(newConnection);
				connectionList.add(newConnection);

				new Thread(newConnection).start();
			}
		}
		
		//When all the connections are running, start checking preferred neighbors and optimistically unchoked neighbor
		NewNeighbours();

		NewThreads();
	}

	private void NewNeighbours() {
		optimisticallyUnchokedNeighbor = new OptimisticallyUnchokedNeighbor(this);
		preferredNeighbors = new PreferredNeighbors(this);
	}

	private void NewThreads() {
		new Thread(optimisticallyUnchokedNeighbor).start();
		new Thread(preferredNeighbors).start();
	}

	private void InitNewConnection(buildConnect newConnection) {
		try {
			newConnection.init();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void PutDownLoadMap(Peer otherInfo) {
		downloadMap.put(otherInfo.peerID, 0);
	}

	private void IfHaveFile(Peer otherInfo) {
		hasFileSet.add(otherInfo.peerID);
	}

	private void InitBlocksAndStatus() {
		blockReaderWriter = new BlockReaderWriter("clients/" + info.peerID + "/" + getData.fileName, info.hasFile);
		unchoked = new neighborsUnchoke();
		Status = new TransmissionStatus(info);
	}


	private void GetListSet() {
		connectionList = new ArrayList<>();
		downloadMap = new HashMap<>();
		startTimeMap = new HashMap<>();
		interested = new HashSet<>();
		inFlight = new HashSet<>();
		hasFileSet = new HashSet<>();
	}

	public static void main(String[] args) throws UnsupportedEncodingException {
		int peerID = Integer.parseInt(args[0]);
		peerProcess peer = new peerProcess(peerID);
		peer.init();
	}
	
}
