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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;

public class OAuth1SignatureServlet extends OAuthServlet {
	private final static String SIGNATURE_METHOD = "CMAC-AES";

	@Override
	public Map<String, String> getOAuthHeaders(String resourceUrl, String httpMethod, String body) throws IOException {
		
		final String applicationId = resources.getString("applicationId");
		final String consumerKey = resources.getString("consumerKey");
		final String consumerSecret = resources.getString("consumerSecret");
		
		URL url = new URL(resourceUrl);
		
		// Set the Nonce and Timestamp parameters
		String nonce = getNonce();
		String timestamp = getTimestamp();

		byte[] requestBody = null;
		// Set the request body if making a POST or PUT request
		if ("POST".equalsIgnoreCase(httpMethod) || "PUT".equalsIgnoreCase(httpMethod)) {
			requestBody = body.getBytes("UTF-8");
		}

		// Create the OAuth parameter name/value pair
		Map<String, String> oauthParams = new LinkedHashMap<String, String>();
		oauthParams.put("oauth_consumer_key", consumerKey);
		oauthParams.put("application_id", applicationId);
		oauthParams.put("oauth_signature_method", SIGNATURE_METHOD);
		oauthParams.put("oauth_timestamp", timestamp);
		oauthParams.put("oauth_nonce", nonce);

		// Get the OAuth 1.0 Signature
		String signature = generateSignature(httpMethod, url, oauthParams, requestBody, consumerSecret);
		// Add the oauth_signature parameter to the set of OAuth Parameters
		oauthParams.put("oauth_signature", signature);

		// Generate a string of comma delimited: keyName="URL-encoded(value)" pairs

		StringBuilder sb = new StringBuilder();
		String delimiter = "";
		for (String keyName : oauthParams.keySet()) {
			sb.append(delimiter);
			String value = oauthParams.get((String) keyName);
			sb.append(keyName).append("=\"").append(URLEncoder.encode(value, "UTF-8")).append("\"");
			delimiter=",";
		}
		
		String urlString = url.toString();
		// omit the queryString from the url
		int startOfQueryString = urlString.indexOf('?');
		if(startOfQueryString != -1) {
			urlString = urlString.substring(0,startOfQueryString);	
		}
		
		// Build the X-Authorization request header
		String xauth = String.format("OAuth realm=\"%s\",%s", urlString, sb.toString());
		Map<String,String> headers = new TreeMap<String,String>();
		headers.put("X-Authorization", xauth);
		
		return headers;
	}
	
	private String normalizeParams(String httpMethod, URL url, Map<String, String> oauthParams,
			byte[] requestBody) throws UnsupportedEncodingException {


		// Sort the parameters in lexicographical order, 1st by Key then by Value
		Map<String, String> kvpParams = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		kvpParams.putAll(oauthParams);

		// Place any query string parameters into a key value pair using equals
		// ("=") to mark
		// the key/value relationship and join each parameter with an ampersand
		// ("&")
		if (url.getQuery() != null) {
			for (String keyValue : url.getQuery().split("&")) {
				String[] p = keyValue.split("=");
				kvpParams.put(p[0],p[1]);
			}

		}

		// Include the body parameter if dealing with a POST or PUT request
		if ("POST".equals(httpMethod) || "PUT".equals(httpMethod)) {
			String body = Base64.encodeBase64String(requestBody).replaceAll("\r\n", "");
			// url encode the body 2 times now before combining other params
			body = URLEncoder.encode(body, "UTF-8");
			body = URLEncoder.encode(body, "UTF-8");
			kvpParams.put("body", body);
		}
		
		// separate the key and values with a "="
		// separate the kvp with a "&"
		StringBuilder combinedParams = new StringBuilder();
		String delimiter="";
		for(String key : kvpParams.keySet()) {
			combinedParams.append(delimiter);
			combinedParams.append(key);
			combinedParams.append("=");
			combinedParams.append(kvpParams.get(key));
			delimiter="&";
		}
		
		// url encode the entire string again before returning
		return URLEncoder.encode(combinedParams.toString(), "UTF-8");
	}
	
	private String generateSignature(String httpMethod, URL url, Map<String, String> oauthParams,
			byte[] requestBody, String secret) throws UnsupportedEncodingException {
		// Ensure the HTTP Method is upper-cased
		httpMethod = httpMethod.toUpperCase();

		// Construct the URL-encoded OAuth parameter portion of the signature
		// base string
		String encodedParams = normalizeParams(httpMethod, url, oauthParams, requestBody);

		// URL-encode the relative URL
		String encodedUri = URLEncoder.encode(url.getPath(), "UTF-8");

		// Build the signature base string to be signed with the Consumer Secret
		String baseString = String.format("%s&%s&%s", httpMethod, encodedUri, encodedParams);

		return generateCmac(secret, baseString);
	}
	
	private String getNonce() {
		return RandomStringUtils.randomAlphanumeric(32);
	}

	private String getTimestamp() {
		return Long.toString((System.currentTimeMillis() / 1000));
	}
	
	private String generateCmac(String key, String msg) throws UnsupportedEncodingException {
		byte[] keyBytes = key.getBytes("UTF-8");
		byte[] data = msg.getBytes("UTF-8");

		CMac macProvider = new CMac(new AESFastEngine());
		macProvider.init(new KeyParameter(keyBytes));
		macProvider.reset();

		macProvider.update(data, 0, data.length);
		byte[] output = new byte[macProvider.getMacSize()];
		macProvider.doFinal(output, 0);

		// Convert the CMAC to a Base64 string and remove the new line the
		// Base64 library adds
		String cmac = Base64.encodeBase64String(output).replaceAll("\r\n", "");

		return cmac;
	}
	
}
