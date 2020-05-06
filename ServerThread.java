import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class ServerThread extends Thread {
	private ArrayList<Card> openPile;
	private Socket clientSocket;
	private Semaphore sem;
	
	ServerThread(String name, ArrayList<Card> openPile, Socket clientSocket, Semaphore sem) {
		super(name);
		this.openPile = openPile;
		this.clientSocket = clientSocket;
		this.sem = sem;
	}
	
	public void run() {
		try {		
			sem.acquire();
			
			openPile.addAll(receive());
			System.out.println("\n" + getName() + " dumps 5 cards in the pile...");
			printPile();
			
			sem.release();
			
			sleep(500);
			
			sem.acquire();
			
			send();
			openPile = receive();
			System.out.println("\n" + getName() + " replaces 2 cards from the pile...");
			printPile();
			
			sem.release();
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printPile() {
		System.out.println("Current cards in Open Pile: ");
		for (int i = 0; i < openPile.size(); ++i) {
			openPile.get(i).printCard();
		}
	}
	
	public ArrayList<Card> receive() throws IOException, ClassNotFoundException {
		InputStream is = clientSocket.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		SerializableList received = (SerializableList)ois.readObject();
		return received.getCardList();
	}
	
	public void send() throws IOException {
		OutputStream os = clientSocket.getOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(os);
		SerializableList toSend = new SerializableList(openPile);
		out.writeObject(toSend);
	}
}
