package file;

import java.util.*;

import main.getData;


import java.io.*;
import java.lang.System;
import java.util.stream.Stream;

public class BlockReaderWriter {

	public static String filePath;
	public List<BlockNode> blockList = new ArrayList<>();

	public BlockReaderWriter(String filepath, boolean hasFile) {
		filePath = filepath;
		if (!hasFile) {
			return;
		}
		ArrayList<Integer> Sizes = new ArrayList<>(4);
		Sizes.add(getData.fileSize); //fileSize
		Sizes.add(getData.pieceSize); //pieceSize
		Sizes.add(Sizes.get(0) % Sizes.get(1)); //tailSize
		Sizes.add(getBlockNum(Sizes.get(0), Sizes.get(1), Sizes.get(2))); //blockNum

		int fileSize = Sizes.get(0);
		int pieceSize = Sizes.get(1);
		int tailSize = Sizes.get(2);
		int blockNum = getBlockNum(fileSize, pieceSize, tailSize);

		tailSize = ModifyTail(pieceSize, tailSize);

		BlockListAdd(pieceSize, tailSize, blockNum);
	}

	private void BlockListAdd(int pieceSize, int tailSize, int blockNum) {
		for (int i = 0; i < blockNum - 1; ++i) {
			blockList.add(new BlockNode(i, pieceSize));
		}
		blockList.add(new BlockNode(blockNum - 1, tailSize));
	}

	private int ModifyTail(int pieceSize, int tailSize) {
		if(tailSize == 0){
			tailSize = pieceSize;
		}
		return tailSize;
	}

	private int getBlockNum(int fileSize, int pieceSize, int tailSize) {
		int blockNum;
		if(tailSize == 0){
			blockNum = fileSize / pieceSize;
		}
		else{
			blockNum= fileSize / pieceSize + 1;
		}
		return blockNum;
	}

	public int insertPiece(int pieceIndex, byte[] bytesInserted) {
		int insertOffset = getOffset(pieceIndex, bytesInserted);
		if(insertOffset == -1) return insertOffset;
		try {
			Integer x = GetRes(bytesInserted, insertOffset);
			if (x != null) return x;
		}catch (IOException e) {
			System.out.println("Unable to insert new text into the file");
			e.printStackTrace();
		}
		return pieceIndex;
	}

	private int getOffset(int pieceIndex, byte[] bytesInserted) {
		BlockNode newNode = new BlockNode(pieceIndex, bytesInserted.length);
		return findInsertOffset(newNode);
	}

	private Integer GetRes(byte[] bytesInserted, int insertOffset) throws IOException {
		File file = new File(filePath);
		if(file.exists()) {
			//Read the file into a byte array
			BufferedInputStream bufferedInputStream = getBufferedInputStream();
			byte[] byteStream = getBytes(file, bufferedInputStream);

			//Insert the byte array of new piece into the byte array
			byte[] concentratedArray = insertByteArray(byteStream, bytesInserted, insertOffset);
			if(concentratedArray == null) {
				return -1;
			}

			//Write the String back to the file
			WriteArray(concentratedArray);
		}
		else {
			FileNotFound(bytesInserted, file);
		}
		return null;
	}

	private byte[] getBytes(File file, BufferedInputStream bufferedInputStream) throws IOException {
		byte[] byteStream = new byte[(int) file.length()];
		ReadBytes(bufferedInputStream, byteStream);
		return byteStream;
	}

	private void FileNotFound(byte[] bytesInserted, File file) throws IOException {
		file.createNewFile();
		WriteArray(bytesInserted);
	}

	private void WriteArray(byte[] concentratedArray) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(filePath);
		BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
		bufferedOutputStream.write(concentratedArray);
		bufferedOutputStream.close();
	}

	private void ReadBytes(BufferedInputStream bufferedInputStream, byte[] byteStream) throws IOException {
		bufferedInputStream.read(byteStream);
		bufferedInputStream.close();
	}

	private int findInsertOffset(BlockNode newNode) {
		int insertOffset = 0;
		int blockIndex = 0;
		for(int i=0; i<blockList.size(); i++) {
			if(blockList.get(i).index < newNode.index) {
				insertOffset += blockList.get(i).length;
				blockIndex++;
			}else if(blockList.get(i).index == newNode.index) {
				//Duplicate piece received
				return -1;
			}else {
				break;
			}
		}
		synchronized(blockList) {
			blockList.add(blockIndex, newNode);
		}
		return insertOffset;
	}


	private void ArrayCopy(byte[] oldBytes, byte[] insertedBytes, int insertIndex, int lengthOld, int lengthInserted, byte[] concentratedBytes) {
		Copy(oldBytes, insertIndex, concentratedBytes, 0, 0);
		CopyInsertBytes(insertedBytes, insertIndex, lengthInserted, concentratedBytes);
		CopyAll(oldBytes, insertIndex, lengthOld, lengthInserted, concentratedBytes);
	}

	private void CopyAll(byte[] oldBytes, int insertIndex, int lengthOld, int lengthInserted, byte[] concentratedBytes) {
		System.arraycopy(oldBytes, insertIndex, concentratedBytes, insertIndex + lengthInserted, lengthOld - insertIndex);
	}

	private void CopyInsertBytes(byte[] insertedBytes, int insertIndex, int lengthInserted, byte[] concentratedBytes) {
		System.arraycopy(insertedBytes, 0, concentratedBytes, insertIndex, lengthInserted);
	}

	private void Copy(byte[] oldBytes, int insertIndex, byte[] concentratedBytes, int i, int i2) {
		System.arraycopy(oldBytes, i, concentratedBytes, i2, insertIndex);
	}

	public byte[] getPiece(int pieceIndex) {
		int offset = 0;
		for(int i = 0; i<blockList.size();i++) {
			if(blockList.get(i).index == pieceIndex) {
				return getPiece(offset, blockList.get(i).length);
			}
			offset += blockList.get(i).length;
		}
		return null;
	}

	private byte[] insertByteArray(byte[] oldBytes, byte[] insertedBytes, int insertIndex) {
		Integer lengthOld = getLengthOld(oldBytes, insertIndex);
		if (lengthOld == null) return null;
		int lengthInserted = insertedBytes.length;
		byte[] concentratedBytes = new byte[lengthOld + lengthInserted];
		ArrayCopy(oldBytes, insertedBytes, insertIndex, lengthOld, lengthInserted, concentratedBytes);
		return concentratedBytes;
	}

	private Integer getLengthOld(byte[] oldBytes, int insertIndex) {
		int lengthOld = oldBytes.length;
		if(insertIndex > lengthOld)
			return null;
		return lengthOld;
	}

	private byte[] getPiece(int offset, int length) {
		byte[] piece = null;
		try {
			File file = new File(filePath);

			BufferedInputStream bufferedInputStream = getBufferedInputStream();
			byte[] byteStream = getBytes(file, bufferedInputStream);
			piece = Arrays.copyOfRange(byteStream, offset, offset + length);
		}catch(IOException e) {
			e.printStackTrace();
		}
		return piece;
	}

	private BufferedInputStream getBufferedInputStream() throws FileNotFoundException {
		FileInputStream fileInputStream = new FileInputStream(filePath);
		return new BufferedInputStream(fileInputStream);
	}

}
