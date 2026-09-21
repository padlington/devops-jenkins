package calltoolsapi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.time.Duration;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONObject;
 
import java.nio.file.Files;

/**
 * Call various tools APIs in the maxinst pod and the system info
 * API in the manage pod
 * Used by Jenkins to initiate and monitor deployments
 */
public class CallToolsAPI 
{
	private static String  version = "1.3";
	
	private static final HttpClient GLOBAL_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .sslContext(insecureContext())
            .build();

	
	
    public CallToolsAPI()   
    {  
    }
   
    
     /**
     * Call the tools api in the MAXINST Pod 
     * Valid Actions are 
     * GETBLDSTATUS - call toolsapi/toolservice/getbuildstatus
     * GETARCHLIST - call toolsapi/toolservice/updatecustomizationarchive with mode = GET 
     * ADDARCH - call toolsapi/toolservice/updatecustomizationarchive with mode ADD or UPDATE
     * CHECKDEPLOYMENT - wait for maxinst pod to stop\start and then manage pod
     * for maxinst pod just loop and call toolsapi/toolservice/getbuildstatus
     * for manage loop and call maximo/api/systeminfo?lean=1
     * SENDEMAIL - call the power platform API to generate the Teams message
     * 
     * @param args
     */
    public static void main(String[] args) 
    {
    	try 
    	{
    		System.out.println("CallToolsAPI version " + version);    		    		
    		boolean success  = false; 
    		if (args.length > 0)
    		{
    			String action = args[0];
    			System.out.println("Action is " + action);
    			if (action.equalsIgnoreCase("GETBLDSTATUS"))
    			{
    				if (args.length != 4)
    				{
    					System.out.println("Usage: callToolsAPI.jar <action> <apikey> <tgtsystem> <workspacefolder>");
    					System.exit(1);
    				}
    				String apiKey = args[1];
    				String tgtSystem = args[2];
    				String workspaceFolder = args[3];
    				String maxinstUrl = getMaxInstUrl(tgtSystem, workspaceFolder);
    				
    				if (maxinstUrl == null)
    				{
    					System.out.println("Invalid target system " + tgtSystem);
    		        	
    				}
    				else
    				{
    					success = getBuildStatus(maxinstUrl, apiKey);
    				}
    			}
    			else if (action.equalsIgnoreCase("GETARCHLIST"))
    			{
    				if (args.length != 4)
    				{
    					System.out.println("Usage: callToolsAPI.jar <action> <apikey> <tgtsystem> <workspacefolder>");
    					System.exit(1);
    				}
    				String apiKey = args[1];
    				String tgtSystem = args[2];
    				String workspaceFolder = args[3];
    				
    				String maxinstUrl = getMaxInstUrl(tgtSystem, workspaceFolder);
    				
    				if (maxinstUrl == null)
    				{
    					System.out.println("Invalid target system " + tgtSystem);
    		        	
    				}
    				else
    				{
    					JSONObject jo = getArchiveList(maxinstUrl,apiKey);
    					if (jo != null)
    					{
    						success = true;
    					}
    					else
    					{
    						success = false;
    					}
    				}
    			}
    			else if (action.equalsIgnoreCase("ADDARCH"))
    			{
    				if (args.length != 6)
    				{
    					System.out.println("Usage: callToolsAPI.jar <action> <apikey> <tgtsystem> <archivename> <archiveurl> <workspaceFolder>");
    					System.exit(1);
    				}
    				String apiKey = args[1];
    				String tgtSystem = args[2];
    				String archiveName = args[3];
    				String archiveUrl = args[4];
    				String workspaceFolder = args[5];
    				success = addArchive(tgtSystem, apiKey, archiveName, archiveUrl, workspaceFolder);
    			}
    			else if (action.equalsIgnoreCase("ADDARCHTEST"))
    			{
    				if (args.length != 6)
    				{
    					System.out.println("Usage: callToolsAPI.jar <action> <apikey> <tgtsystem> <archivename> <archiveurl> <workspaceFolder>");
    					System.exit(1);
    				}
    				String apiKey = args[1];
    				String tgtSystem = args[2];
    				String archiveName = args[3];
    				String archiveUrl = args[4];
    				String workspaceFolder = args[5];
    				success = addArchiveTest(tgtSystem, apiKey, archiveName, archiveUrl, workspaceFolder);
    			}
    			else if (action.equalsIgnoreCase("DELARCH"))
    			{
    				if (args.length != 5)
    				{
    					System.out.println("Usage: callToolsAPI.jar <action> <apikey> <maxinsturl> <archfile> <url>");
    					System.exit(1);
    				}
    				String apiKey = args[1];
    				String maxinstUrl = args[2];
    				String archiveName = args[3];
    				String archiveUrl = args[4];
    				success = deleteArchive(maxinstUrl, apiKey, archiveName, archiveUrl);
    			}
    			else if (action.equalsIgnoreCase("CHECKMAXIMO"))
    			{
    				String apiKey = args[1];
    				String manageUrl = args[2];
    				String waitTimeStr = args[3];
    				int waitTime = Integer.parseInt(waitTimeStr);
    				success = checkMaximoStatus(apiKey, manageUrl, waitTime);
    			}
    			else if (action.equalsIgnoreCase("CHECKBUILD"))
    			{
    				String apiKey = args[1];
    				String maxinstUrl = args[2];
    				String waitTimeStr = args[3];
    				int waitTime = Integer.parseInt(waitTimeStr);
    				success = checkBuildStatus(apiKey, maxinstUrl, waitTime);
    			}
    			else if (action.equalsIgnoreCase("CHECKMAXIMOLOOP"))
    			{
    				String apiKey = args[1];
    				String manageUrl = args[2];
    				String waitTimeStr = args[3];
    				int waitTime = Integer.parseInt(waitTimeStr);
    				success = checkMaximoStatusLoop(apiKey, manageUrl, waitTime);
    			}
    			else if (action.equalsIgnoreCase("CHECKDEPLOYMENT"))
    			{
    				if (args.length != 6)
    				{
    					System.out.println("Usage: callToolsAPI.jar <action> <apikey> <tgtsystem> <nextseqstr> <waittime> <workspaceFolder>");
    					System.exit(1);
    				}
    				
    				String apiKey = args[1];
    				String tgtSystem = args[2];
    				String versionStr = args[3];
    				String waitTimeStr = args[4];
    				int waitTime = Integer.parseInt(waitTimeStr);
    				String workspaceFolder = args[5];
    				success = checkDeploymentStatus(apiKey, tgtSystem, versionStr, waitTime, workspaceFolder);
    				
    			}
    			else if (action.equalsIgnoreCase("SENDEMAIL"))
    			{
    				String message = args[1];
    				String teamsKey = args[2];
    				String debugFlag = args[3];
    				success = sendEmail(message, teamsKey, debugFlag);
    			}
    			
    			if (!success)
    			{
    				System.out.println("Calls Tools API - Error");
    				System.exit(1);
    			}
    		}
    		else
    		{
    			System.out.println("Usage: callToolsAPI.jar <action> <maxinsturl> <archfile> <url>");
    			System.out.println("Valid actions GETBLDSTATUS GETARCHLIST ADDARCH CHECKDEPLOYMEMNT SENDEMAIL");
    			System.exit(1);
    		}
    		
        }
    	catch (Exception ex) 
    	{
    		System.out.println("main Exception " + ex.getMessage());
    		System.exit(1);
    	}
    	System.out.println("Calls Tools API - Success");
    	System.exit(0);
	}
        
    
    /**
     * Delete the given archive  
     */
    private static boolean deleteArchive(String maxinstUrl, String apiKey, String archiveName, String archiveUrl)
	{
		System.out.println("deleteArchive entering");
		boolean res = true;
				
    	String requestBody = "{ \"name\": \"" + archiveName 
        		               + "\", \"mode\": \"delete\", \"url\": \"" 
        		              + archiveUrl +  "\" }";
         
        String tgtUrl = "https://" + maxinstUrl + "/toolsapi/toolservice/updatecustomizationarchive";
        
        try {

        	HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tgtUrl)) 
                .header("Content-Type", "application/json") 
                .header("apikey", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)) 
                .build();

           HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

           System.out.println("Status Code: " + response.statusCode());
           System.out.println("Response Body: " + response.body());
           if (response.statusCode() != 200)
           {
        	   System.out.println("Error calling API");
        	   res = false;
           }
        }
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        	res = false;
        }
		return res;
	}

    /**
     * Add the given archive  
     * If the archive already exists it must be an Update call 
     */
	private static boolean addArchive(String tgtSystem, String apiKey, String archiveName, String archiveUrl, String workspaceFolder)
	{
		System.out.println("addArchive entering");
		boolean res = true;
		
		try {
			
			String mode = "add";
			
			String maxinstUrl = getMaxInstUrl(tgtSystem,workspaceFolder);
			
			if (maxinstUrl == null)
			{
				System.out.println("Invalid target system " + tgtSystem);
	        	return false;
			}
			
			if (archiveExists(maxinstUrl, apiKey, archiveName, archiveUrl))
			{
				System.out.println("addArchive archive exists so must do an update");
				//deleteArchive(maxinstUrl, apiKey, archiveName, archiveUrl);
				mode = "update";
			}
			
	    	String requestBody = "";
	    	if (archiveUrl.contains("naviam"))
			{
				requestBody = "{\"name\": \"" + archiveName + "\",  \"secretname\": \"mas-manage-cl2--cac--sn\", " 
	        		               +  "\"mode\": \"" + mode + "\", \"url\": \"" 
	        		              + archiveUrl +  "\" }";
			}
			else
			{
				requestBody = "{\"name\": \"" + archiveName + "\",  " 
 		               +  "\"mode\": \"" + mode + "\", \"url\": \"" 
 		              + archiveUrl +  "\" }";
			}
	        
	        System.out.println("addArchive body is " + requestBody);
	        String tgtUrl = "https://" + maxinstUrl + "/toolsapi/toolservice/updatecustomizationarchive";
	        System.out.println("addArchive tgtUrl is " + tgtUrl);
	        	
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create(tgtUrl)) 
	                .header("Content-Type", "application/json") 
	                .header("apikey", apiKey)
	                .POST(HttpRequest.BodyPublishers.ofString(requestBody)) 
	                .build();
	
	        HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
	
	        System.out.println("Status Code: " + response.statusCode());
	        System.out.println("Response Body: " + response.body());
	        if (response.statusCode() != 200)
	        {
	        	System.out.println("Error calling API " + response.body());
	        	res = false;
	        }
		}
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        	res = false;
        }
		return res;
	}
	
	/**
     * Add the given archive  
     * If the archive already exists it must be an Update call 
     */
	private static boolean addArchiveNoURL(String tgtSystem, String apiKey, String archiveName, String baseUrl, String module,
			String workspaceFolder)
	{
		System.out.println("addArchive entering");
		//Read maxrelease.txt 
		//C:\Users\adlingtonp\repos\VSBAMAS\maximoconfig\detmas\dbcs\maxrelease.txt
		//workspace/module/dbcs/maxrelease.txt
		//e.g. V9000_59
		FileReader fr;
				
		boolean res = true;
		
        try {
			String filePath = workspaceFolder + "//maximoconfig//" + module + "//dbcs";
			
			fr = new FileReader(filePath + "//maxrelease.txt");
			BufferedReader br = new BufferedReader(fr);
			String lastVersionStr = "";
			String line = "";
			//Read the file from auto deploy list
			while((line = br.readLine()) != null)  
			{  
				lastVersionStr = line.trim();
				//String[] bits = lastVersionStr.split("_");
				//prefix = bits[0];
				//lastVer = Integer.parseInt(bits[1]);
			}
			//construct url
			//e.g. "https://customisations-vsba-fujitsu.devops.naviam.cloud/detmas-V9000_72.zip" }
			String archiveUrl = baseUrl + "/" + module + "-" + lastVersionStr + ".zip";
			
			String mode = "add";
			
			String maxinstUrl = getMaxInstUrl(tgtSystem, workspaceFolder);
			
			if (maxinstUrl == null)
			{
				System.out.println("Invalid target system " + tgtSystem);
	        	return false;
			}
			
			if (archiveExists(maxinstUrl, apiKey, archiveName, archiveUrl))
			{
				System.out.println("addArchive archive exists so must do an update");
				//deleteArchive(maxinstUrl, apiKey, archiveName, archiveUrl);
				mode = "update";
			}
	
	    	String requestBody = "";
	    	if (archiveUrl.contains("naviam"))
			{
				requestBody = "{\"name\": \"" + archiveName + "\",  \"secretname\": \"mas-manage-cl2--cac--sn\", " 
	        		               +  "\"mode\": \"" + mode + "\", \"url\": \"" 
	        		              + archiveUrl +  "\" }";
			}
			else
			{
				requestBody = "{\"name\": \"" + archiveName + "\",  " 
 		               +  "\"mode\": \"" + mode + "\", \"url\": \"" 
 		              + archiveUrl +  "\" }";
			}
	        
	        System.out.println("addArchive body is " + requestBody);
	        
	        String tgtUrl = "https://" + maxinstUrl + "/toolsapi/toolservice/updatecustomizationarchive";
	        
	        System.out.println("addArchive tgtUrl is " + tgtUrl);
	
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create(tgtUrl)) 
	                .header("Content-Type", "application/json") 
	                .header("apikey", apiKey)
	                .POST(HttpRequest.BodyPublishers.ofString(requestBody)) 
	                .build();
	
	        HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
	
	        System.out.println("Status Code: " + response.statusCode());
	        System.out.println("Response Body: " + response.body());
	        if (response.statusCode() != 200)
	        {
	        	System.out.println("Error calling API " + response.body());
	        	res = false;
	        }
		}
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        	res = false;
        }
		return res;
	}
	
	/**
     * Call the power api to send a Teams Message  
     */
	private static boolean sendEmail(String message, String teamsKey, String debugFlag)
	{
		System.out.println("sendEmail entering");
		boolean res = true;
		
        try {
	    	 String requestBody = "{\"type\": \"message\","   
		               +  " \"attachments\": [ { \"contentType\": \"application/vnd.microsoft.card.adaptive\"," 
	    			   + " \"content\": { "
		               + "\"type\": \"AdaptiveCard\","
		               + "\"version\": \"1.5\","
		               + "\"body\": [ {"
		               + "\"type\": \"TextBlock\","
		               + "\"text\": \"Jenkins Alert: Build Failed\","
		               + "\"size\": \"ExtraLarge\","
		               + "\"weight\": \"Bolder\","
		               + "\"color\": \"Attention\" }, {"
		               + "\"type\": \"TextBlock\","
		               + "\"text\": \""
		               + message 
		               + "\","
		               + "\"wrap\": \"true\" } ] } } ] }";
		               
	        
	        System.out.println("sendEmail body is " + requestBody);
	        
	        //String tgtUrl = "https://defaulta19f121d81e14858a9d8736e267fd4.c7.environment.api.powerplatform.com:443/powerautomate/automations/direct/workflows/779e6423318542bf92af9f6930a49726/triggers/manual/paths/invoke?api-version=1&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=otpG_hg1UtUPpPbwMpZuaG9FCFUrZqbku7Sfc5tcSnA";
	        String tgtUrl = "https://defaulta19f121d81e14858a9d8736e267fd4.c7.environment.api.powerplatform.com:443/powerautomate/automations/direct/workflows/779e6423318542bf92af9f6930a49726/triggers/manual/paths/invoke?api-version=1&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=";
	        tgtUrl = tgtUrl + teamsKey;
	        
	        System.out.println("sendEmail tgtUrl is " + tgtUrl);
	        
	        if (debugFlag.equalsIgnoreCase("true"))
	        {
	        	return res;
	        }
	        
	        HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create(tgtUrl)) 
	                .header("Content-Type", "application/json") 
	                .POST(HttpRequest.BodyPublishers.ofString(requestBody)) 
	                .build();
	
	        HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
	
	        System.out.println("Status Code: " + response.statusCode());
	        System.out.println("Response Body: " + response.body());
	        if (response.statusCode() != 202)
	        {
	        	System.out.println("Error calling API " + response.body());
	        	res = false;
	        }
		}
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        	res = false;
        }
		return res;
	}
	
	 /**
     * Test the addArchive function  
     * If the archive already exists it must be an Update call 
     */
	private static boolean addArchiveTest(String tgtSystem, String apiKey, String archiveName, String archiveUrl, String workspaceFolder)
	{
		System.out.println("addArchiveTest entering");
		boolean res = true;
		
		try {
			String mode = "add";
			
			String maxinstUrl = getMaxInstUrl(tgtSystem, workspaceFolder);
			
			if (maxinstUrl == null)
			{
				System.out.println("Invalid target system " + tgtSystem);
	        	return false;
			}
			
			if (archiveExists(maxinstUrl, apiKey, archiveName, archiveUrl))
			{
				System.out.println("addArchive archive exists so must do an update");
				//deleteArchive(maxinstUrl, apiKey, archiveName, archiveUrl);
				mode = "update";
			}
			
			String requestBody = "";
			if (archiveUrl.contains("naviam"))
			{
				requestBody = "{\"name\": \"" + archiveName + "\",  \"secretname\": \"mas-manage-cl2--cac--sn\", " 
	        		               +  "\"mode\": \"" + mode + "\", \"url\": \"" 
	        		              + archiveUrl +  "\" }";
			}
			else
			{
				requestBody = "{\"name\": \"" + archiveName + "\",  " 
 		               +  "\"mode\": \"" + mode + "\", \"url\": \"" 
 		              + archiveUrl +  "\" }";
			}
	         
	        
	        System.out.println("addArchiveTest body is " + requestBody);
	        
	        String tgtUrl = "https://" + maxinstUrl + "/toolsapi/toolservice/updatecustomizationarchive";
	        
	        System.out.println("addArchiveTest maxinst url is " + tgtUrl);
	        
	        String manageUrl = getManageUrl(tgtSystem, workspaceFolder);
	        tgtUrl = "https://" + manageUrl + "/maximo/api/systeminfo?lean=1";
	        System.out.println("addArchiveTest manageurl is " + tgtUrl);
	        
        
		}
		
		catch (Exception ex)
        {
        	System.out.println("addArchiveTest Exception: " + ex.getMessage());
        	res = false;
        }
		return res;
	}

	private static String getMaxInstUrl(String tgtSystem, String workspaceFolder)
	{
		System.out.println("getMaxInstUrl entering");
		String maxinstUrl = null;
		String filePath = workspaceFolder + "//buildCommon//urls.txt";
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			
			String line = "";
			while((line = br.readLine()) != null)  
			{  
				if (line.startsWith(tgtSystem))
				{
					String[] bits = line.trim().split(",");
					if (bits.length == 3)
					{
						maxinstUrl = bits[1];
					}
					break;
				}
			}
		}
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        }
		return maxinstUrl;
	}
	
	private static String getMaxInstUrlV1(String tgtSystem)
	{
		System.out.println("getMaxInstUrl entering");
		
		String maxinstUrl = null;
		
		if (tgtSystem.equalsIgnoreCase("BAUDEV"))
		{
			maxinstUrl = "maxinst.manage.dev.vsba-aims.naviam.app";
		}
		else if (tgtSystem.equalsIgnoreCase("BAUST"))
		{
			maxinstUrl = "maxinst.manage.st.vsba-aims.naviam.app";
		}
		else if (tgtSystem.equalsIgnoreCase("BAUSIT"))
		{
			maxinstUrl = "maxinst.manage.sit.vsba-aims.naviam.app";
		}
		else if (tgtSystem.equalsIgnoreCase("BAUUAT"))
		{
			maxinstUrl = "maxinst.manage.uat.vsba-aims.naviam.app";
		}
		else if (tgtSystem.equalsIgnoreCase("CELSTG"))
		{
			maxinstUrl = "maxinst.manage.eam-stg.countiesenergy.co.nz";
		}
		else if (tgtSystem.equalsIgnoreCase("CELDEV"))
		{
			maxinstUrl = "maxinst.manage.eam-dev.countiesenergy.co.nz";
		}
		else if (tgtSystem.equalsIgnoreCase("CELPRD"))
		{
			maxinstUrl = "maxinst.manage.eam-prd.countiesenergy.co.nz";
		}
		else
		{
			System.out.println("getMaxInstUrl Invalid tgt system " + tgtSystem);
		}
		System.out.println("getMaxInstUrl max inst url is " + maxinstUrl);
		return maxinstUrl;
	}
	
	private static String getManageUrlV1(String tgtSystem)
	{
		System.out.println("getManageUrl entering");
		
		String manageUrl = null;
		
		if (tgtSystem.equalsIgnoreCase("BAUDEV"))
		{
			manageUrl = "mas.manage.dev.vsba-aims.naviam.app";
		}
		else if (tgtSystem.equalsIgnoreCase("BAUST"))
		{
			manageUrl = "mas.manage.st.vsba-aims.naviam.app";
		}
		else if (tgtSystem.equalsIgnoreCase("BAUSIT"))
		{
			manageUrl = "mas.manage.sit.vsba-aims.naviam.app";
		}
		else if (tgtSystem.equalsIgnoreCase("BAUUAT"))
		{
			manageUrl = "mas.manage.uat.vsba-aims.naviam.app";
		}
		else if (tgtSystem.equalsIgnoreCase("CELSTG"))
		{
			manageUrl = "masstgws.manage.eam-stg.countiesenergy.co.nz";
		}
		else if (tgtSystem.equalsIgnoreCase("CELDEV"))
		{
			manageUrl = "masstgws.manage.eam-stg.countiesenergy.co.nz";
		}
		else if (tgtSystem.equalsIgnoreCase("CELPRD"))
		{
			manageUrl = "masstgws.manage.eam-stg.countiesenergy.co.nz";
		}
		else
		{
			System.out.println("getManageUrl Invalid tgt system " + tgtSystem);
		}
		return manageUrl;
	}
	
	private static String getManageUrl(String tgtSystem, String workspaceFolder)
	{
		System.out.println("getManageUrl entering");
		String manageUrl = null;
		String filePath = workspaceFolder + "//buildCommon//urls.txt";
		
		try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
			
			String line = "";
			while((line = br.readLine()) != null)  
			{  
				if (line.startsWith(tgtSystem))
				{
					String[] bits = line.trim().split(",");
					if (bits.length == 3)
					{
						manageUrl = bits[2];
					}
					break;
				}
			}
		}
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        }
		
		return manageUrl;
	}
	
	private static JSONObject getArchiveList(String maxinstUrl, String apiKey) throws Exception
	{
		System.out.println("getArchiveList entering maxInstUrl " + maxinstUrl);
		JSONObject jo = null;
		boolean res = true;
				
        String requestBody = "{\"mode\": \"get\" }";
        
        String tgtUrl = "https://" + maxinstUrl + "/toolsapi/toolservice/updatecustomizationarchive";
        
        System.out.println("getArchiveList entering tgtUrl " + tgtUrl);
        try {

        	HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tgtUrl)) 
                .header("Content-Type", "application/json") 
                .header("apikey", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)) 
                .build();

           HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

           System.out.println("getArchiveList Status Code: " + response.statusCode());
           System.out.println("getArchiveList Response Body: " + response.body());
           
           JSONArray ja = new JSONArray(response.body());
           
           //String jsonStr = "{ \"files\": " + response.body() + "}";
           //String escapedString = jsonStr.replaceAll("\"", "\\\\\"");
           
           jo = new JSONObject();
           jo.put("files",ja);

        }
        catch (Exception ex)
        {
        	System.out.println("getArchiveList Exception: " + ex.getMessage());
        	res = false;
        	throw ex;
        }
		return jo;
		
	}

	private static boolean archiveExists(String maxinstUrl, String apiKey, String archiveName, String archiveUrl) throws Exception
	{
		System.out.println("archiveExists entering");
		
		boolean exists = false;
		try {
			
			JSONObject jo = getArchiveList(maxinstUrl, apiKey);
			if (jo != null) {
				JSONArray ja = jo.getJSONArray("files");
				
				for (Object obj : ja) {
		            JSONObject jsonObject = (JSONObject) obj;
		            String name = jsonObject.getString("customizationArchiveName");
		            System.out.println("archiveExists name is " + name);
		            if (name.equalsIgnoreCase(archiveName)) {
		            	exists = true;
		            }
		        }
			}
		}
		catch (Exception ex)
        {
        	System.out.println("archiveExists Exception: " + ex.getMessage());
        	throw ex;
        }
		return exists;
	}
	
	private static boolean getBuildStatus(String maxinstUrl,String apiKey)
	{
		System.out.println("getBuildStatus entering");
		boolean res = true;
		
        String requestBody = "{\"mode\": \"get\" }";
        
        String tgtUrl = "https://" + maxinstUrl + "/toolsapi/toolservice/getbuildstatus";
        
        try {

        	HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tgtUrl)) 
                .header("Content-Type", "application/json") 
                .header("apikey", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)) 
                .build();

           HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

           System.out.println("Status Code: " + response.statusCode());
           System.out.println("Response Body: " + response.body());
           
           if (response.body().contains("tagged"))
           {
        	   System.out.println("!!!!Build was generated and tagged");
           }
           
           if (response.statusCode() == 503)
           {
        	   System.out.println(" !!!!! Maxinst has gone down");
           }
           
        }
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        	res = false;
        }
      
		return res;
	}

	

	private static void uploadPDF()
    {
		
		String url = "https://stbpdanzclient001ipm.file.core.windows.net/ipmdoclinks/workorders/15304461/_Test.pdf";
		String url2 = "https://stbpdanzclient001ipm.file.core.windows.net/ipmdoclinks/workorders/15304461/_Test.pdf?comp=range&timeout=60";
		
		File file = new File("C:\\Users\\adlingtonp\\Workspace\\call-toolsapi\\inputs\\_Test.pdf");
		
		String bearerHdr = "Bearer ";
		String token = "";
		String bearer = bearerHdr + token;
		
		
		 try {

			    byte[] buf = Files.readAllBytes(file.toPath());
			    
			    int len = buf.length;
			    
			 	// Simple binary upload (assuming server accepts a raw PDF stream)
		        HttpRequest request = HttpRequest.newBuilder()
		                .uri(URI.create(url))
		                .header("Authorization", bearer) 
		                .header("x-ms-version", "2022-11-02")
		                .header("x-ms-type" , "file")
		                .header("x-ms-content-length", String.valueOf(len))
		                .header("x-ms-file-request-intent", "backup")
		                .POST(HttpRequest.BodyPublishers.ofByteArray(Files.readAllBytes(file.toPath())))
		                .build();
		        

	           HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

	           System.out.println("Status Code: " + response.statusCode());
	           System.out.println("Response Body: " + response.body());
	           
	           // Simple binary upload (assuming server accepts a raw PDF stream)
	           String range = "bytes=0-" + String.valueOf(len-1);
	           
	           
		       HttpRequest request2 = HttpRequest.newBuilder()
		                .uri(URI.create(url2))
		                .header("Authorization", bearer) 
		                .header("x-ms-version", "2022-11-02")
		                .header("x-ms-write" , "update")
		                .header("x-ms-range", range)
		                .header("x-ms-file-request-intent", "backup")
		                .header("Content-Length", String.valueOf(len))
		                .POST(HttpRequest.BodyPublishers.ofByteArray(Files.readAllBytes(file.toPath())))
		                .build();
		        
		        response = GLOBAL_CLIENT.send(request2, HttpResponse.BodyHandlers.ofString());
		        
	        }
		 catch (Exception ex)
	     {
	      	System.out.println("Exception: " + ex.getMessage());
	     }
    		
		
    }
	
	
	private static void callToolsAPI()
    {
        String requestBody = "{\"name\": \"mas-det-sit-dbdetmas\", \"mode\": \"add\", \"url\": \"https://mascustomisations.s3.ap-southeast-2.amazonaws.com/FujTestBuilds/mas-det-sit-dbdetmas\" }";
        
        String myUrl = "https://maxinst.manage.sit.vsba-aims.naviam.app/toolsapi/toolservice/updatecustomizationarchive";
        
        try {

        	HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(myUrl)) 
                .header("Content-Type", "application/json") 
                .header("apikey", "keyhere")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody)) 
                .build();

           HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

           System.out.println("Status Code: " + response.statusCode());
           System.out.println("Response Body: " + response.body());
        }
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        }
        
    }
	
	private static boolean checkMaximoStatus(String apiKey,String manageUrl, int waitTime )
	{
		
		long startTime = System.currentTimeMillis();
		//long endTime = startTime + TimeUnit.HOURS.toMillis(1); 
		long endTime = startTime + TimeUnit.MINUTES.toMillis(waitTime);
		
		System.out.println("Checking Maximo Status for " + waitTime + " minutes");
		
		boolean res = true;
		boolean up = false;
		try {
			up = getSystemInfo(apiKey, manageUrl);
			while (!up)
			{
				System.out.println("Maximo not up so sleep");
				TimeUnit.MINUTES.sleep(1);
				up = getSystemInfo(apiKey, manageUrl);
				if (!up && (System.currentTimeMillis() >= endTime))
				{
					System.out.println("Maximo not up after wait time");
					res = false;
					break;
				}
			}
		}
		catch (Exception ex)
		{
			System.out.println("Exception: " + ex.getMessage());
			res = false;
		}
		return res;
		
	}
	
	private static boolean checkMaximoStatusLoop(String apiKey,String manageUrl, int waitTime )
	{
		
		long startTime = System.currentTimeMillis();
		//long endTime = startTime + TimeUnit.HOURS.toMillis(1); 
		long endTime = startTime + TimeUnit.MINUTES.toMillis(waitTime);
		
		System.out.println("Checking Maximo Status for " + waitTime + " minutes");
		
		boolean res = true;
		boolean up = false;
		try {
			while (true)
			{
				System.out.println("getting sys info now");
				getSystemInfo(apiKey, manageUrl);
				System.out.println("Sleeping now");
				TimeUnit.MINUTES.sleep(1);
				if (System.currentTimeMillis() >= endTime)
				{
					break;
				}
			}
		}
		catch (Exception ex)
		{
			System.out.println("Exception: " + ex.getMessage());
			res = false;
		}
		return res;
		
	}
	
	
	private static boolean checkDeploymentStatus(String apiKey, String tgtSystem, String versionStr, int waitTime, String workspaceFolder )
	{
		boolean deployed = false;
		long startTime = System.currentTimeMillis();
		//long endTime = startTime + TimeUnit.HOURS.toMillis(1); 
		long endTime = startTime + TimeUnit.MINUTES.toMillis(waitTime);
		
		Instant instant = Instant.ofEpochMilli(endTime);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

        System.out.println("Checking Deployment Status for " + waitTime + " minutes");
		System.out.println("Checking Deployment Status end time " + dateTime.format(formatter));
		
		
		String maxinstUrl = getMaxInstUrl(tgtSystem, workspaceFolder);
		
		if (maxinstUrl == null)
		{
			System.out.println("Invalid target system " + tgtSystem);
        	return false;
		}
		
		String manageUrl = getManageUrl(tgtSystem, workspaceFolder);
		
		if (manageUrl == null)
		{
			System.out.println("Invalid target system " + tgtSystem);
        	return false;
		}
		
		try {
			System.out.println("Start checking status now");
			
			while (true)
			{
				//Wait for MAXINST to stop
				waitForMaxInst(maxinstUrl,apiKey, endTime, "STOP");
				if (System.currentTimeMillis() >= endTime)
				{
					System.out.println("Wait time has expired");
					break;
				}
				
				//Wait for MAXINST to restart
				waitForMaxInst(maxinstUrl, apiKey, endTime, "START");
				if (System.currentTimeMillis() >= endTime)
				{
					System.out.println("Wait time has expired");
					break;
				}
								
				//Check if Manage is up
				boolean manageUp = isManageUp(manageUrl,apiKey);
				
				if (manageUp) {
					waitForManage(manageUrl,apiKey, endTime, "STOP");
				}
				if (System.currentTimeMillis() >= endTime)
				{
					System.out.println("Wait time has expired");
					break;
				}
				
				waitForManage(manageUrl,apiKey, endTime, "START");
				if (System.currentTimeMillis() >= endTime)
				{
					System.out.println("Wait time has expired");
					break;
				}
				//Check that the product versions are correct
				String version = getVersion(manageUrl, apiKey);
				String [] items = versionStr.split("_");
				String expectedVersionStr = items[1];
				if (version.equalsIgnoreCase(expectedVersionStr))
				{
					deployed = true;
					System.out.println("Version " + expectedVersionStr + " deployed successfully");
					break;
				}
				else
				{
					System.out.println("Version " + version + " not expected");
					break;
				}
			}
		}
		catch (Exception ex)
		{
			System.out.println("Exception: " + ex.getMessage());
			///res = false;
		}
		return deployed;
		
	}
	
	
	private static String  getVersion(String manageUrl, String apiKey)
	{
		String version = null;
		if (manageUrl.contains("countiesenergy"))
		{
			version = getCELMASVersion(manageUrl,apiKey); 
		}
		else
		{
		    //naviam
			version = getDETMASVersion(manageUrl,apiKey);
		}
			
		return version;
	}
	
	private static String  getDETMASVersion(String manageUrl, String apiKey)
	{
		String version = null;

		System.out.println("getDETMASVersion entering");
		
		//HttpClient client = HttpClient.newBuilder().sslContext(insecureContext()).build();
                
    	String tgtUrl = "https://" + manageUrl + "/maximo/api/systeminfo?lean=1";
    	 
        try (HttpClient client = HttpClient.newBuilder().sslContext(insecureContext()).build()){

        	HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tgtUrl)) 
                .header("apikey", apiKey)
                .GET()
                .build();

           HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

           System.out.println("Status Code: " + response.statusCode());
           System.out.println("Response Body: " + response.body());
           
           if (response.statusCode() == 200) {
        	   
        	   System.out.println("Got System Info");
        	   JSONObject jo = new JSONObject(response.body());
        	   JSONObject appVersion = jo.getJSONObject("appVersion");
        	   JSONArray ja = appVersion.getJSONArray("member");
        	   for (Object obj : ja) {
		            JSONObject jsonObject = (JSONObject) obj;
		            String versionKey = jsonObject.getString("versionKey");
		            System.out.println("versionKey is " + versionKey);
		            //DET MAS CHANGES 9.0.0.0 Build Release1 DB Build V9000-59
		            if (versionKey.startsWith("DET MAS CHANGES")) {
		            	System.out.println("Found DET Changes versionKey");
		            	int p1 = versionKey.indexOf("V9000");
		            	version = versionKey.substring(p1+6);
		            	System.out.println("Version is " + version);
		            	break;
		            }
		        }
           }
        }
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        }
        System.out.println("getDETMASVersion leaving");
        return version;
	}

	private static String  getCELMASVersion(String manageUrl, String apiKey)
	{
		String version = null;

		System.out.println("getCELMASVersion entering");
		
		//HttpClient client = HttpClient.newBuilder().sslContext(insecureContext()).build();
                
    	String tgtUrl = "https://" + manageUrl + "/maximo/api/systeminfo?lean=1";
    	 
    	try (HttpClient client = HttpClient.newBuilder().sslContext(insecureContext()).build()){

        	HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tgtUrl)) 
                .header("apikey", apiKey)
                .GET()
                .build();

           HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

           System.out.println("Status Code: " + response.statusCode());
           System.out.println("Response Body: " + response.body());
           
           if (response.statusCode() == 200) {
        	   
        	   System.out.println("Got System Info");
        	   JSONObject jo = new JSONObject(response.body());
        	   JSONObject appVersion = jo.getJSONObject("appVersion");
        	   JSONArray ja = appVersion.getJSONArray("member");
        	   for (Object obj : ja) {
		            JSONObject jsonObject = (JSONObject) obj;
		            String versionKey = jsonObject.getString("versionKey");
		            System.out.println("versionKey is " + versionKey);
		            
		            //Counties Work Management System 2.0.0.0 Build WMS_200_BUILD_2026_04_08_B44 DB Build V200-131
		            if (versionKey.startsWith("Counties Work")) {
		            	System.out.println("Found CEL Changes versionKey");
		            	int p1 = versionKey.indexOf("V200");
		            	version = versionKey.substring(p1+5);
		            	System.out.println("Version is " + version);
		            	break;
		            }
		        }
           }
        }
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        }
        System.out.println("getCELMASVersion leaving");
        return version;
	}


	private static boolean waitForMaxInst(String maxinstUrl, String apiKey, long endTime, String event)
	{
		System.out.println("waitForMaxInst entering for " + event);
		
		boolean carryOn = false;

        String requestBody = "{\"mode\": \"get\" }";
        
        String tgtUrl = "https://" + maxinstUrl + "/toolsapi/toolservice/getbuildstatus";
        
		while (true)
		{
			
	        try {

	        	HttpRequest request = HttpRequest.newBuilder()
	                .uri(URI.create(tgtUrl)) 
	                .header("Content-Type", "application/json") 
	                .header("apikey", apiKey)
	                .POST(HttpRequest.BodyPublishers.ofString(requestBody)) 
	                .build();

	           HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

	           //System.out.println("Status Code: " + response.statusCode());
	          // System.out.println("Response Body: " + response.body());
	           
	           	           
	           if (event.equalsIgnoreCase("STOP") && response.statusCode() == 503)
	           {
	        	   System.out.println(" !!!!! Waiting for STOP and Maxinst has gone down");
	        	   carryOn = true;
	        	   break;
	           }
	           
	           if (event.equalsIgnoreCase("START") && response.statusCode() == 200)
	           {
	        	   System.out.println(" !!!!! Waiting for START and Maxinst has come up");
	        	   carryOn = true;
	        	   break;
	           }
	           
	           if (response.body().contains("tagged"))
	           {
	        	   System.out.println("##### Build was generated and tagged ####");
	           }
	           
	           if (System.currentTimeMillis() >= endTime)
	           {
	        	   carryOn = false;
	        	   break;
	           }
	           
	           System.out.println("waitForMaxInst sleeping now");
	           TimeUnit.MINUTES.sleep(1);
	           
	           
	        }
	        
	        catch (Exception ex)
	        {
	        	System.out.println("Exception: " + ex.getMessage());
	        	
	        	
	        }
	        
		}
		return carryOn;
	}
	
	private static boolean isManageUp(String manageUrl, String apiKey)
	{
		System.out.println("isManageUp entering");
		boolean isUp = false;
		//HttpClient client = HttpClient.newBuilder().sslContext(insecureContext()).build();
		
		String tgtUrl = "https://" + manageUrl + "/maximo/api/systeminfo?lean=1";
		
		try {

	       	HttpRequest request = HttpRequest.newBuilder()
	                    .uri(URI.create(tgtUrl)) 
	                    .header("apikey", apiKey)
	                    .GET()
	                    .build();

	        HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

	        System.out.println("Status Code: " + response.statusCode());
	        System.out.println("Response Body: " + response.body());
	           
	        if (response.statusCode() == 200)
	        {
	        	System.out.println("Manage is up");
	        	isUp = true;
	        }
		}   
	    catch (Exception ex)
	    {
	    	System.out.println("Exception: " + ex.getMessage());
	    }
		return isUp;
		
	}
	
	private static boolean waitForManage(String manageUrl, String apiKey, long endTime, String event)
	{
		System.out.println("waitForManage entering for " + event);
		
		boolean carryOn = false;
		
		//HttpClient client = HttpClient.newBuilder().sslContext(insecureContext()).build();
		
		String tgtUrl = "https://" + manageUrl + "/maximo/api/systeminfo?lean=1";
		
		while (true)
		{
			
	        try {

	        	HttpRequest request = HttpRequest.newBuilder()
	                    .uri(URI.create(tgtUrl)) 
	                    .header("apikey", apiKey)
	                    .GET()
	                    .build();

	           HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

	           //System.out.println("Status Code: " + response.statusCode());
	          // System.out.println("Response Body: " + response.body());
	           
	           	           
	           if (event.equalsIgnoreCase("STOP") && response.statusCode() == 503)
	           {
	        	   System.out.println(" !!!!! Waiting for STOP and Manage has gone down");
	        	   carryOn = true;
	        	   break;
	           }
	           
	           if (event.equalsIgnoreCase("START") && response.statusCode() == 200)
	           {
	        	   System.out.println(" !!!!! Waiting for START and Manage has come up");
	        	   carryOn = true;
	        	   break;
	           }
	           
	           if (System.currentTimeMillis() >= endTime)
	           {
	        	   carryOn = false;
	        	   break;
	           }
	           
	           System.out.println("waitForManage sleeping now");
	           TimeUnit.MINUTES.sleep(1);
	           
	           
	        }
	        
	        catch (Exception ex)
	        {
	        	System.out.println("Exception: " + ex.getMessage());
	        	
	        	
	        }
	        
		}
		return carryOn;
	}
	
	
	
	private static boolean checkBuildStatus(String apiKey,String maxinstUrl, int waitTime )
	{
		
		long startTime = System.currentTimeMillis();
		//long endTime = startTime + TimeUnit.HOURS.toMillis(1); 
		long endTime = startTime + TimeUnit.MINUTES.toMillis(waitTime);
		
		System.out.println("Checking Maximo Build Status for " + waitTime + " minutes");
		
		boolean res = true;
		boolean up = false;
		try {
			while (true)
			{
				System.out.println("checking status now");
				getBuildStatus(maxinstUrl, apiKey);
				System.out.println("Sleeping now");
				TimeUnit.MINUTES.sleep(1);
				if (System.currentTimeMillis() >= endTime)
				{
					break;
				}
			}
		}
		catch (Exception ex)
		{
			System.out.println("Exception: " + ex.getMessage());
			res = false;
		}
		return res;
		
	}
	
	private static boolean checkMaximoStatusV1(String apiKey,String manageUrl)
	{
		System.out.println("Checking Maximo Status");
		
		boolean res = true;
		try {
			boolean up = getSystemInfo(apiKey, manageUrl);
			while (!up)
			{
				System.out.println("Maximo not up so sleep");
				TimeUnit.MINUTES.sleep(1);
				up = getSystemInfo(apiKey, manageUrl);
			}
		}
		catch (Exception ex)
		{
			System.out.println("Exception: " + ex.getMessage());
			res = false;
		}
		return res;
	}
    
	private static boolean getSystemInfo(String apiKey, String manageUrl)
    {
		System.out.println("Calling System Info end point");
		boolean res = false;
                
        //String myUrl = "https://mas.manage.sit.vsba-aims.naviam.app/maximo/api/systeminfo?lean=1";
        //String myUrl = "https://mas9wdm01.manage.mas9dem01.apps.mas9dem01.fujitsu-eam.com/maximo/api/systeminfo?lean=1";
        
    	 String tgtUrl = "https://" + manageUrl + "/maximo/api/systeminfo?lean=1";
    	 
        try {

        	HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tgtUrl)) 
                .header("apikey", apiKey)
                .GET()
                .build();

           HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

           System.out.println("Status Code: " + response.statusCode());
           System.out.println("Response Body: " + response.body());
           
           if (response.statusCode() == 200) {
        	   res = true;
        	   System.out.println("Got System Info");
           }
        
        }
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        }
        return res;
        
    }
	
	
	
	private static void callJenkins()
    {
        String myUrl = "https://eamjenkinsmas.australiasoutheast.cloudapp.azure.com:8443/jenkins/job/AutoTestsAll/buildWithParameters";
        
        try {

        	
        	HttpRequest request = HttpRequest.newBuilder()
        		    .uri(URI.create(myUrl))
        		    .header("content-type", "application/x-www-form-urlencoded")
        		    .header("authorization", "Basic tokenhere")
        		    .method("POST", HttpRequest.BodyPublishers.ofString("TargetAgent=automation-vm&MASVersion=90&MASUrl=mas.manage.sit.vsba-aims.naviam.app&BINFile=01_ST_TC01_Assets.bin&RunTests=true"))
        		    .build();
        	
           HttpResponse<String> response = GLOBAL_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

           System.out.println("Status Code: " + response.statusCode());
           System.out.println("Response Body: " + response.body());
        }
        catch (Exception ex)
        {
        	System.out.println("Exception: " + ex.getMessage());
        }
        
    }
	
	private static SSLContext insecureContext() 
	{
		SSLContext sc = null;
		
	    TrustManager[] noopTrustManager = new TrustManager[]{
	        new X509TrustManager() {
	            public void checkClientTrusted(X509Certificate[] xcs, String string) {}
	            public void checkServerTrusted(X509Certificate[] xcs, String string) {}
	            public X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	        }
	    };
	    
	    try {
	        sc = SSLContext.getInstance("ssl");
	        sc.init(null, noopTrustManager, null);
	        
	    } catch (Exception ex) {}
	    
	    return sc;
	}    
	
	
	private static void createZipFile ()throws IOException
    {
		String sourceFile = "C:/temp/build/SMP";
        FileOutputStream fos = new FileOutputStream("c:/temp/build/SMP.zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        File fileToZip = new File(sourceFile);
        zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
    }
	
	private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
	    if (fileToZip.isHidden()) {
	        return;
	    }
	    if (fileToZip.isDirectory()) {
	        if (fileName.endsWith("/")) {
	            zipOut.putNextEntry(new ZipEntry(fileName));
	            zipOut.closeEntry();
	        } else {
	            zipOut.putNextEntry(new ZipEntry(fileName + "/"));
	            zipOut.closeEntry();
	        }
	        File[] children = fileToZip.listFiles();
	        for (File childFile : children) {
	            zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
	        }
	        return;
	    }
	    FileInputStream fis = new FileInputStream(fileToZip);
	    ZipEntry zipEntry = new ZipEntry(fileName);
	    zipOut.putNextEntry(zipEntry);
	    byte[] bytes = new byte[1024];
	    int length;
	    while ((length = fis.read(bytes)) >= 0) {
	        zipOut.write(bytes, 0, length);
	    }
	    fis.close();
	}
        
}