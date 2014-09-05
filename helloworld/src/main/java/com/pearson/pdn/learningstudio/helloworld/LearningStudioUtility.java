/*
* LearningStudio HelloWorld Application & API Explorer 
* 
* Need Help or Have Questions? 
* Please use the PDN Developer Community at https://community.pdn.pearson.com
*
* @category   LearningStudio HelloWorld
* @author     Wes Williams <wes.williams@pearson.com>
* @author     Pearson Developer Services Team <apisupport@pearson.com>
* @copyright  2014 Pearson Education Inc.
* @license    http://www.apache.org/licenses/LICENSE-2.0  Apache 2.0
* @version    1.0
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.pearson.pdn.learningstudio.helloworld;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LearningStudioUtility {
	private final static Logger logger = Logger.getLogger(LearningStudioUtility.class);
	
	public static class Response {
		public int statusCode;
		public String contentType;
		public String content;
	}
	
	public static Response doGet(String uri, Map<String,String> oauthHeaders) throws IOException {
		return doMethod("GET",uri,oauthHeaders,null);
	}
	
	public static Response doPost(String uri, Map<String,String> oauthHeaders, String body) throws IOException {
		return doMethod("POST",uri,oauthHeaders,body);
	}
	
	public static Response doPut(String uri, Map<String,String> oauthHeaders, String body) throws IOException {
		return doMethod("PUT",uri,oauthHeaders,body);
	}
	
	public static Response doDelete(String uri, Map<String,String> oauthHeaders) throws IOException {
		return doMethod("DELETE",uri,oauthHeaders,null);
	}
	
	private static Response doMethod(String method, String uri, Map<String,String> oauthHeaders, String body) throws IOException {
		boolean isXml = uri.toLowerCase().endsWith(".xml");
		URL url = new URL(uri);
		HttpsURLConnection request = (HttpsURLConnection) url.openConnection();
		try {
			request.setRequestMethod(method);
			request.setRequestProperty("User-Agent","LS-HelloWorld-Java-V1");
			
			Set<String> oauthHeaderKeys = oauthHeaders.keySet();
			for(String oauthHeaderKey : oauthHeaderKeys) {
				request.addRequestProperty(oauthHeaderKey,oauthHeaders.get(oauthHeaderKey));
			}
			
			Map<String, List<String>> requestProps = null;
			if(method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
				request.setRequestProperty("Content-Type", isXml ? "application/xml" : "application/json");
				request.setRequestProperty("Content-Length", String.valueOf(body.getBytes("UTF-8").length));
				request.setDoOutput(true);
				
				requestProps = request.getRequestProperties();
		
				DataOutputStream out = new DataOutputStream(request.getOutputStream());
				try {
					out.writeBytes(body);
					out.flush();
				}
				finally {
					out.close();
				}
			}
			else {
				requestProps = request.getRequestProperties();
			}
			
			
			if(logger.isDebugEnabled()) {
				logger.debug("--------REQUEST URL--------");
				logger.debug(method + " " + uri);
				logger.debug("--------REQUEST HEADERS--------");	
				for(String propKey : requestProps.keySet()) {
					logger.debug(propKey + " = " + requestProps.get(propKey));
				}
				if(method.equalsIgnoreCase("POST") || method.equalsIgnoreCase("PUT")) {
					logger.debug("--------REQUEST CONTENT--------");
					logger.debug(body);
				}
			}
			
			int responseCode = request.getResponseCode();
			Response response = new Response();
			response.statusCode = responseCode;
			
			if(logger.isDebugEnabled()) {
				logger.debug("--------RESPONSE HEADERS--------");
				Map<String, List<String>> responseProps = request.getHeaderFields();
				for(String propKey : responseProps.keySet()) {
					logger.debug(propKey + " = " + responseProps.get(propKey));
				}
			}
			
			if(responseCode != 200 && responseCode != 201 ) {
				if(logger.isDebugEnabled()) {
					InputStream inputStream = null;
					if(responseCode < 400) {
						inputStream = request.getInputStream();
					}
					else {
						inputStream = request.getErrorStream();
					}
					if(inputStream != null) {
						String content = getContent(inputStream);
						logger.debug("--------RESPONSE CONTENT--------");
						logger.debug(content);
					}
				}
			
				response.contentType = "text/plain";
				response.content = String.valueOf(responseCode);
				return response;
			}
			
			String contentType = request.getContentType();
			String content = getContent(request.getInputStream());
			
			if(contentType==null && content.length()==0) {
				contentType = "text/plain";
				content = String.valueOf(responseCode);
			}
			else if(contentType != null && contentType.matches("application/json.*")) {
				content = formatJson(content);
			}

			if(logger.isDebugEnabled()) {
				logger.debug("--------RESPONSE CONTENT--------");
				logger.debug(content);
			}
			
			response.contentType = contentType;
			response.content = content;
			
			return response;
		}
		finally {
			request.disconnect();
		}
	}
	
	public static String getContent(InputStream inputStream) throws IOException  {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));
		try {
			StringBuilder responseBody = new StringBuilder();
			String line = null;
			while((line=bufferedReader.readLine())!=null) {
				responseBody.append(line);
			}
			return responseBody.toString();
		}
		finally {
			bufferedReader.close();
		}
	}
	
	private static String formatJson(String content) throws IOException {
		JsonParser parser = new JsonParser();
		JsonObject json = parser.parse(content).getAsJsonObject();
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		
		return json==null ? "" : gson.toJson(json);
	}

}
