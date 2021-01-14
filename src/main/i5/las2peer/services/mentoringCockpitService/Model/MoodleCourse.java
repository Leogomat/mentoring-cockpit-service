package i5.las2peer.services.mentoringCockpitService.Model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;

import i5.las2peer.services.mentoringCockpitService.MentoringCockpitService;
import i5.las2peer.services.mentoringCockpitService.Interactions.Completed;
import i5.las2peer.services.mentoringCockpitService.Interactions.UserResourceInteraction;
import i5.las2peer.services.mentoringCockpitService.Interactions.Viewed;
import i5.las2peer.services.mentoringCockpitService.Model.Resources.CompletableResource;
import i5.las2peer.services.mentoringCockpitService.Model.Resources.File;
import i5.las2peer.services.mentoringCockpitService.Model.Resources.Quiz;
import i5.las2peer.services.mentoringCockpitService.Model.Resources.Resource;
import i5.las2peer.services.mentoringCockpitService.Suggestion.MoodleSuggestionEvaluator;
import i5.las2peer.services.mentoringCockpitService.Suggestion.Suggestion;
import i5.las2peer.services.mentoringCockpitService.Suggestion.TextFormatter;
import i5.las2peer.services.mentoringCockpitService.Themes.Theme;
import i5.las2peer.services.mentoringCockpitService.Themes.ThemeResourceLink;
import i5.las2peer.services.mentoringCockpitService.Model.Resources.Hyperlink;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

public class MoodleCourse extends Course {

	public MoodleCourse(String courseid, String courseURL, MentoringCockpitService service) {
		super(courseid, courseURL, service, new MoodleSuggestionEvaluator(0, 1));
	}
	
	@Override
	public void updateKnowledgeBase(long since) {
		setTimeToCurrent();
		newResources.clear();
		
		createResources(since);
		createUsers(since);
		createInteractions(since);
		createThemes(since);
	}

	@Override
	public String getSuggestion(String userid, String courseid) {
		updateKnowledgeBase(lastUpdated);
		Suggestion suggestion =  users.get(userid).getSuggestion();
		if (suggestion != null) {
			System.out.println("DEBUG --- Priority: " + suggestion.getPriority());
			return suggestion.getSuggestionText();
		} else {
			return "No suggestions available";
		}
	}

	@Override
	public String getThemeSuggestions(String themeid, String courseid) {
		ArrayList<String> items = new ArrayList<String>();
		for (ThemeResourceLink link : themes.get("http://halle/domainmodel/" + themeid).getResourceLinks().values()) {
			items.add(link.getSuggestionText());
		}
		return "The following resources are related to the theme " + TextFormatter.quote(themeid) + ":" + TextFormatter.createList(items);
	}

	@Override
	public void createUsers(long since) {
		// Match
		JSONObject match = new JSONObject();
		match.put("statement.context.extensions.https://tech4comp&46;de/xapi/context/extensions/courseInfo.courseid", Integer.parseInt(courseid));
		JSONObject gtObject = new JSONObject();
		gtObject.put("$gt", Instant.ofEpochSecond(since).toString());
		match.put("statement.stored", gtObject);
		JSONObject matchObj = new JSONObject();
		matchObj.put("$match", match);
		
		
		
		// Project
		JSONObject project = new JSONObject();
		project.put("_id", "$statement.actor.account.name");
		project.put("userid", "$statement.actor.account.name");
		project.put("name", "$statement.actor.name");
		JSONObject projectObj = new JSONObject();
		projectObj.put("$project", project);
		
		// Group
		JSONObject groupObject = new JSONObject();
		JSONObject group = new JSONObject();
		JSONObject idObject = new JSONObject();
		JSONObject nameObject = new JSONObject();
		idObject.put("$first", "$userid");
		nameObject.put("$first", "$name");
		group.put("_id", "$_id");
		group.put("userid", idObject);
		group.put("name", nameObject);
		groupObject.put("$group", group);
		
		// Assemble pipeline
		JSONArray pipeline = new JSONArray();
		pipeline.add(matchObj);
		pipeline.add(projectObj);
		pipeline.add(groupObject);
		
		StringBuilder sb = new StringBuilder();
		for (byte b : pipeline.toString().getBytes()) {
			sb.append("%" + String.format("%02X", b));
		}
		
		String res = service.LRSconnect(sb.toString());
		
		//System.out.println("DEBUG --- Users: " + res);
		
		JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
		try {
			JSONArray data = (JSONArray) parser.parse(res);
			for (int i = 0; i < data.size(); i++) {
				JSONObject userObj = (JSONObject) data.get(i);
				users.put(userObj.getAsString("userid"), new MoodleUser(userObj.getAsString("userid"), userObj.getAsString("name"), this));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void createResources(long since) {
		// Match
		JSONObject match = new JSONObject();
		match.put("statement.context.extensions.https://tech4comp&46;de/xapi/context/extensions/courseInfo.courseid", Integer.parseInt(courseid));
		JSONObject gtObject = new JSONObject();
		gtObject.put("$gt", Instant.ofEpochSecond(since).toString());
		match.put("statement.stored", gtObject);
		JSONObject matchObj = new JSONObject();
		matchObj.put("$match", match);
		
		// Project
		JSONObject project = new JSONObject();
		project.put("_id", "$statement.object.id");
		project.put("object", "$statement.object");
		JSONObject projectObj = new JSONObject();
		projectObj.put("$project", project);
		
		// Group
		JSONObject groupObject = new JSONObject();
		JSONObject group = new JSONObject();
		JSONObject nameObject = new JSONObject();
		nameObject.put("$first", "$object.definition.name.en-US");
		group.put("_id", "$_id");
		group.put("name", nameObject);
		groupObject.put("$group", group);
		
		// Assemble pipeline
		JSONArray pipeline = new JSONArray();
		pipeline.add(matchObj);
		pipeline.add(projectObj);
		pipeline.add(groupObject);
		
		StringBuilder sb = new StringBuilder();
		for (byte b : pipeline.toString().getBytes()) {
			sb.append("%" + String.format("%02X", b));
		}
		
		String res = service.LRSconnect(sb.toString());
		
		
		
		JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
		try {
			JSONArray data = (JSONArray) parser.parse(res);
			//System.out.println("DEBUG --- Size: " + data.size());
			for (int i = 0; i < data.size(); i++) {
				JSONObject resourceObj = (JSONObject) data.get(i);
				if (resourceObj.getAsString("_id").contains("quiz")) {
					resources.put(resourceObj.getAsString("_id"), new Quiz(resourceObj.getAsString("_id"), resourceObj.getAsString("name"), resourceObj.getAsString("_id")));
				} else if (resourceObj.getAsString("_id").contains("resource")) {
					resources.put(resourceObj.getAsString("_id"), new File(resourceObj.getAsString("_id"), resourceObj.getAsString("name"), resourceObj.getAsString("_id")));
				} else if (resourceObj.getAsString("_id").contains("url")) {
					resources.put(resourceObj.getAsString("_id"), new Hyperlink(resourceObj.getAsString("_id"), resourceObj.getAsString("name"), resourceObj.getAsString("_id")));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void createThemes(long since) {
		
		// First, create all themes
		try {
			String query = "PREFIX ulo: <http://uni-leipzig.de/tech4comp/ontology/>\r\n" + 
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\r\n" + 
					"	\r\n" + 
					"    SELECT ?s1 ?shortid WHERE {\r\n" + 
					"  		GRAPH <http://triplestore.tech4comp.dbis.rwth-aachen.de/Wissenslandkarten/data/%s> {\r\n" + 
					"  			?s1 a ulo:Theme .  \r\n" + 
					"  			?s1 rdfs:label ?shortid .  \r\n" + 
					"		}\r\n" + 
					"    }";
			
			String response = sparqlQuery(query);
			ArrayList<String> themeids = new ArrayList<String>();
			
			JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
			JSONObject responseObj = (JSONObject) parser.parse(response.toString());
			JSONObject resultsObj = (JSONObject) responseObj.get("results");
			JSONArray bindingsArray = (JSONArray) resultsObj.get("bindings");
			for (int i = 0; i < bindingsArray.size(); i++) {
				JSONObject bindingObj = (JSONObject) bindingsArray.get(i);
				JSONObject subjectObj = (JSONObject) bindingObj.get("s1");
				themeids.add(subjectObj.getAsString("value"));
			}
			
			System.out.println("DEBUG --- URIS: " + themeids.toString());
			
			for (String themeid : themeids) {
				themes.put(themeid, new Theme(themeid));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Then, create theme structure
		try {
			String query = "PREFIX ulo: <http://uni-leipzig.de/tech4comp/ontology/>\r\n" + 
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\r\n" + 
					"	\r\n" + 
					"    SELECT ?s1 ?o1 WHERE {\r\n" + 
					"  		GRAPH <http://triplestore.tech4comp.dbis.rwth-aachen.de/Wissenslandkarten/data/Moodle_18> {\r\n" + 
					"  			?s1 ulo:superthemeOf ?o1 .  \r\n" + 
					"		}\r\n" + 
					"    }";
			
			String response = sparqlQuery(query);
			
			JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
			JSONObject responseObj = (JSONObject) parser.parse(response.toString());
			JSONObject resultsObj = (JSONObject) responseObj.get("results");
			JSONArray bindingsArray = (JSONArray) resultsObj.get("bindings");
			for (int i = 0; i < bindingsArray.size(); i++) {
				JSONObject bindingObj = (JSONObject) bindingsArray.get(i);
				JSONObject subjectObj = (JSONObject) bindingObj.get("s1");
				JSONObject objectObj = (JSONObject) bindingObj.get("o1");
				themes.get(subjectObj.getAsString("value")).addSubtheme(themes.get(objectObj.getAsString("value")));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// Finally, assign resources and resource information
		try {
			String query = "PREFIX ulo: <http://uni-leipzig.de/tech4comp/ontology/>\r\n" + 
					"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\r\n" + 
					"    SELECT ?themeid ?resourceid ?infoType ?infoVal WHERE {\r\n" + 
					"  		GRAPH <http://triplestore.tech4comp.dbis.rwth-aachen.de/Wissenslandkarten/data/Moodle_18> {\r\n" + 
					"    		?themeid ulo:continuativeMaterial ?s1 .\r\n" + 
					"  			?s1 ulo:id ?resourceid .\r\n" + 
					"    		?s1 ?infoType ?infoVal .\r\n" + 
					"  		} \r\n" + 
					"    } ";
			
			String response = sparqlQuery(query);
			
			JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
			JSONObject responseObj1 = (JSONObject) parser.parse(response.toString());
			JSONObject resultsObj1 = (JSONObject) responseObj1.get("results");
			JSONArray bindingsArray1 = (JSONArray) resultsObj1.get("bindings");
			for (int i = 0; i < bindingsArray1.size(); i++) {
				JSONObject bindingObj = (JSONObject) bindingsArray1.get(i);
				String themeid = ((JSONObject) bindingObj.get("themeid")).getAsString("value");
				String resourceid = ((JSONObject) bindingObj.get("resourceid")).getAsString("value");
				if (resources.containsKey(resourceid)) {
					if (!themes.get(themeid).getResourceLinks().containsKey(resourceid)) {
						themes.get(themeid).getResourceLinks().put(resourceid, new ThemeResourceLink(resources.get(resourceid)));
					}
					
					String infoType = ((JSONObject) bindingObj.get("infoType")).getAsString("value");
					String infoVal = ((JSONObject) bindingObj.get("infoVal")).getAsString("value");
					themes.get(themeid).getResourceLinks().get(resourceid).addInfo(infoType, infoVal);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String sparqlQuery(String query) {
		try {
			URL url = new URL(service.triplestoreDomain);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/sparql-query");
			conn.setRequestProperty("Accept", "application/json");
			conn.setDoOutput(true);
			
			try(OutputStream os = conn.getOutputStream()) {
			    byte[] input = String.format(query, "Moodle_" + courseid).getBytes("utf-8");
			    os.write(input, 0, input.length);			
			}
			StringBuilder response = new StringBuilder();
			try(BufferedReader br = new BufferedReader(
					  new InputStreamReader(conn.getInputStream(), "utf-8"))) {
					    String responseLine = null;
					    while ((responseLine = br.readLine()) != null) {
					        response.append(responseLine.trim());
					    }
					    System.out.println("DEBUG --- RESPONSE: " + response.toString());
					}
			return response.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "SPARQL connection failed";
		}
	}

	@Override
	public void createInteractions(long since) {
		// Match
		JSONObject match = new JSONObject();
		match.put("statement.context.extensions.https://tech4comp&46;de/xapi/context/extensions/courseInfo.courseid", Integer.parseInt(courseid));
		JSONObject matchObj = new JSONObject();
		JSONObject gtObject = new JSONObject();
		gtObject.put("$gt", Instant.ofEpochSecond(since).toString());
		match.put("statement.stored", gtObject);
		matchObj.put("$match", match);
		
		// Project
		JSONObject project = new JSONObject();
		JSONObject idObject = new JSONObject();
		idObject.put("userid", "$statement.actor.account.name");
		idObject.put("verb", "$statement.verb.id");
		idObject.put("result", "$statement.result");
		idObject.put("timestamp", "$statement.stored");
		idObject.put("objectid", "$statement.object.id");
		project.put("_id", idObject);
		JSONObject projectObj = new JSONObject();
		projectObj.put("$project", project);
		
		// Group
		JSONObject groupObject = new JSONObject();
		JSONObject group = new JSONObject();
		group.put("_id", "$_id");
		groupObject.put("$group", group);
		
		// Assemble pipeline
		JSONArray pipeline = new JSONArray();
		pipeline.add(matchObj);
		pipeline.add(projectObj);
		pipeline.add(groupObject);
		
		StringBuilder sb = new StringBuilder();
		for (byte b : pipeline.toString().getBytes()) {
			sb.append("%" + String.format("%02X", b));
		}
		
		String res = service.LRSconnect(sb.toString());
		
		//System.out.println("DEBUG --- Relations: " + res);
		
		JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
		try {
			JSONArray data = (JSONArray) parser.parse(res);
			
			//System.out.println("DEBUG --- Size: " + data.size());
			for (int i = 0; i < data.size(); i++) {
				JSONObject dataObj = (JSONObject) data.get(i);
				JSONObject relationObj = (JSONObject) dataObj.get("_id");
				User user = users.get(relationObj.getAsString("userid"));
				Resource resource = resources.get(relationObj.getAsString("objectid"));
				String verb = relationObj.getAsString("verb");
				long timestamp = Instant.parse(relationObj.getAsString("timestamp")).getEpochSecond();
				
				if (resource != null) {
					//TODO: Add more verbs
					UserResourceInteraction interaction = null;
					if (verb.contains("completed")) {
						JSONObject resultObject = (JSONObject) relationObj.get("result");
						if (Boolean.parseBoolean(resultObject.getAsString("completion"))) {
							CompletableResource completableResource = (CompletableResource) resource;
							JSONObject scoreObject = (JSONObject) resultObject.get("score");
							interaction = new Completed(timestamp, user, completableResource, Double.parseDouble(scoreObject.getAsString("scaled")));
						}
					} else if (verb.contains("viewed")) {
						interaction = new Viewed(timestamp, user, resource);
					}
					
					// Add interactions to interaction lists
					if (interaction != null) {
						if (!user.getInteractionLists().containsKey(resource.getId())) {
							user.getInteractionLists().put(resource.getId(), new ArrayList<UserResourceInteraction>());
						}
						user.getInteractionLists().get(resource.getId()).add(interaction);
					}
					
					// Add resource to user's newly interacted resource list
					user.getRecentlyInteractedResources().add(resource);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
