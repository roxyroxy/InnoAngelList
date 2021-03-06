package inno;

import inno.models.Location;
import inno.models.QueryStartup;
import inno.models.Startup;
import inno.models.Startups;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@RestController
public class Service {

	private AccessToken token = new AccessToken();
	private Gson gson = new Gson();
	private static final int ERROR_CODE = -1;
	static Logger log = Logger.getLogger(Service.class.getName());

	@RequestMapping(value = "/accesscode", method = RequestMethod.GET)
	public ModelAndView redirectToAccessCode() {
		String redirectUrl = "https://angel.co/api/oauth/authorize?client_id="
				+ User.getClientid() + "&response_type=code";

		return new ModelAndView("redirect:" + redirectUrl);
	}

	@RequestMapping(value = "/inno", method = RequestMethod.GET)
	public ModelAndView getAccessCode(@RequestParam(value = "code") String code) {
		User auth_user = new User(code);
		Gson gson = new Gson();
		String postUrl = "https://angel.co/api/oauth/token?client_id="
				+ User.getClientid() + "&client_secret="
				+ User.getClientsecret() + "&code=" + auth_user.getCode()
				+ "&grant_type=authorization_code";
		HttpClient httpClient = HttpClientBuilder.create().build();
		ResponseHandler<String> res = new BasicResponseHandler();

		HttpPost postMethod = new HttpPost(postUrl);
		try {
			String httpResponse = httpClient.execute(postMethod, res);
			token = gson.fromJson(httpResponse, AccessToken.class);

		} catch (ClientProtocolException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
		ModelAndView mav = new ModelAndView();
		mav.addObject("test", "test1");
		mav.setViewName("index");
		return mav;

	}

	@RequestMapping(value = "/startupsByTagLocation/{id}", method = RequestMethod.GET)
	public void getAllStartups(@PathVariable("id") int id) {

		String url = "https://api.angel.co/1/tags/" + id
				+ "/startups?access_token=" + token.getAccess_token();

		HttpClient httpClient = HttpClientBuilder.create().build();
		ResponseHandler<String> res = new BasicResponseHandler();
		HttpGet getRequest = new HttpGet(url);
		try {
			String getResponse = httpClient.execute(getRequest, res);
			System.out.println(getResponse);
		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	@RequestMapping(value = "/getLocationTagId/{name}", method = RequestMethod.GET)
	public int getLocationTagId(@PathVariable("name") String name) {

		String url = "https://api.angel.co/1/search/" + "?query=" + name
				+ "&type=LocationTag" + "&access_token="
				+ token.getAccess_token();
		HttpClient httpClient = HttpClientBuilder.create().build();
		ResponseHandler<String> res = new BasicResponseHandler();
		HttpGet getRequest = new HttpGet(url);
		try {
			String getResponse = httpClient.execute(getRequest, res);
			JsonParser parser = new JsonParser();
			JsonArray jArray = parser.parse(getResponse).getAsJsonArray();

			List<Location> locations = new ArrayList<Location>();

			for (JsonElement obj : jArray) {
				Location loc = gson.fromJson(obj, Location.class);
				locations.add(loc);
			}
			// TODO deal with this
			if (locations.size() > 1) {
				log.warn("There are more locations with the same name!");
			}
			if (!locations.isEmpty()) {
				return locations.get(0).getId();
			}
			else{
				log.error("No locations with that name has been found");
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
		return ERROR_CODE;
	}

	@RequestMapping(value = "getById/{id}", method = RequestMethod.GET)
	public void getById(@PathVariable("id") int id) {

		String url = "https://api.angel.co/1/startups/" + id + "?access_token="
				+ token.getAccess_token();
		HttpClient httpClient = HttpClientBuilder.create().build();
		ResponseHandler<String> res = new BasicResponseHandler();
		HttpGet getRequest = new HttpGet(url);
		try {
			String getResponse = httpClient.execute(getRequest, res);
			System.out.println(getResponse);
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/getStartupsByLocation/{locationName}", method = RequestMethod.GET)
	public ModelAndView getStartupsByLocation(
			@PathVariable("locationName") String locationName) {
		
		System.out.println(locationName);
		
		int locationId = getLocationTagId(locationName);
		Startups total_startups = new Startups();

		for (int pageNo = 1; pageNo < 10; pageNo++) {
			String url = "https://api.angel.co/1/tags/" + locationId
					+ "/startups?access_token=" + token.getAccess_token()
					+ "&?order=popularity&page=" + pageNo;
			HttpClient httpClient = HttpClientBuilder.create().build();
			ResponseHandler<String> res = new BasicResponseHandler();
			HttpGet getRequest = new HttpGet(url);
			Startups startups_perpage = new Startups();
			try {
				String getResponse = httpClient.execute(getRequest, res);
				startups_perpage = gson.fromJson(getResponse, Startups.class);

			} catch (IOException e) {
				e.printStackTrace();
			}
			total_startups.getStartups().addAll(startups_perpage.getStartups());
		}
		if (total_startups.getStartups().isEmpty()){
			log.warn("No startup with that name has been found");
		}
		System.out.println("SIZE:" + total_startups.getStartups().size());
		
		ModelAndView mav = new ModelAndView();
		mav.addObject("total_startups_by_location", total_startups.getStartups());
		mav.setViewName("startupsTable");
		return mav;
	}

	@RequestMapping(value = "/startupByName", method = RequestMethod.POST)
	public ModelAndView getStartupByName(@RequestBody Object object) {

		String element = object.toString();
		Gson gsonElement = new Gson();

		String name = gsonElement.fromJson(element, QueryStartup.class)
				.getName();
		
		System.out.println("Getting startups with the name: " + name);
		
		String url = "https://api.angel.co/1/search" + "?query=" + name
				+ "&access_token=" + token.getAccess_token();

		HttpClient httpClient = HttpClientBuilder.create().build();
		ResponseHandler<String> res = new BasicResponseHandler();
		HttpGet getRequest = new HttpGet(url);
		List<Startup> startups = new ArrayList<Startup>();

		try {
			String getResponse = httpClient.execute(getRequest, res);
			JsonParser parser = new JsonParser();
			JsonArray jArray = parser.parse(getResponse).getAsJsonArray();
			for (JsonElement obj : jArray) {
				Startup startup = gson.fromJson(obj, Startup.class);
				startups.add(startup);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		ModelAndView mav = new ModelAndView();
		mav.addObject("total_startups_by_location",
				startups);

		mav.setViewName("startupsList");
		return mav;
	}

	@RequestMapping(value = "/startupsTable", method = RequestMethod.POST)
	public ModelAndView getStartupsTable(@RequestBody Object object) {

		String element = object.toString();
		Gson gsonElement = new Gson();

		String location = gsonElement.fromJson(element, QueryStartup.class)
				.getLocation();

		int qualityIndex;

		try {
			qualityIndex = gsonElement.fromJson(element, QueryStartup.class)
					.getQualityIndex();
		} catch (Exception e) {
			qualityIndex = -1;
		}

		System.out.println("Querying after location = " + location
				+ " and quality index = " + qualityIndex);

		int locationId = getLocationTagId(location);
		Startups total_startups = new Startups();

		for (int pageNo = 1; pageNo < 10; pageNo++) {
			String url = "https://api.angel.co/1/tags/" + locationId
					+ "/startups?access_token=" + token.getAccess_token()
					+ "&?order=popularity&page=" + pageNo;
			HttpClient httpClient = HttpClientBuilder.create().build();
			ResponseHandler<String> res = new BasicResponseHandler();
			HttpGet getRequest = new HttpGet(url);
			Startups startups_perpage = new Startups();
			try {
				String getResponse = httpClient.execute(getRequest, res);
				startups_perpage = gson.fromJson(getResponse, Startups.class);

			} catch (IOException e) {
				e.printStackTrace();
			}
			total_startups.getStartups().addAll(startups_perpage.getStartups());
		}
		if (total_startups.getStartups().isEmpty()) {
			log.warn("No startup with that name has been found");
		}
		System.out.println("# Startups in " + location + " = "
				+ total_startups.getStartups().size());
		
		ModelAndView mav = new ModelAndView();
		mav.setViewName("startupsList");
		
		if (qualityIndex >= 0) {
			List<Startup> filteredStartups = new ArrayList<Startup>();
			for (Startup startup : total_startups.getStartups()) {
				if (startup.getQuality() == qualityIndex) {
					filteredStartups.add(startup);
				}
			}
			System.out.println("#Filtered startups = "
					+ filteredStartups.size());
			mav.addObject("total_startups_by_location", filteredStartups);
		} else {
			mav.addObject("total_startups_by_location",
					total_startups.getStartups());
		}		
		return mav;
	}
}