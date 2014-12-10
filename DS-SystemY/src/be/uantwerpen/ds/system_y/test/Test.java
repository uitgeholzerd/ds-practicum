package be.uantwerpen.ds.system_y.test;

import java.util.Map.Entry;
import java.util.TreeMap;

public class Test {
	public static void main(String[] args) throws Exception {
		TreeMap<String, String> test = new TreeMap<String, String>();
		test.put("test", "123");
		test.put("lol", "roflmao");
		test.put("a", "z");
		
		System.out.println("Print original");
		for (Entry<String, String> entry : test.entrySet()) {
			System.out.println(entry.getKey() + " - " + entry.getValue());
		}
		
		TreeMap<String, String> clone = new TreeMap<String, String>();
		test.putAll(clone);
		clone.put("New", "!!!");
		clone.put("test", "???");
		clone.remove("a");

		System.out.println("\n*****************************");
		System.out.println("Print clone");
		for (Entry<String, String> entry : clone.entrySet()) {
			System.out.println(entry.getKey() + " - " + entry.getValue());
		}

		System.out.println("\n*****************************");
		System.out.println("Print original2");
		for (Entry<String, String> entry : test.entrySet()) {
			System.out.println(entry.getKey() + " - " + entry.getValue());
		}
	}

}
