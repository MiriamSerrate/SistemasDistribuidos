import java.io.Serializable;


public class Value implements Serializable {

	private static final long serialVersionUID = 1L;
	String value;
	int version;
	
	public Value(String value, int version){
		this.value = value;
		this.version = version;
	}
}
