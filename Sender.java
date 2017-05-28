import java.net.*;
import java.io.*;

public class Sender{
	DatagramSocket s;
	byte[] msg;

    File actualFile;
    FileInputStream actualFileContent;
    RandomAccessFile virtualFile;
    
    int totalBytes, sentBytes, receiverPort, msgCount;
    
    InetAddress receiverHost;

    public Sender(InetAddress host, int port, int payload) throws IOException {
    	receiverHost = host;
		receiverPort = port;
		msg = new byte[payload];

		s = new DatagramSocket();
        s.connect(receiverHost, receiverPort);
    }
    
    public void sendFile(File actualFile, int virtualfileSize) throws IOException{
		if (virtualfileSize != 0) {
			// System.out.println("Virtual File");

			// Create a random file with spicified size
			virtualFile = new RandomAccessFile("virtualFile", "rw");
			virtualFile.setLength(virtualfileSize);
			totalBytes = virtualfileSize;
		} else {
			// System.out.println("Actual File");

			// Get specified file and file size
			actualFileContent = new FileInputStream(actualFile);
			totalBytes = actualFileContent.available();
		}

		// Send first packet which contains file size
		sendPacket(Integer.toString(totalBytes).getBytes());

		// Should be enough to receive reply message from receiver
		// Didn't use msg here, because msg may not be big enough
		byte[] buffer = new byte[8]; 
		DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
		s.receive(reply);
	
		// If receiver received firts packet successfully, it should reply START
		String start = new String(reply.getData(), 0, reply.getLength());
		if (start.equals("START")){
			// Count number of msg sent
			msgCount = 0;

			int actualMsgSize = 0;
			while (sentBytes < totalBytes){
		    	if (virtualfileSize != 0) { // Read from virtual file
		    		// If last message is smaller than payload
		    		// Adjust msg size to send the last message
		    		if ((totalBytes - sentBytes) < msg.length) {
		    			msg = new byte[totalBytes - sentBytes];
		    			actualMsgSize = virtualFile.read(msg);
		    		} else {
		    			actualMsgSize = virtualFile.read(msg);
		    		}
		    	} else { // Read from actual file
		    		// If last message is smaller than payload
		    		// Adjust msg size to send the last message
		    		if ((totalBytes - sentBytes) < msg.length) {
		    			msg = new byte[totalBytes - sentBytes];
		    			actualMsgSize = actualFileContent.read(msg);
		    		} else {
		    			actualMsgSize = actualFileContent.read(msg);
		    		}
		    	}
		    	
		    	sendPacket(msg);

		    	sentBytes = sentBytes + actualMsgSize;
		    	msgCount++;
			}

			// System.out.println("Complete");
			System.out.println(msgCount + " " + sentBytes);
	    }
		else{
			System.out.println("Receiver didn't receieve the first packet successfully");
		}
	}

	private void sendPacket(byte[] message) throws IOException {
		DatagramPacket packet = new DatagramPacket(message, message.length);
		s.send(packet);
    }

    public static void main(String[] args) throws IOException{
		// Checking the arguments
		if (args.length != 4) {
			System.out.println("Error: Expect 4 arguments");
			System.exit(1);
		}

		// Initialize variables
		InetAddress theAddress = null;
		int thePort = 0;
		int payload = 0;
		int virtualfileSize = 0;
		File actualFile = new File(args[3]);

		// Check if host address is reachable
		try {
			theAddress = InetAddress.getByName(args[0]);
		}
		catch(UnknownHostException e) {
			System.out.println("Error: Host address is not reachable");
			System.exit(1);
		}

		// Chek if port number is an integer and greater than 0
		try {
			thePort = Integer.parseInt(args[1]);
		}
		catch(NumberFormatException e) { 
			System.out.println("Error: Port number must be an integer");
			System.exit(1);
		}
		if(thePort < 0) {
			System.out.println("Error: Port number must be greater than 0");
			System.exit(1);
		}

		// Chek if payload is an integer and greater than 1
		try {
			payload = Integer.parseInt(args[2]);
		}
		catch(NumberFormatException e) { 
			System.out.println("Error: Payload size must be an integer");
			System.exit(1);
		}
		if(payload < 1) {
			System.out.println("Error: Payload size must be greater than 1");
			System.exit(1);
		}

		// Check if user want to send a virtual file
		try {
			virtualfileSize = Integer.parseInt(args[3]);

			// Check if virtual file size is greater than 0
			if(virtualfileSize <= 0) {
				System.out.println("Error: Virtual file size must be greater than 0");
				System.exit(1);
			}
		}
		catch(NumberFormatException e) {
			// Check if file exists
			if(!actualFile.canRead()) {
				System.out.println("Error: Cannot open the file");
				System.exit(1);
			}
		}

		// Start sender
		Sender sender = new Sender(theAddress, thePort, payload);

		// Send the (virtual)file
		sender.sendFile(actualFile, virtualfileSize);
    }
}