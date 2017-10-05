
/**
 * UrlCache Class
 *  
 * @author Shannon Tucker-Jones 10101385 (template code by Majid Ghaderi)
 * @version Oct 5, 2017
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class UrlCache 
{
	//Initialize default port number
	public static final int DEFAULT_PORT = 80;	
	//Initialize default date
	public static final String DEFAULT_DATE = "Thu, 1 Jan 1970 00:00:00 GMT";
	//Initialize HashMap
	public static Map<String, String> catalog = new HashMap<String, String>();

    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw runtime exception.
	 *
     * @throws IOException if encounters any errors/exceptions
     */
	public UrlCache() throws IOException 
	{
		//Initialization code
		File f = new File("catalog.txt");
		
		//If file already exists and is not a directory...
		if(f.exists() && !f.isDirectory())
		{
			//If file is nonempty...
			if(f.length() > 0)
			{
				FileInputStream fstream = null;
				DataInputStream in = null;
				BufferedReader br = null;
				
				try
				{
					//Load catalog.txt
					//Initalize streams and reader
					fstream = new FileInputStream("catalog.txt");
					in = new DataInputStream(fstream);
					br = new BufferedReader(new InputStreamReader(in));
					
					int i = 0;
					String line1;
					String line2;
					
					//Read file two lines at a time
					while ((line1 = br.readLine()) != null && (line2 = br.readLine()) != null)   
					{
						//Remove the newline character from every line
						//line1 = URL = key
						line1 = line1.replaceAll("\n", "");
						//line2 = LastModifiedDate = value
						line2 = line2.replaceAll("\n", "");	
						
						//Put lines into HashMap catalog					
						catalog.put(line1, line2);
					}
				}
				catch (Exception e)
				{
					//Check where error is
					e.printStackTrace();
					e.getMessage();
					throw new IOException();
				}
				finally
				{
					//Close streams  
					if(fstream != null)
						fstream.close();
					if(in != null)
						in.close();
					if(br != null)
						br.close();
				}
			}
		}
		
		//Else file doesn't exist...
		else
		{
			try 
			{
				//Create a new, empty catalog.txt file
				//HashMap catalog will be empty
				f.createNewFile();
			}
			catch (Exception e) 
			{
				//Check where error is
				e.printStackTrace();
				e.getMessage();
				throw new IOException();
			}
		}
	}
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws IOException if encounters any errors/exceptions
     */
	public void getObject(String url) throws IOException 
	{
		//Initialize variables
		List<Object> var = parse(url);
		
		String hostName = (var.get(0)).toString();
		String pathName = (var.get(1)).toString();
		int portNumber = (Integer) var.get(2);
		
		String lastModifiedDate = "";
			
		//If URL is in the catalog, get its lmd
		if(catalog.containsKey(url))
			lastModifiedDate = catalog.get(url);
			
		//Else, give lmd a default value
		//Insert <url,lmd> into the catalog
		else
		{
			lastModifiedDate = DEFAULT_DATE;
			catalog.put(url, lastModifiedDate);
		}
		
		//Initialize socket and streams		
		Socket s = null;		
		InputStream input = null;
		OutputStream output = null;	
		ByteArrayOutputStream bStream = null;
				
		try
		{
			//Create socket
			//Establish TCP connection to server on specified port number
			s = new Socket(hostName,portNumber);
			
			//Get socket's input/output streams
			input = s.getInputStream();
			output = s.getOutputStream();
			
			//Create HTTP GET request
			String getRequest = "GET /" + pathName + " HTTP/1.0" + "\r\n";
			getRequest = getRequest + "Host: " + hostName + "\r\n" + "If-Modified-Since: " + lastModifiedDate + "\r\n\r\n";
			
			//System.out.println(getRequest);
			
			//Convert GET request to a sequence of bytes
			byte[] bRequest = getRequest.getBytes("US-ASCII");
	
			//Write bytes to output stream
			//Flush output stream
			output.write(bRequest);
			output.flush();
			
			//Read the header character by character
			String header = "";
			while(!header.contains("\r\n\r\n"))
				header = header + (char) input.read();

			//If webpage was modified:
			if(header.contains("200 OK"))
			{
				//Initialize byte stream and buffer
				bStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024*1024];
				int bytesRead = -1; 
			
				//While there is still data to read from the input stream...
				while((bytesRead = input.read(buffer)) != -1)
				{
					//Write bytes to the ByteArrayOutputStream
					bStream.write(buffer, 0, bytesRead);
				}
			
				//Flush stream
				bStream.flush();
			
				//Get final byte array
				byte[] bBody = bStream.toByteArray();
								
				//Update the cache
				updateCache(pathName, bBody);
				
				//Extract Last-Modified date from header
				Pattern pattern = Pattern.compile("Last-Modified: (.*?)\r\n");
				Matcher matcher = pattern.matcher(header);
				String lmdHeader = "";
				while(matcher.find())
					lmdHeader = (matcher.group(1));
				
				//Update catalog HashMap
				catalog.put(url, lmdHeader);
				
				//Update catalog.txt
				updateCatalog(url, lmdHeader);
				
				System.out.println("200 OK: The requested page has been modified.");
			}

			//Else the webpage was not modified:
			else if(header.contains("304 Not Modified"))
				System.out.println("304 Not Modified: The requested page has not been modified.");
			
			//Else the GET request was improperly formatted
			else if(header.contains("400 Bad Request"))
				System.out.println("ERROR 400 Bad Request: Your browser sent a request that the server could not understand.");
			
			//Else the requested page was not found
			else if(header.contains("404 Not Found"))
				System.out.println("ERROR 404 Not Found: The requested page could not be found.");
				
			//Else there was another error
			else
			{
				String[] headerSplit = header.split("\r\n");
				System.out.println("UNDEFINED ERROR: Could not retrieve the requested page. First line of header: " + headerSplit[0]);
			}
				
		}
		catch (Exception e)
		{
			//Check where error is
			e.printStackTrace();
			e.getMessage();
			throw new IOException();
		}
		finally
		{
			//Close socket and data streams
			if(s != null)
				s.close();
			if(input != null)
				input.close();
			if(output != null)
				output.close();
			if(bStream != null)
				bStream.close();
		}
	}
	
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     */
	public long getLastModified(String url) 
	{
		//Initialize date format
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
		
		//Set time zone to GMT
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		try
		{
			//Get lastModifiedDate from catalog
			String lmd = catalog.get(url);
			
			//Parse date string
			Date date = format.parse(lmd);
			
			//Return time in milliseconds
			long millis = date.getTime();
			return millis;
		}
		catch (Exception e) 
		{
			//If object not in catalog, throw a RuntimeException
			e.printStackTrace();
			e.getMessage();
			throw new RuntimeException();
		}
	}
	
	/**
     * Updates the cache in the local directory by overwriting the existing data with more recent data
	 *
     * @param path 	pathname of the URL
     * @param body	byte array containing the body of the HTTP response
	 * @throws IOException if encounters any errors/exceptions
     */
	public void updateCache(String path, byte[] body) throws IOException
	{
		//Create a file instance with a directory pathway
		File f = new File(path);
		
		//Make parent directories
		f.getParentFile().mkdirs();
		
		//Create an empty file for the body
		f.createNewFile();
		
		FileOutputStream fos = null;
				
		try
		{
			//Write byte array to the file
			fos = new FileOutputStream(path);
			fos.write(body);
			fos.flush();
		}
		catch (Exception e)
		{
			//Check where error is
			e.printStackTrace();
			e.getMessage();
			throw new IOException();
		}
		finally
		{
			//Close stream
			if(fos != null)
				fos.close();
		}
		return;
	}
	
    /**
     * Updates catalog.txt by either appending new lines to the end of the file or by overwriting an existing url's date with a more recent date
	 *
     * @param url 	URL of the object 
     * @param lmd	LastModifiedDate of the object
	 * @throws IOException if encounters any errors/exceptions
     */
	public void updateCatalog(String url, String lmd) throws IOException
	{
		//Update catalog.txt
		File f = new File("catalog.txt");
				
		//If file is empty...
		if(f.length() == 0)
		{
			FileWriter fw = null;
			
			try
			{
				//Open existing catalog.txt file
				String file = "catalog.txt";
				fw = new FileWriter(file, true);
				
				//Append data to the file
				fw.write(url + "\n");
				fw.write(lmd + "\n");
				
				//Flush stream
				fw.flush();
			}
			catch (Exception e)
			{
				//Check where error is
				e.printStackTrace();
				e.getMessage();
				throw new IOException();
			}
			finally
			{
				//Close writer
				if(fw != null)
					fw.close();
			}
		}
				
		//Else if file is nonempty...
		else
		{
			//Initialize variables
			BufferedReader br = null;
			BufferedWriter bw = null;
			
			String oldFile = "catalog.txt";
			String tmpFile = "catalog1.txt";
			
			String line1;
			String line2;
			
			try
			{
				//Create temp file
				br = new BufferedReader(new FileReader(oldFile));
				bw = new BufferedWriter(new FileWriter(tmpFile));				
				
				boolean replace = false;
				
				//Read the file two lines at a time
				while ((line1 = br.readLine()) != null && (line2 = br.readLine()) != null)   
				{
					//Remove the newline characters
					line1 = line1.replaceAll("\n", "");
					line2 = line2.replaceAll("\n", "");	
						
					//If url is in catalog.txt
					if(line1.equals(url))
					{
						//Update the lastModifiedDate
						//Set a flag that the value was replaced
						line2 = line2.replace(line2, lmd);
						replace = true;
					}

					//Write the url and date to the temp file
					bw.write(line1+"\n");
					bw.write(line2+"\n");
					
				}
				
				//Else the end of file was reached and url was not found
				if(!replace)
				{
					//Append url and date to the temp file
					bw.write(url+"\n");
					bw.write(lmd+"\n");
				}
				
				//Flush stream
				bw.flush();
			}
			catch (Exception e)
			{
				//Check where error is
				e.printStackTrace();
				e.getMessage();
				throw new IOException();
			}
			finally
			{
				//Close reader and writer
				if(br != null)
					br.close();
				if(bw != null)
					bw.close();
			}
			
			//Delete the old catalog file
			File oF = new File(oldFile);
			oF.delete();

			//Rename the temp file to the old file's name
			File nF = new File(tmpFile);
			nF.renameTo(oF);
			
		}	
		
		return;	
	}
	
    /**
     * Parses a given url for its hostName, pathName, and portNumber
	 *
     * @param url 	URL of the object 
	 * @return an ArrayList of Objects containing the hostName, pathName, and portNumber
     */
	public List<Object> parse(String url)
	{
		String host = "";
		String path = "";
		int port = 0;
		
		//If a port is specified...
		if(url.indexOf(':') > 0 && url.indexOf('/') > 0)
		{
			//Extract the port substring and convert it to an integer
			String pn = url.substring(url.indexOf(":") + 1, url.indexOf("/"));
			port = Integer.parseInt(pn);
			
			//Parse for the host
			String[] halve = url.split(":");
			host = halve[0];
			
			//Parse for the path
			halve = url.split("/");
			path = halve[1];
			for(int i = 2; i < halve.length; i++)
				path = path + "/" + halve[i];
		} 
		
		//If no port is specified...
		else
		{
			//Assign the default port number
			port = DEFAULT_PORT;
			
			//Parse for the host
			String[] halve = url.split("/");
			host = halve[0];
			
			//Parse for the path
			path = halve[1];
			for(int i = 2; i < halve.length; i++)
				path = path + "/" + halve[i];		
		}
		
		//Add results to an ArrayList
		List<Object> variables = new ArrayList <Object>(3);

		variables.add(host);
		variables.add(path);
		variables.add(port);
		
		//Return the ArrayList
		return variables;
	}

}
