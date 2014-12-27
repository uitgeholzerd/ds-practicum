package be.uantwerpen.ds.system_y.test;


public class Test {
	public static void main(String[] args) throws Exception {
		String o;
		int hash;
		
		o = "10.0.5.5sb";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		o = "10.0.5.6h";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		o = "10.0.5.7idpdf";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		

		o = "111";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		o = "zzz";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		o = "lollies";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
		o = "pap";
		hash = Math.abs(o.hashCode()) % (int) Math.pow(2, 15);
		System.out.println(o + " - " + hash);
	}

}
