/*
- Server listens on port 2020
- Client opens port 2021 when connecting
- Server keeps socket streams open to connect to clients
*/

import java.net.*;
import java.io.*;
import java.util.*;
import java.sql.*;
import java.security.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class ServerMain {
	//Get base64-encoded hash of a string:
	private static String sha256base64(String str) throws Exception {
		MessageDigest md256 = MessageDigest.getInstance("SHA-256");
		byte[] hashed = md256.digest(str.getBytes("UTF-8"));
		return Base64.getEncoder().encodeToString(hashed);
	}
	
	//Register:
	private static boolean register(Connection conn, String username, String password) throws Exception {
		//Hash password:
		String passHash256 = sha256base64(password);
		//Add record:
		PreparedStatement registerStmt = conn.prepareStatement("insert into users(username, password) values(?, ?)");
		registerStmt.setString(1, username);
		registerStmt.setString(2, passHash256);
		return (registerStmt.executeUpdate() == 1);
	}
	
	//Login, returns the session ID:
	private static String login(Connection conn, String username, String password) throws Exception {
		//Hash password:
		String passHash256 = sha256base64(password);
		//Check if user exists:
		PreparedStatement userExist = conn.prepareStatement("select username from users where username=? and password=?");
		userExist.setString(1, username);
		userExist.setString(2, passHash256);
		ResultSet userRs = userExist.executeQuery();
		//If user doesn't exist, return null:
		if(!userRs.next()) return null;
		//Else, generate sessionId:
		String sessionId = String.valueOf(System.currentTimeMillis()) + username;
		//Set and return sessionId:
		PreparedStatement sessIdStmt = conn.prepareStatement("update users set sessionId=? where username=?");
		sessIdStmt.setString(1, sessionId);
		sessIdStmt.setString(2, username);
		sessIdStmt.executeUpdate();
		return sessionId;
	}
	
	//Verify user's identity and send a message as a JSON string:
	private static String message(Connection conn, String sessionId, String message) throws Exception {
		//Check if user exists:
		PreparedStatement userCheck = conn.prepareStatement("select username from users where sessionId=?");
		userCheck.setString(1, sessionId);
		ResultSet checkRs = userCheck.executeQuery();
		if(!checkRs.next()) return null;
		//Prepare JSON object if user exists:
		String username = checkRs.getString("username");
		JSONObject msgJson = new JSONObject();
		msgJson.put("username", username);
		msgJson.put("message", message);
		return msgJson.toString();
	}
	
	//Logout:
	private static boolean logout(Connection conn, String sessionId) throws Exception {
		//Set sessionId to none:
		PreparedStatement logoutStmt = conn.prepareStatement("update users set sessionId=\'none\' where sessionId=?");
		logoutStmt.setString(1, sessionId);
		return (logoutStmt.executeUpdate() == 1);
	}
	
	public static void main(String[] args) throws Exception {
		//Socket:
		ServerSocket mainSock = new ServerSocket(2020);
		//DB connection:
		Connection conn = null;
		try {
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/CSE1007", "project", "project");
		}
		catch(SQLException s1) {
			System.out.println("Failed to connect: " + s1.getMessage());
			System.exit(1);
		}
		//List of sessionIds and output streams for their sockets:
		HashMap<String, BufferedWriter> userSockets = new HashMap<>();
		
		//Main loop:
		while(true) {
			//Get connection from client:
			Socket tempSock = mainSock.accept();
			BufferedReader tempIn = new BufferedReader(new InputStreamReader(tempSock.getInputStream()));
			//Get JSON:
			String reqString = tempIn.readLine();
			JSONObject reqJson = null;
			try {
				//Get request:
				reqJson = (JSONObject) (new JSONParser().parse(reqString));
			}
			catch(ParseException pe1) {
				System.out.println("Error handling request: " + pe1.getMessage());
				tempIn.close();
				tempSock.close();
				continue;
			}
			catch(NullPointerException ne1) {
				System.out.println("Error: Client disconnected");
				tempIn.close();
				tempSock.close();
				continue;
			}
			//Open temporary connection to client:
			BufferedWriter tempOut = new BufferedWriter(new OutputStreamWriter(tempSock.getOutputStream()));
			//Execute request:
			try {
				switch((String)reqJson.get("action")) {
					case "register": {
						//Call register function:
						if(register(conn, (String)reqJson.get("username"), (String)reqJson.get("password")))
							tempOut.write("DONE\n", 0, 5);
						else
							tempOut.write("ERROR\n", 0, 6);
						break;
					}
					case "login": {
						//Call login function:
						String sessionId = login(conn, (String)reqJson.get("username"), (String)reqJson.get("password"));
						if(sessionId == null)
							tempOut.write("ERROR\n", 0, 6);
						else {
							//Add socket output to hashmap:
							try {
								String clientIp = tempSock.getInetAddress().getHostName();
								Socket tempClientSock = new Socket(clientIp, 2021);
								BufferedWriter tempClientOut = new BufferedWriter(new OutputStreamWriter(tempClientSock.getOutputStream()));
								userSockets.put(sessionId, tempClientOut);
							}
							catch(Exception e) {
								tempOut.write("ERROR\n", 0, 6);
								break;
							}
							tempOut.write("DONE\n", 0, 5);
							tempOut.write(sessionId + "\n", 0, sessionId.length() + 1);
						}
						break;
					}
					case "message": {
						//Verify user and create message JSON:
						String msgJson = message(conn, (String)reqJson.get("sessionid"), (String)reqJson.get("message"));
						if(msgJson == null) {
							tempOut.write("ERROR\n", 0, 6);
						}
						else {
							//Write message JSON to every connected user:
							for(Map.Entry bEntry: userSockets.entrySet()) {
								BufferedWriter bOut = (BufferedWriter) bEntry.getValue();
								bOut.write(msgJson + "\n", 0, msgJson.length() + 1);
								bOut.flush();
							}
							tempOut.write("DONE\n", 0, 5);
						}
						break;
					}
					case "logout": {
						//Logout:
						String sessionId = (String)reqJson.get("sessionid");
						if(logout(conn, sessionId)) {
							//Close and remove socket connection:
							userSockets.get(sessionId).close();
							userSockets.remove(sessionId);
							tempOut.write("DONE\n", 0, 5);
						}
						else {
							tempOut.write("ERROR\n", 0, 6);
						}
						break;
					}
				}
			}
			catch(SQLException s2) {
				System.out.println("Database Error: " + s2.getMessage());
			}
			finally {
				tempOut.flush();
				tempOut.close();
				tempIn.close();
				tempSock.close();
			}
		}
	}
}