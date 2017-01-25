package apollo.util;
import java.io.Serializable;
import java.math.BigInteger;

/**
* The email field is not used for anything, but if you provide it
* it will be stored.
*/
public class Credentials implements Serializable {
	public String username;
	public String email;
	//password is encrypted and in base-16 format
	public String password;

	public Credentials(String u,String e,String p) {
		username=u;
		email=e;
		password=p;
	}

	public static Credentials encrypt(String pk,String username,String email,int password) {
		if (password<1000) {
			throw new IllegalArgumentException("Password must be at least 1000");
		}
		BigInteger bigM = BigInteger.valueOf(password);
		BigInteger bigN = new BigInteger(pk,16);
		//the 65537 is a constant
		BigInteger bigE = BigInteger.valueOf(65537L);

		BigInteger encrypted = bigM.modPow(bigE,bigN);
		return new Credentials(username,email, encrypted.toString(16));
	}
}