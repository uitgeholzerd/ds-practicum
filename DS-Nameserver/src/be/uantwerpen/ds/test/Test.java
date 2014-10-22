package be.uantwerpen.ds.test;

import java.net.InetAddress;
import java.net.Socket;

public class Test {
	public static void main(String[] args) throws Exception {
		Socket s = new Socket("8.8.8.8", 53);
		InetAddress address = s.getLocalAddress();
		System.out.println(address.getHostAddress());
		s.close();
	}

}
