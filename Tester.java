
/**
 * A simple test driver
 * 
 * @author 	Majid Ghaderi
 * @version	3.2, Sep 22, 2017
 *
 */

import java.io.IOException;

public class Tester {
	
	public static void main(String[] args) {

		// include whatever URL you like
		// these are just some samples
		String[] url = {"people.ucalgary.ca/~mghaderi/index.html",
						"people.ucalgary.ca/~mghaderi/test/uc.gif",
						"people.ucalgary.ca/~mghaderi/test/a.pdf",
						"people.ucalgary.ca:80/~mghaderi/test/test.html"};
		
		// this is a very basic tester
		// the TAs will use a more comprehensive set of tests
		try {
			UrlCache cache = new UrlCache();
			
			for (int i = 0; i < url.length; i++)
				cache.getObject(url[i]);
			
			System.out.println("\nLast-Modified for " + url[0] + " is: " + cache.getLastModified(url[0]) + "\n");
			cache.getObject(url[0]);
			System.out.println("\nLast-Modified for " + url[0] + " is: " + cache.getLastModified(url[0]) + "\n");
		}
		catch (IOException e) {
			System.out.println("\nThere was a problem: " + e.getMessage());
		}
	}
	
}
