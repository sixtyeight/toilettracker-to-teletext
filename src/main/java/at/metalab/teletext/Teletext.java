package at.metalab.teletext;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SCPOutputStream;

public class Teletext {

	public static String GREEN = "\u0082";
	public static String YELLOW = "\u0083";
	public static String BLUE = "\u0084";
	public static String MAGENTA = "\u0085";
	public static String CYAN = "\u0086";
	public static String WHITE = "\u0087";
	public static String BLINK = "\u0088";
	public static String DOUBLE = "\u008d";
	
	public static void scp(byte[] data, String remoteDir, String remoteFilename) throws Exception {
		Connection conn = null;
	
		try {
			conn = new Connection("10.20.30.66");
	
			conn.connect();
			boolean isAuthenticated = conn.authenticateWithPassword("pi", "raspberry");
	
			if (isAuthenticated == false) {
				throw new IOException("Authentication failed.");
			}
	
			SCPClient scpClient = new SCPClient(conn);
	
			SCPOutputStream scpOut = null;
	
			try {
				scpOut = scpClient.put(remoteFilename, data.length, remoteDir, "0600");
				IOUtils.write(data, scpOut);
			} finally {
				if (scpOut != null) {
					scpOut.close();
				}
			}
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
	
	}
	
}