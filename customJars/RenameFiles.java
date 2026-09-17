package renamefiles;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;



public class RenameFiles 
{
	private static String  workspaceFolder;
	
    public RenameFiles()   
    {  
    	
    }
    
    
    /**
     * Rename files using AutoDeployList.txt and maxrelease.txt 
     * @param args
     */
    public static void main(String[] args) 
    {
    	try 
    	{
    		if (args.length == 3)
    		{
        		workspaceFolder = args[0];
        		String module = args[1];
        		String release = args[2];
        		
    			System.out.println("Renaming DBCs for workspace " + args[0]);
    			System.out.println("Renaming DBCs for module " + args[1]);
    			System.out.println("Renaming DBCs for release " + args[2]);
    			//module or feature e.g detmas
    			//release e.g. Release1
    			boolean isValid  = renameDBCs(module, release);
    			if (!isValid)
    			{
    				System.out.println("Rename DBCs - Error");
    				System.exit(1);
    			}
    		}
    		else
    		{
    			System.out.println("Usage: renameDBCs.jar <workspace> <module> <release>");
    			System.exit(1);
    		}
    		
        }
    	catch (Exception ex) 
    	{
    		System.out.println("Rename DBCs Exception " + ex.getMessage());
    		System.exit(1);
    	}
    	System.out.println("Rename DBCs - Done");
    	System.exit(0);
	}
    
       
    
    /**
     * 1. Get last used filename from maxrelease.txt 
     * 2. Rename DBCs and copy to tools folder
     * 3. Update AutoDeployList.txt
     * 4. Update product.xml
     * 5. Update maxrelease.txt
     * @param module e.g. detmas
     * @param release e.g. Release1
     * @return
     */
    private static boolean renameDBCs(String module, String release)
    {
    	boolean isValid = true;

    	isValid = true;
    	System.out.println("In renameDBCs for Module:" + module + " Release: " + release);
    	String filePath = "";
    	
    	//We now have a detmas module for MAS builds
    	//Release is currently Release1

    	
		FileReader fr;
		try
		{
			String line;
			String lastVersionStr;
			String nextVerStr = "";
			int lastVer = 0;
			String prefix = "";
			
			//Get latest version from maxrelease.txt e.g. V9000_12
	    	filePath = workspaceFolder + "/maximoconfig/" + module + "/dbcs";
			fr = new FileReader(filePath + "/maxrelease.txt");
			BufferedReader br = new BufferedReader(fr);
			//Read the file from auto deploy list
			while((line = br.readLine()) != null)  
			{  
				lastVersionStr = line.trim();
				String[] bits = lastVersionStr.split("_");
				prefix = bits[0];
				lastVer = Integer.parseInt(bits[1]);
			}
			int nextVer = lastVer + 1;
			fr.close();
			System.out.println("renameDBCs - Next version is " + nextVer);
			
			//Now rename the files 
			//First write to AutoDeployList.tmp
			//Then copy AutoDeployList.tmp to AutoDeployList.txt
			//--V9000_01.dbc:DET-DUMMY-01.dbc 
			//DETEPEXIT-AS.dbc 
			//DETCASESWO-EP.dbc 
			
		    FileReader fr1;
	    	filePath = workspaceFolder + "/maximoconfig/" + module + "/dbcs/" + release;
		    FileWriter fw = new FileWriter(filePath + "/AutoDeployList.tmp");
		    //BufferedWriter writer = new BufferedWriter(fw);
		    
			fr1 = new FileReader(filePath + "//AutoDeployList.txt");
			//reads the file  
			BufferedReader br1 = new BufferedReader(fr1);  
			StringBuilder sb = new StringBuilder();
			  
			String fileEntry;
			int noOfNewDCs = 0;
			//Read the file from auto deploy list
			while((line = br1.readLine()) != null)  
			{  
				fileEntry = line.trim();
				
				//Just rewrite lines where files have already been renamed
				if (fileEntry.startsWith("--"))
				{
			        fw.write(fileEntry + System.lineSeparator());
					continue;
				}
				
				//Process a new file
				String newFileEntry = "";
				String newFileName = prefix + "_";
				if (nextVer < 10)
				{
					newFileName = newFileName + "0" + nextVer +  ".dbc";
					nextVerStr = "0" + nextVer;
				}
				else
				{
					newFileName = newFileName + nextVer +  ".dbc";
					nextVerStr = nextVerStr + nextVer;
				}
				
				newFileEntry = "--" + newFileName + ":" + fileEntry;
				//Write new file to tmp file
				fw.write(newFileEntry + System.lineSeparator());
				
				//next copy the file to the tools\en\module folder
				String sourcePathStr = workspaceFolder + "/maximoconfig/" + module + "/dbcs/" + release + "/" + fileEntry;
				Path sourcePath = Paths.get(sourcePathStr);
				String targetFileStr = workspaceFolder + "/maximoconfig/" + module + "/tools/en//" + module + "/" + newFileName;
				Path targetPath = Paths.get(targetFileStr);
				Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("renameDBCs - Source " + sourcePathStr + " copied to " + targetFileStr);
				nextVer++;
				noOfNewDCs++;
			}
			
			fw.close();
			fr1.close();
			if (noOfNewDCs > 0)
			{
				System.out.println("renameDBCs - Added " + noOfNewDCs + " file to AutoDeployList.tmp");				
		
				//Copy AutoDeployList.tmp to AutoDeployList.txt
				String sourcePathStr = workspaceFolder + "/maximoconfig/" + module + "/dbcs/" + release + "/AutoDeployList.tmp";
				Path sourcePath = Paths.get(sourcePathStr);
				String targetFileStr = workspaceFolder + "/maximoconfig/" + module + "/dbcs/" + release + "/AutoDeployList.txt";
				Path targetPath = Paths.get(targetFileStr);
				Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("renameDBCs - Copied AutoDeployList.tmp to AutoDeployList.txt");
				
				//Delete AutoDeployList.tmp 
				Files.deleteIfExists(sourcePath);
				System.out.println("renameDBCs - deleted AutoDeployList.tmp");
				
				//Update maxrelease.txt e.g from  V9000_00 to V9000_02
				filePath = workspaceFolder + "/maximoconfig/" + module + "/dbcs";
				FileWriter fw2 = new FileWriter(filePath + "/maxrelease.txt");
				fw2.write(prefix + "_" + nextVerStr + System.lineSeparator());
				fw2.close();
				System.out.println("renameDBCs - Updated maxrelease.txt");
				
				//Update product.xml
				FileWriter fw3 = new FileWriter(workspaceFolder + "/maximoconfig/" + module + "/product/" + module + ".tmp");
				FileReader fr3;
				fr3 = new FileReader(workspaceFolder + "/maximoconfig/" + module + "/product/" + module + ".xml");
				//reads the file  
				BufferedReader br3 = new BufferedReader(fr3);  
				  
				//Read the file from auto deploy list
				while((line = br3.readLine()) != null)  
				{  
					if (line.contains("<dbversion>"))
					{
						fw3.write("<dbversion>" + prefix + "-" + nextVerStr + "</dbversion>" +  System.lineSeparator());
					}
					else
					{
						fw3.write(line + System.lineSeparator());
					}
				}
				fw3.close();
				fr3.close();
				
				//Copy tmp file to product xml file
				sourcePathStr = workspaceFolder + "/maximoconfig/" + module + "/product/" + module + ".tmp";
				sourcePath = Paths.get(sourcePathStr);
				targetFileStr = workspaceFolder + "/maximoconfig/" + module + "/product/" + module + ".xml";
				targetPath = Paths.get(targetFileStr);
				Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("renameDBCs - Updated product.xml");

				//Delete module.tmp 
				Files.deleteIfExists(sourcePath);
				System.out.println("renameDBCs - deleted temp xml file");
				
				
			}
			{
				System.out.println("renameDBCs - No new files to add");
				String sourcePathStr = workspaceFolder + "/maximoconfig/" + module + "/dbcs/" + release + "/AutoDeployList.tmp";
				Path sourcePath = Paths.get(sourcePathStr);
				Files.deleteIfExists(sourcePath);
				System.out.println("renameDBCs - deleted AutoDeployList.tmp");
			}
		}
		catch (FileNotFoundException e)
		{
			//e.printStackTrace();
			System.out.println("renameDBCs - FileNotFoundException " + e.getMessage());
			isValid = false;
		} 
		catch (IOException e)
		{
			//e.printStackTrace();
			System.out.println("renameDBCs - IOException " + e.getMessage());
			isValid = false;
		}
		if (isValid)
		{
			System.out.println("Leaving renameDBCs - Success");	
		}
		else
		{
			System.out.println("Leaving renameDBCs - Failed");
		}
		
		return isValid;
    }

    
    private static void runTest1()
    {
    	
    	// **** Before ****
    	//maxrelease.txt = V9000_00 
    	//AutoDeployList.txt - before 
    	//DET-DUMMY-01.dbc 
    	//DETEPEXIT-AS.dbc 
    	
    	// product.xml <dbversion>V9000-00</dbversion> 
    	
    	// ***** After *****
    	//maxrelease.txt = V9000_02
    	//in tools/en/detmas
    	//V9000_01.dbc
    	//V9000_02.dbc
    	
    	//AutoDeployList.txt - after
    	// --V9000_01.dbc:DET-DUMMY-01.dbc 
    	// --V9000_02.dbc:DETEPEXIT-AS.dbc  
    	
    	//product.xml <dbversion>V9000-02</dbversion>
    	
    	workspaceFolder = "C:\\Users\\adlingtonp\\WorkspaceNew\\check-deploylist\\inputs\\ws2";
    	boolean res = renameDBCs("detmas", "CR03.IM01");
    	
    }
    
    private static void runTest2()
    {
    	
    	// ******** Before  ********
    	//maxrelease.txt = V9000_11 
    	//AutoDeployList.txt - before 
    	/*
    	 * --V9000_01.dbc:DET-DUMMY-01.dbc 
			--V9000_02.dbc:DETEPEXIT-AS.dbc 
			--V9000_03.dbc:DETCASESWO-EP.dbc 
			--V9000_04.dbc:DETIPMWOAPPR-EP.dbc 
			--V9000_05.dbc:DETIPMWOUPD-EP.dbc 
			--V9000_06.dbc:DETORAASSNEW-EP.dbc 
			--V9000_07.dbc:DETORAASSUPD-EP.dbc 
			--V9000_08.dbc:DETORAINV-EP.dbc 
			--V9000_09.dbc:DETORAWO-EP.dbc 
			--V9000_10.dbc:SET-EXT-MSGID-WONUM-EP.dbc 
			--V9000_11.dbc:AG_01_LOCASSETSTATUS_Fix.dbc 
			 AG_02_New_Row_Fix.dbc 
             AG_03_Map_App_Auth.dbc 
             YT_29_12_2025_DETHAL.mxs 
    	 */
    	// product.xml <dbversion>V9000-11</dbversion>
    	
    	//After
      	//maxrelease.txt = V9000_14
    	//in tools/en/detmas
    	//V9000_01.dbc
    	//V9000_02.dbc
    	
    	//AutoDeployList.txt - before 
    	/*
    	 * --V9000_01.dbc:DET-DUMMY-01.dbc 
			--V9000_02.dbc:DETEPEXIT-AS.dbc 
			--V9000_03.dbc:DETCASESWO-EP.dbc 
			--V9000_04.dbc:DETIPMWOAPPR-EP.dbc 
			--V9000_05.dbc:DETIPMWOUPD-EP.dbc 
			--V9000_06.dbc:DETORAASSNEW-EP.dbc 
			--V9000_07.dbc:DETORAASSUPD-EP.dbc 
			--V9000_08.dbc:DETORAINV-EP.dbc 
			--V9000_09.dbc:DETORAWO-EP.dbc 
			--V9000_10.dbc:SET-EXT-MSGID-WONUM-EP.dbc 
			--V9000_11.dbc:AG_01_LOCASSETSTATUS_Fix.dbc 
			--V9000_12.dbc: AG_02_New_Row_Fix.dbc 
            --V9000_13.dbc: AG_03_Map_App_Auth.dbc 
             --V9000_14.mxs:YT_29_12_2025_DETHAL.mxs 
    	 */  
    	
    	// product.cml <dbversion>V9000-14</dbversion>
    	//
    	
    	workspaceFolder = "C:\\Users\\adlingtonp\\WorkspaceNew\\check-deploylist\\inputs\\detmas";
    	boolean res = renameDBCs("detim", "CR03.IM01");
    	
    }

}