/*
 * Implementation of a two way messaging server in Java
 * By Srihari Nelakuditi for CSCE 416
 */

// I/O related package
import java.io.*;

// Socket related package
import java.net.*;

import java.util.*;
/*
 * This class does all of two way messaging server's job
 * It simultaneously watches both keyboard and socket for input
 *
 * It consists of 2 threads: parent thread (code inside main method)
 * and child thread (code inside run method)
 *
 * Parent thread spawns a child thread and then
 * reads from the socket and writes to the screen
 *
 * Child thread reads from the keyboard and writes to socket
 *
 * Since a thread is being created with this class object,
 * this class declaration includes "implements Runnable"
 */
public class GroupChatServer
{

	// For storing threads
	static Vector<ClientHandler> cliThreads = new Vector<>();
	/*
	 * The messaging server program starts from here.
	 * It sets up streams for reading & writing from keyboard and socket
	 * Spawns a thread which does the stuff under the run() method
	 * Then, it continues to read from socket and write to display
	 */
	public static void main(String args[]) throws IOException
	{
		
		System.out.println("Waiting for users...");
		// Server needs a port to listen on
		if (args.length != 1) {
			System.out.println("Usage: java GroupChatServer <port>");
			System.exit(1);
		}
		ServerSocket ss = new ServerSocket(Integer.parseInt(args[0]));
		Socket cs;

		while (true) {
			cs = ss.accept();
			BufferedReader fromSockReader = new BufferedReader(
					new InputStreamReader(cs.getInputStream()));
			PrintWriter toSockWriter =
					new PrintWriter(cs.getOutputStream(), true);
			String clientName = fromSockReader.readLine();
			
			ClientHandler groupMembers = new ClientHandler(cs, clientName, fromSockReader, toSockWriter);
			Thread accepting = new Thread(groupMembers);

			cliThreads.add(groupMembers);
			for (int i = 0; i < cliThreads.size(); i++) {
				if (!clientName.equals(cliThreads.get(i).clientName)) {
					cliThreads.get(i).toSockWriter.println("~"+clientName+"~ has joined the chat");
				}
			}

			accepting.start();
		}

	}
}

class ClientHandler implements Runnable {
	final BufferedReader fromSockReader;
	final PrintWriter toSockWriter;
	final String clientName;
	Socket cs;

	public ClientHandler(Socket cs, String clientName, BufferedReader fromSockReader, PrintWriter toSockWriter) {
		this.clientName = clientName;
		this.cs = cs;
		this.fromSockReader = fromSockReader;
		this.toSockWriter = toSockWriter;
	}

	@Override
	public void run()
	{
		while (true) {
			try {
				// Read a line from the user
				String line = fromSockReader.readLine();

				// If we get null, it means EOF, so quit
				if (line == null) {
					cleanup(this.cs, this.fromSockReader, this.toSockWriter);
					break;
				}
				for (int i = 0; i < GroupChatServer.cliThreads.size(); i++) {
						if (!GroupChatServer.cliThreads.get(i).clientName.equals(this.clientName)) {
							// Write the line to the socket
							GroupChatServer.cliThreads.get(i).toSockWriter.println(line);
						}

				}
			} catch (IOException e) {
				for (int i = 0; i < GroupChatServer.cliThreads.size(); i++) {
					GroupChatServer.cliThreads.get(i).toSockWriter.println("SERVER: "+clientName+" has left the chat");
				}
				break;
			}
		}
	}

	public void cleanup(Socket cs, BufferedReader fromSockReader, PrintWriter toSockWriter) {
		GroupChatServer.cliThreads.remove(this);
		try {
			if (fromSockReader != null) {
				fromSockReader.close();
			}
			if (toSockWriter != null) {
				toSockWriter.close();
			}
			if (cs != null) {
				cs.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}