package mcast.ht.apps.filecopy;

import ibis.ipl.ReadMessage;
import ibis.ipl.WriteMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;

import mcast.ht.storage.ByteArrayStorage;
import mcast.ht.storage.Piece;
import mcast.ht.storage.Storage;

public class FileSetStorage implements Storage {

	private ByteArrayStorage delegate;
	
	public FileSetStorage(FileSet fileSet, int pieceSize) 
	throws IOException 
	{
		// serialize the fileset into a byte array
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);
		
		for (FileInfo fileInfo: fileSet) {
			dos.writeUTF(fileInfo.getPath());
			dos.writeLong(fileInfo.file.length());
		}
		
		byte[] data = bos.toByteArray();
		
		delegate = new ByteArrayStorage(data, 0, data.length, pieceSize);;
	}
	
	public FileSetStorage(int byteSize, int pieceSize) {
		byte[] data = new byte[byteSize];
		
		delegate = new ByteArrayStorage(data, 0, data.length, pieceSize);
	}
		
    public int getByteSize() {
        return delegate.getData().length;
    }
    
	public FileSet getFileSet(File target) 
	throws IOException
	{
		FileSet result = new FileSet();
		
		ByteArrayInputStream bis = new ByteArrayInputStream(delegate.getData());
		DataInputStream dis = new DataInputStream(bis);
		
		try {
			while (true) {
				File file = new File(target, dis.readUTF());
				long size = dis.readLong();
				
				FileInfo fileInfo = new FileInfo(file, "", size); 
				
				result.add(fileInfo);
			}
		} catch (EOFException ignored) {
			// ignore
		}
		
		return result;
	}
	
	public void close() throws IOException {
		delegate.close();
	}
	
	public void clear() throws IOException {
		delegate.clear();
	}

	public Piece createPiece(int index) {
		return delegate.createPiece(index);
	}

	public byte[] getDigest() throws IOException {
		return delegate.getDigest();
	}

	public int getPieceCount() {
		return delegate.getPieceCount();
	}

	public Piece readPiece(ReadMessage m) throws IOException {
		return delegate.readPiece(m);
	}

	public void writePiece(Piece piece, WriteMessage m) throws IOException {
		delegate.writePiece(piece, m);
	}
	
}
