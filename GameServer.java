import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.Scanner;

public class GameServer {
	private ServerSocket welcomeSocket;
	private Socket clientSockets[];
	private int numDecks;
	private int numClients;
	private ArrayList<Card> pile;		
	private ArrayList<Card> openPile;
	private String gameColor;
	private int playerIndex;
	private String players[];
	private int score[];
	
	GameServer(int port) throws IOException {
		Scanner input = new Scanner(System.in);

		System.out.print("Enter number of clients: ");
		numClients = input.nextInt();
		
		System.out.print("Enter number of decks: ");
		numDecks = input.nextInt();
		
		input.close();
		
		try {
			if (numClients < 2 || numClients > 4)
				throw new PlayersOutOfBoundsException("Number of players is out of bounds!");
			
			if (numDecks < 1 || numDecks > 4) 
				throw new NumDecksOutOfBoundsException("Number of decks is out of bounds!");	
		} catch (NumDecksOutOfBoundsException n) {
			n.printStackTrace();
		} catch (PlayersOutOfBoundsException p) {
			p.printStackTrace();
		}
		
		welcomeSocket = new ServerSocket(port);
		clientSockets = new Socket[numClients];
		players = new String[numClients];
		score = new int[numClients];
		pile = new ArrayList<Card>();
		openPile = new ArrayList<Card>();
		
		for (int i = 0; i < numDecks; ++i)
			pile.addAll(generateDeck());
		
		shufflePile();
	}
	
	private void shufflePile() {
		Collections.shuffle(pile);
	}
	
	private ArrayList<Card> generateDeck() {
		ArrayList<Card> deck = new ArrayList<Card>();
		
		String[] suits = {"Hearts", "Spades", "Diamonds", "Clubs"};
		
		for (int i = 0; i < suits.length; ++i) {
			deck.add(new Card("A", suits[i], 14));
			deck.add(new Card("K", suits[i], 13));
			deck.add(new Card("Q", suits[i], 12));
			deck.add(new Card("J", suits[i], 11));
			
			for (int j = 2; j < 11; ++j) {
				deck.add(new Card(Integer.toString(j), suits[i], j));
			}
		}
		return deck;
	}
	
	private void distributeCards() throws IOException {
		ArrayList<ArrayList<Card>> hands = new ArrayList<ArrayList<Card>>();
		
		for (int i = 0; i < numClients; ++i) {
			ArrayList<Card> newCards = new ArrayList<Card>();
			hands.add(newCards);
		}
		
		int currPlayer = 0;
		while (pile.size() != 0) {
			ArrayList<Card> temp = hands.get(currPlayer);
			temp.add(pile.remove(0));
			hands.set(currPlayer, temp);
			
			if (currPlayer == numClients - 1)
				currPlayer = -1;
			++currPlayer;
		}
		
		for (int i = 0; i < numClients; ++i)
			send(i, hands.get(i));
	}
	
	private void initialDistribute() {
		boolean foundJack = false;
	
		System.out.println("\nDistributing cards to decide color...\n");
		
		int i = 0;
		while (!foundJack) {
			Card currCard = pile.remove(0);
			
			if (currCard.getName().equals("J")) {
				playerIndex = i;
				
				foundJack = true;
				
				String[] suits = {"Hearts", "Spades", "Diamonds", "Clubs"};
				Random rand = new Random();
				int index = rand.nextInt(suits.length - 1);
				gameColor = suits[index];
				System.out.println(players[i] + " got a " + currCard.getName()+ " of " + currCard.getSuit());
				System.out.println(players[playerIndex] + " chooses " + gameColor  + " as color of the game...");
			}
			else {
				System.out.println(players[i] + " got a " + currCard.getName()+ " of " + currCard.getSuit());
			}
			
			if (i == numClients - 1) {
				i = -1;
			}
			++i;
		}
	}
	
	private void resetPile() {
		ArrayList<Card> newPile = new ArrayList<Card>();
		for (int i = 0; i < numDecks; ++i) {
			newPile.addAll(generateDeck());
		}
		pile = newPile;
		shufflePile();
	}
	
	public void simulate() throws IOException, InterruptedException, ClassNotFoundException {
		initialDistribute();
		
		System.out.println("\nResetting Pile and re-distributing cards to players...");
		resetPile();
		
		distributeCards();
		
		ServerThread serverThreads[] = new ServerThread[numClients];
		
		Semaphore sem = new Semaphore(1, true);
		
		for (int i = 0; i < numClients; ++i) {
			serverThreads[i] = new ServerThread(players[i], openPile, clientSockets[i], sem);
		}
		
		serverThreads[playerIndex].setPriority(10);
		
		for (int i = 0; i < numClients; ++i) {
			serverThreads[i].start();
		}
		
		for (int i = 0; i < numClients; ++i) {
			serverThreads[i].join();
		}
		
		String toSend = "";
		for (int i = 0; i < numClients; ++i) {
			ArrayList<Card> hand = receiveObject(i);
			toSend += calculateScore(hand, i);
		}
		toSend += decideWinner();
		
		for (int i = 0; i < numClients; ++i) {
			send(toSend, i);
		}
		
		System.out.println("\n" + toSend);
		
		System.out.println("Server exiting...");
	}
	
	public String calculateScore(ArrayList<Card> cards, int index) throws IOException {
		String log = "";
		for (int i = 0; i < cards.size(); ++i) {
			if (cards.get(i).getSuit().equals(gameColor))
				score[index] += 2 * cards.get(i).getValue();
			else
				score[index] += cards.get(i).getValue();
		}
		log += players[index] + "'s final score: " + score[index] + "\n";
		return log;
	}
	
	private String decideWinner() {
		String log = "";
		int max  = score[0];
		for (int i = 1; i < numClients; i++) {
			if (max < score[i] ) {
				max = score[i];
			}
		}
		
		log += "\nWinner(s) of the Game: \n";		
		for (int i = 0; i < numClients; i++) {
			if (score[i] == max)
				log += players[i] + " with Score: " + score[i] + "\n";	
		}
		
		return log;
	}
	
	public void listen() throws IOException {
		System.out.println("\nListening for requests from " + numClients + " clients...");
		for (int i = 0; i < numClients; ++i) {
			clientSockets[i] = welcomeSocket.accept();
			players[i] = receive(i);
			System.out.println("Request received from client: " + players[i] + ", with IP: " + clientSockets[i].getLocalAddress().getHostAddress());
		}
	}
	
	public String receive(int sockID) throws IOException {
		InputStream sin = clientSockets[sockID].getInputStream();
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(sin));
		String buffer = inFromClient.readLine();
		return buffer;
	}
	
	public ArrayList<Card> receiveObject(int sockID) throws IOException, ClassNotFoundException {
		InputStream is = clientSockets[sockID].getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		SerializableList received = (SerializableList)ois.readObject();
		return received.getCardList();
	}
	
	public void send(int sockID, ArrayList<Card> cards) throws IOException {
		OutputStream os = clientSockets[sockID].getOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(os);
		SerializableList toSend = new SerializableList(cards);
		out.writeObject(toSend);
	}
	
	public void send(String message, int index) throws IOException {
	    PrintWriter out = new PrintWriter(clientSockets[index].getOutputStream(), true);
	    out.println(message);
	}
	
	public void close() throws IOException {
		for (int i = 0; i < numClients; ++i)
			clientSockets[i].close();
		welcomeSocket.close();
	}
	
	public int getNumClients() {
		return numClients;
	}
	
	public static void main(String args[]) {
		Scanner input = new Scanner(System.in);
		System.out.print("Enter port number: ");
		int port = input.nextInt();
		
		try {
			GameServer s = new GameServer(port);
			
			s.listen();
			
			s.simulate();
			
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			input.close();
		}
	}
}
