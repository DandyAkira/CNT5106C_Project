package main;

import java.util.*;
import java.io.IOException;

public class PreferredNeighbors implements Runnable {

	
	private int unchokedInterval;
	private int pieceNumber;
	
	Peer thisPeer;
	
	List<buildConnect> connectionList;
	Map<Integer, Integer> downloadMap;
	Map<Integer, Long> startTimeMap;
	Set<Integer> interestedSet;
	Set<Integer> hasFileSet;
	
	TransmissionStatus peerStatus;
	
	neighborsUnchoke unchokedNeighbors;
	
	PreferredNeighbors(peerProcess peer) {
		unchokedInterval = getData.unChokeTime * 1000;
		pieceNumber = (int)Math.ceil(((double)getData.fileSize / (double)getData.pieceSize));
		thisPeer = peer.info;
		connectionList = peer.connectionList;
		downloadMap = peer.downloadMap;
		startTimeMap = peer.startTimeMap;
		interestedSet = peer.interested;
		hasFileSet = peer.hasFileSet;
		peerStatus = peer.Status;
		unchokedNeighbors = peer.unchoked;
	}

	
	private void DealwithPreferNeighbour() throws IOException {
		System.out.println("Selecting neighbour");
		long current = System.currentTimeMillis();
		TreeMap<Double, Integer> speedMap = new TreeMap<>(Collections.reverseOrder());
		for (Integer peerId : interestedSet) {
			if(downloadMap.containsKey(peerId) && startTimeMap.containsKey(peerId)){
				System.out.println("from " + peerId +": " + (downloadMap.get(peerId) * 1000) * 1.0 / (current - startTimeMap.get(peerId)) );
				speedMap.put((downloadMap.get(peerId) * 1000) * 1.0 / (current - startTimeMap.get(peerId)), peerId);

			}
			else{
				speedMap.put(0.0, peerId);
				System.out.println("from " + peerId +": " +  0.0 );
			}
		}
		SendingUnchoke(GetHighRateList(speedMap));

	}

	private ArrayList<Integer> GetHighRateList(TreeMap<Double, Integer> speedMap){

		ArrayList<Integer> highRateList = new ArrayList<>();
		int index = 1;
		for (Integer id : speedMap.values()) {
			if (index != getData.neighbourNum) {
				highRateList.add(id);
				index++;
			}
			else{
				break;
			}
		}

		if (highRateList.isEmpty())
			return highRateList;

		Integer key = unchokedNeighbors.optimalNeighbor;
		highRateList.remove(key);
		if(highRateList.size() == getData.neighbourNum + 1){
			highRateList.remove(highRateList.size() - 1);
		}

		return highRateList;
	}


	private void SendingUnchoke(ArrayList<Integer> highRateList) throws IOException {
		synchronized (unchokedNeighbors.preferredNeighborsSet) {
			for (Integer key : highRateList) {
				unchokedNeighbors.preferredNeighborsSet.add(key);
				System.out.println("Add " + key + " into neighbour");
			}
			WritePreferredNeighbourLog(highRateList);

			for (buildConnect connection : connectionList) {
				if (unchokedNeighbors.preferredNeighborsSet.contains(connection.distantPeer.peerID)) {
					connection.server.sendUnchokeMessage();
				}
			}
		}
	}

	private void WritePreferredNeighbourLog(ArrayList<Integer> highRateList) throws IOException {
		(new Log(thisPeer.peerID)).PrefeerredNeighborsLog(thisPeer.peerID, highRateList);
	}


	private boolean checkAllPeersDownload() {
		for(Integer peerID : downloadMap.keySet()) {
			int peerPieceNum = downloadMap.get(peerID);
			if(peerPieceNum != pieceNumber) {
				if(hasFileSet.contains(peerID)) {
					continue;
				}
				if (peerID == thisPeer.peerID) {
					continue;
				}

				return false;
			}
		}
		return peerStatus.IfDownloadComplete();
	}
	
	@Override
	public void run() {
		try {
			while (!peerStatus.checkCompleted()) {
				DealwithPreferNeighbour();
				Thread.sleep(unchokedInterval);

				synchronized (unchokedNeighbors.preferredNeighborsSet) {
					for(int i=0; i<connectionList.size(); i++){
						if(unchokedNeighbors.preferredNeighborsSet.contains(connectionList.get(i).distantPeer.peerID)){
							connectionList.get(i).server.sendChokeMessage();
						}
					}
					unchokedNeighbors.preferredNeighborsSet.clear();
				}

				if(checkAllPeersDownload()) {
					synchronized(peerStatus) {
						peerStatus.setUploadCompleted();
					}
				}
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(thisPeer.peerID + " has no neighbour anymore");
	}

}