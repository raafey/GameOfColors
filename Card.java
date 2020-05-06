import java.io.Serializable;
public class Card implements Serializable {
	private static final long serialVersionUID = 1L;
	private String name;
	private String suit;
	private int value;
	
	Card() {}
	
	Card(String name, String suit, int value) {
		this.setName(name);
		this.setSuit(suit);
		this.setValue(value);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSuit() {
		return suit;
	}

	public void setSuit(String suit) {
		this.suit = suit;
	}

	public int getValue() {
		return value;
	}
	
	public String getCardInfo() {
		return name + " of " + suit;
	}

	public void setValue(int value) {
		this.value = value;
	}
	
	public void printCard() {
		System.out.println(name + " of " + suit);
	}
}