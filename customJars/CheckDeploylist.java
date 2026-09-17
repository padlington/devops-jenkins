package checkdeploylist;


import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.net.URI;

import java.io.IOException;


public class CheckDeploylist 
{
	private static String  workspaceFolder;
	
    public CheckDeploylist()   
    {  
    	
    }
    
    
    /**
     * Parse AutoDeployList.txt and 
     * 1. Check that all entries have a valid extension
     * 2. Check that all entries have a matching file 
     * @param args
     */
    public static void main(String[] args) 
    {
    	try 
    	{
    		//runTests();
    		//callToolsAPI();
    		   		
    		
    		if (args.length == 3)
    		{
    			workspaceFolder = args[0];
        		String module = args[1];
        		String release = args[2];
    			System.out.println("Checking DBCs for workspace " + args[0]);
    			System.out.println("Checking DBCs for module " + args[1]);
    			System.out.println("Checking DBCs for release " + args[2]);
    			boolean isValid  = readDeployList(module, release);
    			if (!isValid)
    			{
    				System.exit(1);
    			}
    		}
    		else
    		{
    			System.out.println("Usage: checkDBCs.jar <workspace> <module> <release>");
    			System.exit(1);
    		}
    		
        }
    	catch (Exception ex) 
    	{
    		System.out.println("main Exception " + ex.getMessage());
    		System.exit(1);
    	}
    	System.out.println("Check Deploy List jar - Done");
    	System.exit(0);
	}
    
    
    
    
    /**
     * Read AutoDeploylist.txt and then check each entry 
     * has a valid extension and a matching file 
     * @param module
     * @param release
     * @return
     */
    private static boolean readDeployList(String module, String release)
    {
    	boolean isValid = true;
    	
    	ArrayList<String> validExtensions = new ArrayList<String>();
    	validExtensions.add("dbc");
    	validExtensions.add("db2");
    	validExtensions.add("mxs");
    	validExtensions.add("sql");
    	isValid = true;
    	System.out.println("In readDeployList for Module:" + module + " Release: " + release);
    	String filePath = "";
    	    	
    	//We now have a det feature\module for MAS builds
    	filePath = workspaceFolder + "/maximoconfig/" + module + "/dbcs/" + release;
    	
		FileReader fr;
		try
		{
			System.out.println("Reading " + filePath + "/AutoDeployList.txt");
			fr = new FileReader(filePath + "/AutoDeployList.txt");
			//reads the file  
			BufferedReader br = new BufferedReader(fr);  
			StringBuilder sb = new StringBuilder();
			String line;  
			String fileEntry;
			//Read the file from auto deploy list
			while((line = br.readLine()) != null)  
			{  
				fileEntry = line.trim();
				
				//Skip lines which have already been renamed
				if (fileEntry.startsWith("--"))
				{
					continue;
				}
				
				String fileParts[] = fileEntry.split("\\.");
				//Check the file has an extension
				if (fileParts.length < 2)
				{
					System.out.println("In readDeployList Entry missing extension " + fileEntry);
					isValid = false;
					continue;
				}
				//String extension = fileParts[1].toLowerCase();
				String extension = fileParts[fileParts.length-1].toLowerCase();
				
				System.out.println("In readDeployList Checking extension " + extension);
				//Check the extension is valid
				if (!validExtensions.contains(extension))
				{
					System.out.println("In readDeployList Invalid extension " + fileEntry);
					isValid = false;
					continue;
				}
				
				//Now make sure the file exists 
				final File folder = new File(filePath);

		        List<String> result = new ArrayList<String>();

		        boolean found = searchForFile(fileEntry, folder, result);
		        
		        //System.lineSeparator()
		        //if (!result.contains(folder + "\\" + fileEntry))
		        //String fileToFind = folder + System.lineSeparator() + fileEntry;
		        //System.out.println("Searching for file " + fileToFind);
		        
		        	
		        if (!found)
		        {
		        	System.out.println("In readDeployList File not found " + fileEntry);
					isValid = false;
		        }
		        else
		        {
		        	System.out.println("In readDeployList Found file " + fileEntry);
		        }
			}
		}
		catch (FileNotFoundException e)
		{
			//e.printStackTrace();
			System.out.println("readDeployList - FileNotFoundException " + e.getMessage());
			isValid = false;
		} 
		catch (IOException e)
		{
			//e.printStackTrace();
			System.out.println("readDeployList - IOException " + e.getMessage());
			isValid = false;
		}
		if (isValid)
		{
			System.out.println("Leaving readDeployList - Success");	
		}
		else
		{
			System.out.println("Leaving readDeployList - Failed");
		}
		
		return isValid;
    }

    /**
     * Search for the given file in the DBC's folder
     * @param pattern
     * @param folder
     * @param result
     */
    private static boolean searchForFile(final String pattern, final File folder, List<String> result)
    {
    	boolean found = false;
    	for (final File f : folder.listFiles()) {

            //if (f.isDirectory()) {
            //	searchForFile(pattern, f, result);
            //}

            if (f.isFile()) {
                if (f.getName().matches(pattern)) {
                    result.add(f.getAbsolutePath());
                    found = true;
                    break;
                }
            }
        }
    	return found;
    }
    
    private static void runTests()
    {
    	
    	//String fileEntry = "AJ_MS_09.017.COMMTEMPLATE.dbc";
    	String fileEntry = "AJ_MS_09_017_COMMTEMPLATE.dbc";
    	
    	String fileParts[] = fileEntry.split("\\.");
    	
    	if (fileParts.length < 2)
		{
			System.out.println("In readDeployList Entry missing extension " + fileEntry);
			
		}
		String extension = fileParts[fileParts.length-1].toLowerCase();
		
    	//det folder has no module name
    	//e.g. C:\Jenkins\workspace\DETMaximoDEV3\maximoconfig\dbcs\CR02.M33
		//detim
    	//e.g. C:\Jenkins\workspace\DETMaximoDEV3\maximoconfig\detim\dbcs\CR03.IM01
    	//workspaceFolder = "C:\\Users\\adlingtonp\\WorkspaceNew\\check-deploylist\\inputs\\ws1";
    	//boolean res =  readDeployList("det", "CR02.M33");
    	
    	workspaceFolder = "C:\\Users\\adlingtonp\\WorkspaceNew\\check-deploylist\\inputs\\ws2";
    	boolean res = readDeployList("detim", "CR03.IM01");
    	
    }
}