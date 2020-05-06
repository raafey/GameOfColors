import java.io.Serializable;
import java.util.ArrayList;

public class SerializableList implements Serializable {
	private ArrayList<Card> cards;
	private static final long serialVersionUID = 10;
	
	SerializableList(ArrayList<Card> cards) {
		this.cards = cards;
	}
	
	ArrayList<Card> getCardList() {
		return cards;
	}
}