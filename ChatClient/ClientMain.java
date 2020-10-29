import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.geometry.*;
import java.net.*;
import java.io.*;
import java.sql.*;

public class ClientMain extends Application {
	@Override
	public void start(Stage stage) {
		//outSock connects on port 2020
		//inGetSock accepts inSock on 2021
		Socket outSock, inSock;
		ServerSocket inGetSock;
		
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
		TextField password = new TextField();
		dialog.add(password, 1, 2);
		
		//Buttons:
		Button register = new Button("Register");
		dialog.add(register, 0, 3);
		Button login = new Button("Login");
		dialog.add(login, 1, 3);
		
		//Add pane to server picker:
		loginPane.setCenter(dialog);
		
		//Register button action:
		register.setOnAction(e -> {
			//Connect:
			
		});
		
		//Login button action:
		login.setOnAction(e -> {
			//Connect:
			
		});
		
		//Create chat pane:
		GridPane chatPane = new GridPane();
		chatPane.setMinSize(600, 400);
		
		//Start with server picker active:
		stage.setScene(new Scene(loginPane, 600, 400));
		stage.show();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}