package main;

import java.io.*;
import java.util.*;

public class OptimisticallyUnchokedNeighbor implements Runnable {

	private static int unchokedInterval;
	
	Peer thisPeer;
	
	volatile TransmissionStatus transmissionStatus;
	
	volatile List<buildConnect> connectionList;
	volatile Set<Integer> interestedSet;
	
	volatile neighborsUnchoke unchokedNeighbors;
	
	OptimisticallyUnchokedNeighbor(peerProcess peer){
		unchokedInterval = getData.optim * 1000;
		GetPeerSet(peer);
		GetListsSet(peer);
	}

	private void GetListsSet(peerProcess peer) {
		connectionList = peer.connectionList;
		interestedSet = peer.interested;
		unchokedNeighbors = peer.unchoked;
	}

	private void GetPeerSet(peerProcess peer) {
		thisPeer = peer.info;
		transmissionStatus = peer.Status;
	}

	@Override
	public void run() {
		try {
			//Wait for all the connection is set up
			Thread.sleep(unchokedInterval);
			TransLoop();
		}catch(InterruptedException | IOException e) {
			e.printStackTrace();
		}
		System.out.println("Neighbour Clear.");
	}

	private void TransLoop() throws IOException, InterruptedException {
		while(!transmissionStatus.checkCompleted()) {
			int peerID = getOptimisticallyUnchokedNeighbor();
			buildConnect connection = getConnection(peerID);
			//If all the interested peers have been chosen as preferred neighbors, skip this round
			//If the peerID got is wrong, skip this round
			if(connection == null) {
				Thread.sleep(unchokedInterval);
				continue;
			}
			ChokeOrUnChoke(peerID, connection);
		}
	}

	private int getOptimisticallyUnchokedNeighbor() throws IOException {
		int peerID = getRandomPeerID();
		while(checkUnhokedAlready(peerID) || (CheckPeerInBound(peerID, -1))) {
			if(!CheckUnchokeStatues() || CheckPeerInBound(interestedSet.size(), 0)) {
				return -1;
			}

			peerID = getRandomPeerID();
			if (CheckPeerInBound(peerID, -1))
				return -1;
		}
		unchokedNeighbors.optimalNeighbor = peerID;

		WriteUnChokeLog(peerID);

		return peerID;
	}

	private boolean CheckPeerInBound(int peerID, int i) {
		return peerID == i;
	}

	private void WriteUnChokeLog(int peerID) throws IOException {
		(new Log(thisPeer.peerID)).OptimisticallyUnchokedNeighborLog(peerID);
	}

	private boolean CheckUnchokeStatues() {
		return !unchokedNeighbors.preferredNeighborsSet.containsAll(interestedSet);
	}
	
	private boolean checkUnhokedAlready(int peerId) {
		return !unchokedNeighbors.preferredNeighborsSet.contains(peerId);
	}
	
	private int getRandomPeerID() {
		Integer[] peers =  CheckPeerInterested();
		if (peers == null) return -1;
		int index = (int)(Math.random()*(peers.length));
		return peers[index];
	}

	private Integer[] CheckPeerInterested() {
		Integer[] peers;
		synchronized(interestedSet) {
			peers = interestedSet.toArray(new Integer[0]);
			if (CheckPeerInBound(peers.length, 0))
				return null;
		}
		return peers;
	}

	private buildConnect getConnection(int peerID) {
		for(buildConnect connection : connectionList) {
			if(CheckPeerInBound(connection.distantPeer.peerID, peerID)) {
				return connection;
			}
		}
		return null;
	}

	private void ChokeOrUnChoke(int peerID, buildConnect connection) throws IOException, InterruptedException {
		connection.server.sendUnchokeMessage();
		Thread.sleep(unchokedInterval);
		if(interestedSet.contains(peerID) || CheckPeerInBound(unchokedNeighbors.optimalNeighbor, peerID)) {
			connection.server.sendChokeMessage();
		}
	}

}
