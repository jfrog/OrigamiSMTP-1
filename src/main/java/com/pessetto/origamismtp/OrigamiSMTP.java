package com.pessetto.origamismtp;

import com.pessetto.origamismtp.filehandlers.EmailHandler;
import com.pessetto.origamismtp.filehandlers.inbox.Inbox;
import com.pessetto.origamismtp.status.StatusListener;
import com.pessetto.origamismtp.threads.ConnectionHandler;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/** The OrigamiSMTP main class
 * @author Travis Pessetto
 * @author pessetto.com
 */
public class OrigamiSMTP{

	private ServerSocket smtpSocket;
	private List<StatusListener> statusListeners;
	private int port;
	private String[] protocols;
	private boolean keepRunning = true;
	private EmailHandler eh;

	/** Creates an instance opened to the specified port
	 * @param port The port to open SMTP on
	 */
	public OrigamiSMTP(int port, String[] protocols)
	{
		this.port = port;
		this.protocols = protocols;
		statusListeners = new ArrayList<StatusListener>();
		this.eh = new EmailHandler();
	}


	/** Here for command line usage.
	 * Starts the server with TLSv1.2 protocol only by default.
	 * Passing 'default' as the second argument will enable:
	 * SSLv3, TLSv1, TLSv1.1, TLSv1.2
	 *
	 * @param args Program arguments
	 * @throws Exception Anything that could go wrong with the connection
	 */
	public static void main(String args[]) throws Exception
	{
		String[] enabledProtocols = {"TLSv1.2"};
		int bindPort = 2525;
		if ( args.length > 0 && !(args[0].equals("default")) ) {
			bindPort = checkCustomPort(args, bindPort);

			if ( Arrays.asList(args).contains("default") ) {
				enabledProtocols = null;
				System.out.println("Setting secured TLS socket enabled protocols: SSLv3, TLSv1, TLSv1.1, TLSv1.2");
			} else {
				enabledProtocols = tryToGetTLSProtocols(args);
				System.out.println("Setting TLS socket enabled protocols: " + Arrays.toString(enabledProtocols));
			}

		} else {
			System.out.println("Default to port 2525 and TLSv1.2 protocol");
		}

		OrigamiSMTP console = new OrigamiSMTP(bindPort, enabledProtocols);
		console.startSMTP();
	}


	private static int checkCustomPort(String[] args, int bindPort) {
		System.out.println("Setting port to " + args[0]);
		try {
			bindPort = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			e.printStackTrace();
			System.out.println("The first argument must be an integer.");
			System.exit(1);
		}
		return bindPort;
	}

	private static String[] tryToGetTLSProtocols(String[] potentialProtocols) {
		String[] filteredProtocols = Arrays.stream(potentialProtocols)
				.filter(s -> s.startsWith("TLS"))
				.toArray(String[]::new);
		if (filteredProtocols.length == 0) {
			System.out.println("TLS protocols only must be specified.");
			System.exit(1);
		}
		return filteredProtocols;
	}

	/**
	 * Adds a status listener
	 * @param sl A class that implements StatusListener
	 */
	public void addStatusListener(StatusListener sl)
	{
		statusListeners.add(sl);
	}

	/** Closes the SMTP connection
	 */
	public void closeSMTP()
	{
		try {
			smtpSocket.close();
			keepRunning = false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/** Starts the SMTP server
	 * @throws BindException Failure to bind to port
	 */
	public void startSMTP() throws BindException
	{
		try
		{
			ExecutorService threadPool = Executors.newWorkStealingPool();
			java.lang.System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
			java.lang.System.setProperty("sun.security.ssl.allowLegacyHelloMessages", "true");
			Socket ssls = null;
			System.out.println("Starting SMTP");
			InetSocketAddress bindAddress = new InetSocketAddress(port);
			smtpSocket = new ServerSocket();
			smtpSocket.setReuseAddress(true);
			smtpSocket.bind(bindAddress);
			System.out.println("Socket Opened");
			notifyStarted();
			while(!Thread.interrupted()  || !Thread.currentThread().isInterrupted() || keepRunning == true)
			{
				System.out.println("AWAIT CONNECTION");
				Socket connectionSocket = smtpSocket.accept();
				ConnectionHandler connectionHandler = new ConnectionHandler(connectionSocket, protocols, eh);
				threadPool.submit(connectionHandler);
				System.out.println("Connection sent to thread");
			}
			if(Thread.interrupted() || Thread.currentThread().isInterrupted())
			{
				notifyStopped();
				System.out.println("Quit due to interupted thread");
			}
		}
		catch(BindException ex)
		{
			notifyStopped();
			System.err.println("Could not bind to port");
			throw ex;
		}
		catch(Exception ex)
		{
			notifyStopped();
			System.err.println("Failed to open socket");
			ex.printStackTrace(System.err);
		}
	}

	/** Notifies the status listeners of SMTP start
	 */
	private void notifyStarted()
	{
		for(StatusListener listener : statusListeners)
		{
			listener.smtpStarted();
		}
	}

	/** Notifies the status listeners of SMTP stopped
	 */
	private void notifyStopped()
	{
		for(StatusListener listener : statusListeners)
		{
			listener.smtpStopped();
		}
	}


	public String getLatestMessage() {
		return eh.getInbox().getNewestMessage().getMessage();
	}

	public String getSubjectOfLastMessage() {
		return eh.getInbox().getNewestMessage().getSubject();
	}


}
