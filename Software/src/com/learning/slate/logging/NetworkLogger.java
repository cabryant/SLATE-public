/**
 * 
 */
package com.learning.slate.logging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.learning.slate.Logger;
import com.learning.slate.SlateComponent;
import com.learning.slate.SlateComponent.TuioAction;
import com.learning.slate.data.DataManager;
import com.learning.slate.data.Event;
import com.learning.slate.data.SlateSettings.NetworkRole;
import com.learning.slate.data.ToolkitSettings;
import com.learning.slate.toolkits.base.BaseStateEnum;

/**
 * @author coram
 *
 */
public class NetworkLogger
{
	private static final String TOOLKIT_SETTINGS_REQUEST = "TOOLKIT_SETTINGS_REQUEST";
	private static final String CHALLENGE_REQUEST = "CHALLENGE_REQUEST";
	private static final String HELP_REQUEST = "HELP_REQUEST";
	private static final String SAVE_REQUEST = "SAVE_REQUEST";
	
	private SlateComponent slateComponent;
	private BaseStateEnum networkSubState;
	private NetworkRole networkRole;
	private boolean running = false;
	private PrintWriter out = null;
	private Map<String,Event> eventMap = null;
	private ToolkitSettings remoteToolkitSettings;
	private boolean sentToolkitSettings = false;
	private Thread thread;
	
	public boolean isRunning() { return running; }
	
	public NetworkLogger(NetworkRole networkRole, SlateComponent slateComponent)
	{
		this.networkRole = networkRole;
		this.networkSubState = BaseStateEnum.Network_Waiting;
		this.slateComponent = slateComponent;
	}
	
	public ToolkitSettings getRemoteToolkitSettings()
	{
		return remoteToolkitSettings;
	}
	
	public BaseStateEnum getNetworkSubState()
	{
		return networkSubState;
	}
	
	public void start()
	{
		running = true;
		eventMap = new HashMap<String,Event>();
		
		thread = new Thread(new Runnable()
		{
			public void run()
			{
				if (NetworkRole.Server.equals(networkRole))
				{
					startServer();
				}
				else
				{
					startClient();
				}
				
				Logger.info("Finished Network Sesssion");
				stop();
				slateComponent.reset();
			}
		});
		
		thread.start();
	}
	public void stop()
	{
		running = false;
		out = null;
		eventMap = null;
		thread = null;
		sentToolkitSettings = false;
		remoteToolkitSettings = null;
	}
	
	//private static final int SERVER_PORT = 4444;
	//private static final String SERVER_IP = "171.64.184.216";
	//String server_ip = "172.24.40.247";
	//String server_ip = "171.64.187.217";
	//int server_port = 4444;
	
	private void startServer() {
    	
		ServerSocket serverSocket = null;
		Socket clientSocket = null;
		BufferedReader in = null;
		
		try
		{
	        try {
	            serverSocket = new ServerSocket(slateComponent.getSlateSettings().getServerPort());
	            Logger.info("Listening on Port: " + slateComponent.getSlateSettings().getServerPort());
	        } catch (IOException e) {
	        	Logger.error("Could not listen on port: " + slateComponent.getSlateSettings().getServerPort());
	            throw e;
	        }
	
	        try {
	            clientSocket = serverSocket.accept();
	            Logger.info("Accept succeeded");
	        } catch (IOException e) {
	        	Logger.error("Accept failed.");
	            throw e;
	        }
	
	        out = new PrintWriter(clientSocket.getOutputStream(), true);
	        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	        
	        // SHARE LOCAL ToolkitSettings
	        sendToolkitSettings();
	        
	        String inputLine;
	        while (running && ((inputLine = in.readLine()) != null))
	        	processMessage(inputLine);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		finally
		{
			if (out != null) out.close();
	        try {
				if (in != null) in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        try {
				if (clientSocket != null) clientSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        try {
				if (serverSocket != null)
					serverSocket.close();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
	
	private void startClient()
	{
		Socket socket = null;
		BufferedReader in = null;
		
		try
		{
			try {
			    socket = new Socket(slateComponent.getSlateSettings().getServerIp(),
			    		            slateComponent.getSlateSettings().getServerPort());
			    out = new PrintWriter(socket.getOutputStream(), true);
			    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (UnknownHostException e) {
				Logger.error("Don't know about host: " + slateComponent.getSlateSettings().getServerIp());
			    throw e;
			    //System.exit(1);
			} catch (IOException e) {
				Logger.error(e.toString());
				e.printStackTrace();
			    Logger.error("Couldn't get I/O for the connection to: " + slateComponent.getSlateSettings().getServerIp());
			    throw e;
			    //System.exit(1);
			}
			
			Logger.info("Connection Success!");

			// Share local ToolkitSettings
			sendToolkitSettings();
			
			String inputLine;
	        while (running && ((inputLine = in.readLine()) != null))
	        	processMessage(inputLine);
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		finally
		{
			if (out != null) out.close();
			try {
				if (in != null) in.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				if (socket != null)
					socket.close();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
	
	// SHARING SETTINGS FROM OUR SIDE
	private void sendToolkitSettings()
	{
		if (out != null)
		{
			out.println(DataManager.toCompactXml(slateComponent.getToolkitSettings()));
			sentToolkitSettings = true;
			Logger.info("Sent ToolkitSettings");
		}
	}
	
	public void challenge()
	{
		if (BaseStateEnum.None.equals(networkSubState) ||
			BaseStateEnum.Network_Try.equals(networkSubState) ||
			BaseStateEnum.Network_Saved.equals(networkSubState))
		{
			sendCommand(CHALLENGE_REQUEST);
			networkSubState = BaseStateEnum.Network_Take_A_Challenge;
		}
	}
	
	public void help()
	{
		if (BaseStateEnum.Network_Solve.equals(networkSubState))
		{
			sendCommand(HELP_REQUEST);
			// It will be as if you just made the challenge
			networkSubState = BaseStateEnum.Network_Watch_2;
		}
	}
	
	public void save()
	{
		sendCommand(SAVE_REQUEST);
		if (BaseStateEnum.Network_Make_A_Challenge.equals(networkSubState))
		{
			// Saved a challenge, so watch the solution
			networkSubState = BaseStateEnum.Network_Watch;
		}
		else if (BaseStateEnum.Network_Solve.equals(networkSubState) ||
				 BaseStateEnum.Network_Help.equals(networkSubState))
		{
			// Saved a solution, so go back to the beginning
			networkSubState = BaseStateEnum.Network_Saved;
		}
	}
	
	private void sendCommand(String command)
	{
		if (out != null)
		{
			out.println(command);
			Logger.info(command + " command issued");
		}
	}
	
	// LOGGING EVENTS ON OUR SIDE
	public void logEvent(Event event) throws Exception
	{
		if (out != null)
		{
			if (!sentToolkitSettings)
			{
				Logger.info("have not yet sent ToolkitSettings, so didn't write: " + DataManager.toCompactXml(event));
			}
			else
			{
				out.println(DataManager.toCompactXml(event));
			}
		}
		else
		{
			Logger.info("out was null, so didn't write: " + DataManager.toCompactXml(event));
		}
	}
	
	private void processMessage(String message)
	{
		if (message.equals(TOOLKIT_SETTINGS_REQUEST))
		{
			sendToolkitSettings();
		}
		else if (message.startsWith("<toolkit"))
		{
			processToolkitSettings(message);
		}
		else if (remoteToolkitSettings == null)
		{
			sendCommand(TOOLKIT_SETTINGS_REQUEST);
		}
		else if (message.equals(CHALLENGE_REQUEST))
		{
			// partner is requesting a challenge
			networkSubState = BaseStateEnum.Network_Make_A_Challenge;
		}
		else if (message.equals(HELP_REQUEST))
		{
			// partner is asking for help - ACCEPT THE CHALLENGE
			networkSubState = BaseStateEnum.Network_Help;
		}
		else if (message.equals(SAVE_REQUEST))
		{
			if (BaseStateEnum.Network_Take_A_Challenge.equals(networkSubState))
			{
				// partner has made a challenge for you to solve
				networkSubState = BaseStateEnum.Network_Solve;
			}
			else
			{
				// partner is saving a solution
				networkSubState = BaseStateEnum.Network_Try;
			}
		}
		else if (message.startsWith("<event"))
		{
			processEvent(message);
		}
	}

	// FIXME TODO verify that toolkits are compatible?
	private void processToolkitSettings(String toolkitSettingsXml)
	{
		try
		{
			remoteToolkitSettings = (ToolkitSettings) DataManager.fromXml(toolkitSettingsXml);
			networkSubState = BaseStateEnum.None;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	// VIEWING EVENTS FROM THE OTHER SIDE
	private void processEvent(String xmlEvent)
	{
		try
		{
			Event event = (Event) DataManager.fromXml(xmlEvent);
			if (event == null) return;
			
			updateEventMap(event);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	synchronized private void updateEventMap(Event event)
	{
		String key = event.getKey();
		TuioAction tuioAction = event.getTuioAction();		
		
		if (TuioAction.Add.equals(tuioAction))
		{
			//System.out.println("add " + key);
			eventMap.put(key, event);
		}
		else if (TuioAction.Update.equals(tuioAction))
		{
			//System.out.println("update " + key);
			eventMap.put(key, event);
		}
		else if (TuioAction.Remove.equals(tuioAction))
		{
			//System.out.println("remove " + key);
			eventMap.remove(key);
		}
	}
	
	synchronized public List<Event> getCurrentEvents()
	{
		// Make a copy to avoid concurrent modification exception
		List<Event> events = new ArrayList<Event>();
		
		// Don't show for the following states:
		if (!BaseStateEnum.Network_Make_A_Challenge.equals(networkSubState) &&
			!BaseStateEnum.Network_Help.equals(networkSubState))
		{
			for (Event event : eventMap.values())
			{
				events.add(event);
			}
		}
		return events;
	}
}
