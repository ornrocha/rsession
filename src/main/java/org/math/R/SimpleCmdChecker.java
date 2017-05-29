package org.math.R;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import org.math.R.Logger.Level;

/**
 * @author ornrocha 
 * implements features of BalusC 
 */

public class SimpleCmdChecker implements Runnable{


	private ReusableInputStream stream;
	private String output=null;
	private Logger log;
	
	public SimpleCmdChecker(InputStream instream) throws IOException{
		this.stream=new ReusableInputStream(instream);
	}
	
	public SimpleCmdChecker(InputStream instream, Logger console) throws IOException{
		this.stream=new ReusableInputStream(instream);
		this.log = console;
	}
	
	@Override
	public void run() {
		if(stream!=null){
			
			BufferedReader inputFile = new BufferedReader(new InputStreamReader(stream));
			String currentline = null;
	        try {
				while((currentline = inputFile.readLine()) != null) {
					//System.out.println(currentline);
					if(currentline!=null && !currentline.isEmpty()){
						output=currentline;
						if(log!=null)
							log.println(currentline, Level.INFO);
						//System.out.println(currentline);
					}
				}
				
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public String getOutput(){
		return output;
	}
	
	
	
	private class ReusableInputStream extends InputStream{
	    /// 
		/// class From: http://stackoverflow.com/questions/13301076/how-to-clone-an-inputstream-in-java-in-minimal-time
		/// author: BalusC
	
		private InputStream input;
		private ByteArrayOutputStream output;
		private ByteBuffer buffer;

		public ReusableInputStream(InputStream input) throws IOException {
			this.input = input;
			this.output = new ByteArrayOutputStream(input.available()); // Note: it's resizable anyway.
		}

		@Override
		public int read() throws IOException {
			byte[] b = new byte[1];
			read(b, 0, 1);
			return b[0];
		}

		@Override
		public int read(byte[] bytes) throws IOException {
			return read(bytes, 0, bytes.length);
		}

		@Override
		public int read(byte[] bytes, int offset, int length) throws IOException {
			if (buffer == null) {
				int read = input.read(bytes, offset, length);

				if (read <= 0) {
					input.close();
					input = null;
					buffer = ByteBuffer.wrap(output.toByteArray());
					output = null;
					return -1;
				} else {
					output.write(bytes, offset, read);
					return read;
				}
			} else {
				int read = Math.min(length, buffer.remaining());

				if (read <= 0) {
					buffer.flip();
					return -1;
				} else {
					buffer.get(bytes, offset, read);
					return read;
				}
			}

		}

	}
}