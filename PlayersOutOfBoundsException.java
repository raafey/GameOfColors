public class PlayersOutOfBoundsException extends Exception{
	private static final long serialVersionUID = 1L;

	PlayersOutOfBoundsException() {}
	
	PlayersOutOfBoundsException(String msg) {
		super(msg);
	}
}