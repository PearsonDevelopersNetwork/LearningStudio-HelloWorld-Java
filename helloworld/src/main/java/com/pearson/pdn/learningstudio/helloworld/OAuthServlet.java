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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public abstract class OAuthServlet extends HttpServlet  {
	protected final static String LS_API_URL = "https://api.learningstudio.com";
	protected final static ResourceBundle resources = ResourceBundle.getBundle(OAuthServlet.class.getName());
	
	// See OAuth1SignatureServlet and OAuth2AssertionServlet
	public abstract Map<String,String> getOAuthHeaders(String url,  String httpMethod, String httpBody) throws IOException;

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		doMethod(req,res);
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
		doMethod(req,res);
	}
	
	public void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
		doMethod(req,res);
	}
	
	public void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
		doMethod(req,res);
	}
	
	private void doMethod(HttpServletRequest req, HttpServletResponse res) throws IOException {
		String path = req.getPathInfo();
		String queryString = req.getQueryString();
		
		if(queryString!=null) {
			path = path + "?" + queryString;
		}
		
		PrintWriter out = res.getWriter();
		try {
			String url = LS_API_URL + path; 
			String body = LearningStudioUtility.getContent(req.getInputStream());
			Map<String,String> oauthHeaders = getOAuthHeaders(url, req.getMethod(),body);
			
			LearningStudioUtility.Response response = null;
			if(req.getMethod().equalsIgnoreCase("GET")) {
				response = LearningStudioUtility.doGet(url, oauthHeaders);
			}
			else if(req.getMethod().equalsIgnoreCase("POST")) {
				response = LearningStudioUtility.doPost(url, oauthHeaders,body);
			}
			else if(req.getMethod().equalsIgnoreCase("PUT")) {
				response = LearningStudioUtility.doPut(url, oauthHeaders,body);				
			}
			else if(req.getMethod().equalsIgnoreCase("DELETE")) {
				response = LearningStudioUtility.doDelete(url, oauthHeaders);
			}
			
			res.setContentType(response.contentType);
			
			out.println(response.content);		
		} 
		catch (Exception e) {
			out.println("Internal Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
