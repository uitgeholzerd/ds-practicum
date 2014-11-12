package be.uantwerpen.ds.system_y.test;

import java.util.TreeMap;

public class Test {
	public static void main(String[] args) throws Exception {
		TreeMap<String, Boolean> lockRequests = new TreeMap<String, Boolean>();
		lockRequests.put("test", true);
		lockRequests.put("lol", false);
		lockRequests.put("a", null);
		
		System.out.println("test found: " + lockRequests.containsKey("test"));
		System.out.println("test value: " + lockRequests.get("test"));
		System.out.println("lol found: " + lockRequests.containsKey("lol"));
		System.out.println("lol value: " + lockRequests.get("lol"));
		System.out.println("a found: " + lockRequests.containsKey("a"));
		System.out.println("lol value: " + lockRequests.get("a"));
	}

}
