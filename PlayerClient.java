import java.io.*;
import java.net.*;
import java.util.*;

public class PlayerClient {
	private Socket clientSocket;
	private String playerName;
	private ArrayList<Card> cards;
	
	PlayerClient(String playerName, String hostname, int port) throws IOException {
		this.playerName = playerName;
		clientSocket = new Socket(hostname, port);
		cards = new ArrayList<Card>();
	}
	
	public void send(String message) throws IOException {
	    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
	    out.println(message);
	}
	
	public void receiveScore() throws IOException {
		InputStream sin = clientSocket.getInputStream();
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(sin));
		System.out.println();
		String buffer = inFromClient.readLine();
		while (buffer != null) {
			System.out.println(buffer);
			buffer = inFromClient.readLine();
		}
	}
	
	public void send(ArrayList<Card> list) throws IOException {
		OutputStream os = clientSocket.getOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(os);
		SerializableList toSend = new SerializableList(list);
		out.writeObject(toSend);
	}
	
	public ArrayList<Card> receive() throws IOException, ClassNotFoundException {
		InputStream is = clientSocket.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		SerializableList received = (SerializableList)ois.readObject();
		return received.getCardList();
	}
	
	public void appendCards() throws IOException, ClassNotFoundException {
		cards.addAll(receive());
	}
	
	public void printCards() {
		for (int i = 0; i < cards.size(); ++i) {
			cards.get(i).printCard();
		}
	}
	
	public void close() throws IOException {
		clientSocket.close();
	}
	
	public String getPlayerName() {
		return playerName;
	}
	
	public void sortCards() {
		ArrayList<Card> hearts = new ArrayList<Card>();
		ArrayList<Card> spades = new ArrayList<Card>();
		ArrayList<Card> diamonds = new ArrayList<Card>();
		ArrayList<Card> clubs = new ArrayList<Card>();
		
		while (cards.size() != 0) {
			Card currCard = cards.remove(0);
			
			if (currCard.getSuit().equals("Hearts")) {
				hearts.add(currCard);
			} else if (currCard.getSuit().equals("Spades")) {
				spades.add(currCard);
			} else if (currCard.getSuit().equals("Diamonds")) {
				diamonds.add(currCard);
			} else if (currCard.getSuit().equals("Clubs")) {
				clubs.add(currCard);
			}
		}
		
		cards.addAll(sort(hearts));
		cards.addAll(sort(spades));
		cards.addAll(sort(diamonds));
		cards.addAll(sort(clubs));
	}
	
	private ArrayList<Card> sort(ArrayList<Card> list) {
		for (int i = 0; i < list.size(); i++) { 
			for (int j = 0; j < list.size() - i - 1; j++) 
	        {
				if (list.get(j).getValue() > list.get(j + 1).getValue()) 
	            { 
					Card temp = list.get(j); 
	                list.set(j, list.get(j+1)); 
	                list.set(j + 1, temp); 
	            }
	        }
		}
		return list;
	}
	
	public void dumpCards() throws IOException {
		String log = "";
		ArrayList<Card> dumpedCards = new ArrayList<Card>();
		log += "\n" + playerName + " dumping 5 cards to open pile: \n";
		
		for (int i = 0; i < 5; ++i) {
			Random rand = new Random();
			int index = rand.nextInt(cards.size() - 1);
			dumpedCards.add(cards.remove(index));
			log += dumpedCards.get(i).getName() + " of " + dumpedCards.get(i).getSuit();
			
			if (i < 4) {
				log += ", ";
			}
		}
		System.out.println(log);
		send(dumpedCards);
	}
	
	public void sendCards() throws IOException, ClassNotFoundException {
		send(cards);
	}
	
	public void replaceCard() throws IOException, ClassNotFoundException {
		ArrayList<Card> openPile = receive();
		String log = "";
		String str1 = "\nCards drawn from hand: ", str2 = "Cards drawn from pile: ";
		for (int i = 0; i < 2; ++i) {
			Random rand = new Random();
			
			int index1 = rand.nextInt(openPile.size() - 1);
			int index2 = rand.nextInt(cards.size() - 1);
			
			Card temp = cards.remove(index2);
			Card temp2 = openPile.remove(index1);
			cards.add(temp2);
			openPile.add(temp);
			
			str1 += temp.getName() + " of " + temp.getSuit();			
			str2 += temp2.getName() + " of " + temp2.getSuit();
			
			if (i == 0) {
				str1 += ", ";
				str2 += ", ";
			}
		}
		log += "\n" + playerName + " replacing 2 cards from pile...\n";
		log += str1 + "\n";
		log += str2;
		System.out.println(log);
		send(openPile);
	}
	
	public static void main(String[] args) {
		Scanner input = new Scanner(System.in);
		System.out.print("Enter name of Player: ");
		String name = input.nextLine();
		
		System.out.print("Enter hostname: ");
		String hostname = input.nextLine();
		
		System.out.print("Enter port number: ");
		int port = input.nextInt();
		
		try {
			PlayerClient client = new PlayerClient(name, hostname, port);
			
			System.out.println("\nRequesting handshake with server...\n");
			client.send(client.getPlayerName());

			client.appendCards();
			
			client.sortCards();
			
			System.out.println("Cards received from game server...\n");
			System.out.println("Sorting cards...");
			client.printCards();
			
			client.dumpCards();
			
			Thread.sleep(1000);
			
			client.replaceCard();
		
			Thread.sleep(1000);
			
			client.sendCards();
			
			Thread.sleep(1000);
			
			client.receiveScore();
			
			System.out.println("Client exiting...");
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			input.close();
		}
	}
}