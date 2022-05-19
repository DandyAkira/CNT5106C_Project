package main;

public class TransmissionStatus {//check if transmission is done


	public boolean downloadCompleted;
	public boolean uploadCompleted;
	
	TransmissionStatus(Peer info){
		downloadCompleted = info.bitField.checkCompleted();
		uploadCompleted = false;
	}

	
	public void setDownloadCompleted() {
		synchronized(this){
			downloadCompleted = true;
		}
	}
	
	public void setUploadCompleted() {
		synchronized(this){
			uploadCompleted = true;
		}
	}
	
	public boolean checkCompleted() {
		synchronized(this){
			return downloadCompleted && uploadCompleted;
		}
	}

	public boolean IfDownloadComplete(){
		synchronized(this){
			return downloadCompleted;
		}
	}


	
}
