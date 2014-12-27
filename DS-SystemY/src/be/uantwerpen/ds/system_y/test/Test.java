package be.uantwerpen.ds.system_y.test;


public class Test {
	public static void main(String[] args) throws Exception {
		String o;
		int hash;
		
		o = "10.0.5.5euftly";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		o = "10.0.5.6cdnd";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		o = "10.0.5.7kdcuo";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		
		System.out.println();
		
		o = "lollies";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		o = "affa";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		o = "pap";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		o = "111";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		o = "zzz";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
	}

}
