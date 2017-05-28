import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Receiver{
	DatagramSocket s;
	DatagramPacket firstPacket, dataPacket;
    byte[] buffer;

    FileOutputStream fileWriter;

    int receivedBytes, totalBytes, msgCount;
	    
    public Receiver(String name, int timeout) throws IOException{
		// Init
		s = new DatagramSocket(0);
		buffer = new byte[4096];
	
		System.out.println(s.getLocalPort());
	
		// Wait for sender to send firts packet
		// First packet should be the expected file size
		firstPacket = receivePacket();
	
		// Get expected file size from the packet
		String firstPacketContent = new String(firstPacket.getData(), 0, firstPacket.getLength());
		totalBytes = Integer.parseInt(firstPacketContent);;
	
		// Send reply to sender
		// "Already know the file size, you can start sending file data."
		sendReply(firstPacket.getAddress(), firstPacket.getPort(), (new String("START")).getBytes());
	
		// Creat an empty file with given file name
		fileWriter = new FileOutputStream(name);

		// Adjust buffer size (file size might be smaller than 4096 bytes)
		if (totalBytes < 4096) buffer = new byte[totalBytes];
	
		// Use timer is not accurate, might be a muti-threading issue
		// Timer timer = new Timer();
		// timer.schedule(new TimerTask() {
  		// 	@Override
  		// 	public void run() {
  		// 		System.out.println("Time out!!!");
    	// 		System.out.println("Bytes actually sent: " + receivedBytes);
		// 		System.out.println("Number of message: " + msgCount);
		// 		System.exit(1);
  		// 	}
		// }, timeout);

		final Runnable writeData = new Thread() {
			@Override 
  			public void run(){ 
  				// Count number of message received 
   				msgCount = 0;

   				// Keep track of the number of bytes received
   				// Compare it with file size
				while(receivedBytes < totalBytes) {
					try { 
						dataPacket = receivePacket();
						fileWriter.write(dataPacket.getData(), 0, dataPacket.getLength());
					}
					catch (IOException e) { 
  						System.exit(1);
					}
					
					receivedBytes = receivedBytes + dataPacket.getLength();
					msgCount++;
	    		}
  			}
		};

		// In order to calculate time accurately, use single thread
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		final Future future = executor.submit(writeData);
		executor.shutdown();

		try { 
  			future.get(timeout, TimeUnit.MILLISECONDS); 
		}
		catch (InterruptedException ie) { 
  			System.exit(1);
		}
		catch (ExecutionException ee) { 
  			System.exit(1);
		}
		catch (TimeoutException te) { 
  			// System.out.println("Timeout!");
  			System.out.println(msgCount + " " + receivedBytes);
			System.exit(1);
		}
		// Stop immediately.
		if (!executor.isTerminated()) executor.shutdownNow(); 

		// System.out.println("Complete");
		System.out.println(msgCount + " " + receivedBytes);
		System.exit(0);
    }
    
    
    public DatagramPacket receivePacket() throws IOException{
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
		s.receive(packet);
	
		return packet;
    }

    public void sendReply(InetAddress host, int port, byte[] message) throws IOException {
       	DatagramPacket packet = new DatagramPacket(message, message.length, host, port);
       	s.send(packet);
   	}	

   public static void main(String[] args) throws IOException {
		// Checking the arguments
		if (args.length != 2) {
			System.out.println("Error: Expect 2 arguments");
			System.exit(1);
		}

		// Initialize variables
    	String filename = args[0];
		int timeout = 0;

		// Check if timeout is an integer and greater than 0
		try {
			timeout = Integer.parseInt(args[1]);
		}
		catch(NumberFormatException e) { 
			System.out.println("Timeout must be and integer");
			System.exit(1);
		}
		if(timeout < 0) {
			System.out.println("Timeout must be greater than 0");
			System.exit(1);
		}

		// Start receiver
		Receiver receiver = new Receiver(filename, timeout);
    }
}