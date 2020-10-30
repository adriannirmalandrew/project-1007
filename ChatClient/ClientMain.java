import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;
import java.net.*;
import java.io.*;
import java.sql.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class ClientMain extends Application {
	//Session ID:
	private static String sessionId;
	//Username:
	private static String userName;
	//Accept inSock on 2021
	private static Socket inSock;
	private static BufferedReader inRead;
	
	//Message receiver thread:
	private static Thread msgRecv;
	
	@Override
	public void start(Stage stage) {
		//Set up window:
		Group group = new Group();
		Scene scene = new Scene(group, 600, 400);
		stage.setTitle("Digital Assignment 1 and 2");
		stage.setScene(scene);
		
		//Create login pane:
		BorderPane loginPane= new BorderPane();
		loginPane.setMinSize(600, 400);
		//Controls:
		GridPane dialog = new GridPane();
		dialog.setPadding(new Insets(100, 150, 150, 150));
		dialog.setHgap(5); dialog.setVgap(5);
		//IP address:
		dialog.add(new Label("IP Address:"), 0, 0);
		TextField serverIp = new TextField();
		dialog.add(serverIp, 1, 0);
		//Username:
		dialog.add(new Label("Username:"), 0, 1);
		TextField username = new TextField();
		dialog.add(username, 1, 1);
		//Password:
		dialog.add(new Label("Password:"), 0, 2);
		PasswordField password = new PasswordField();
		dialog.add(password, 1, 2);
		
		//Buttons:
		Button register = new Button("Register");
		dialog.add(register, 0, 3);
		Button login = new Button("Login");
		dialog.add(login, 1, 3);
		
		//Add pane to server picker:
		loginPane.setCenter(dialog);
		
		//Create chat pane:
		GridPane chatPane = new GridPane();
		chatPane.setMinSize(600, 400);
		TextArea msgArea = new TextArea();
		chatPane.add(msgArea, 0, 0);
		
		//Register button action:
		register.setOnAction(e -> {
			//Connect:
			Socket tempSock = null;
			try {
				tempSock = new Socket(serverIp.getText(), 2020);
				//Create and send JSON request:
				JSONObject registerObj = new JSONObject();
				registerObj.put("action", "register");
				registerObj.put("username", username.getText());
				registerObj.put("password", password.getText());
				String registerObjString = registerObj.toString();
				BufferedWriter tempOut = new BufferedWriter(new OutputStreamWriter(tempSock.getOutputStream()));
				tempOut.write(registerObjString + "\n", 0, registerObjString.length() + 1);
				tempOut.flush();
				//Receive response:
				BufferedReader tempIn = new BufferedReader(new InputStreamReader(tempSock.getInputStream()));
				String resp = tempIn.readLine();
				if(resp.equals("DONE")) {
					Alert doneAlert = new Alert(AlertType.INFORMATION, "Registration Successful");
					doneAlert.show();
				}
				else {
					Alert regAlert = new Alert(AlertType.ERROR, "Invalid Username or Password!");
					regAlert.show();
				}
				//Clear inputs:
				password.setText("");
				//Close connection:
				tempOut.close();
				tempIn.close();
				tempSock.close();
			}
			catch(Exception ie1) {
				Alert connFail = new Alert(AlertType.ERROR, "Registration error: " + ie1.getMessage());
				connFail.show();
			}
		});
		
		//Login button action:
		login.setOnAction(e -> {
			//Connect:
			Socket tempSock = null;
			try {
				tempSock = new Socket(serverIp.getText(), 2020);
				//Create and send JSON request:
				JSONObject loginObj = new JSONObject();
				loginObj.put("action", "login");
				loginObj.put("username", username.getText());
				loginObj.put("password", password.getText());
				String loginObjString = loginObj.toString();
				BufferedWriter tempOut = new BufferedWriter(new OutputStreamWriter(tempSock.getOutputStream()));
				tempOut.write(loginObjString + "\n", 0, loginObjString.length() + 1);
				tempOut.flush();
				//Open receiving connection:
				ClientMain.inSock = new ServerSocket(2021).accept();
				//Receive response:
				BufferedReader tempIn = new BufferedReader(new InputStreamReader(tempSock.getInputStream()));
				String resp = tempIn.readLine();
				if(resp.equals("DONE")) {
					//Get session ID:
					ClientMain.sessionId = tempIn.readLine();
					//Show welcome message:
					Alert doneAlert = new Alert(AlertType.INFORMATION, "Welcome, " + username.getText());
					doneAlert.show();
					//Change panes:
					stage.setScene(new Scene(chatPane, 600, 400));
					stage.show();
					//Start a message updater thread:
					ClientMain.msgRecv = new Thread(new Runnable() {
						public void run() {
							//Open reader for port 2021:
							try {
								ClientMain.inRead = new BufferedReader(new InputStreamReader(ClientMain.inSock.getInputStream()));
							}
							catch(IOException ie1) {
								//Display error message:
								Alert recvAlert = new Alert(AlertType.ERROR, "Could not receive messages! Please sign in again.");
								recvAlert.show();
							}
							//Read for messages and display them:
							try {
								while(true) {
									//Receive:
									String msgJson = ClientMain.inRead.readLine();
									//Parse:
									JSONObject msgObject = (JSONObject) new JSONParser().parse(msgJson);
									//Write to msgArea:
									String usernameStr = (String)msgObject.get("username");
									String messageStr = (String)msgObject.get("message");
									msgArea.setText(msgArea.getText() + usernameStr + ": " + messageStr + "\n");
								}
							}
							catch(IOException ir1) {
								Alert recvAlert = new Alert(AlertType.ERROR, "Could not receive messages! Please sign in again.");
								recvAlert.show();
							}
							catch(ParseException pr1) {}
						}
					});
					ClientMain.msgRecv.start();
				}
				else {
					Alert regAlert = new Alert(AlertType.ERROR, "Invalid Username or Password!");
					regAlert.show();
				}
				//Close connection:
				tempOut.close();
				tempIn.close();
				tempSock.close();
			}
			catch(IOException ie1) {
				Alert connFail = new Alert(AlertType.ERROR, "Login error: " + ie1.getMessage());
				connFail.show();
			}
		});
		
		//Start with server picker active:
		stage.setScene(new Scene(loginPane, 600, 400));
		stage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}