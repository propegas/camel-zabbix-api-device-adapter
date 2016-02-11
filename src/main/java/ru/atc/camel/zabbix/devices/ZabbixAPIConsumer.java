package ru.atc.camel.zabbix.devices;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import io.github.hengyunabc.zabbix.api.ZabbixApi;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;
//import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
//import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
//import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonObject;
import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;
import io.github.hengyunabc.zabbix.api.Request;
import io.github.hengyunabc.zabbix.api.RequestBuilder;
import ru.at_consulting.itsm.device.Device;
import ru.at_consulting.itsm.event.Event;
import scala.xml.dtd.ParameterEntityDecl;

public class ZabbixAPIConsumer extends ScheduledPollConsumer {

	private static Logger logger = LoggerFactory.getLogger(Main.class);

	private static ZabbixAPIEndpoint endpoint;

	//private static String SavedWStoken;

	private static CloseableHttpClient httpClient;

	public enum PersistentEventSeverity {
		OK, INFO, WARNING, MINOR, MAJOR, CRITICAL;

		public String value() {
			return name();
		}

		public static PersistentEventSeverity fromValue(String v) {
			return valueOf(v);
		}
	}

	public ZabbixAPIConsumer(ZabbixAPIEndpoint endpoint, Processor processor) {
		super(endpoint, processor);
		ZabbixAPIConsumer.endpoint = endpoint;
		// this.afterPoll();
		this.setTimeUnit(TimeUnit.MINUTES);
		this.setInitialDelay(0);
		this.setDelay(endpoint.getConfiguration().getDelay());
	}

	@Override
	protected int poll() throws Exception {

		String operationPath = endpoint.getOperationPath();

		if (operationPath.equals("devices"))
			return processSearchDevices();

		// only one operation implemented for now !
		throw new IllegalArgumentException("Incorrect operation: " + operationPath);
	}

	@Override
	public long beforePoll(long timeout) throws Exception {

		logger.info("*** Before Poll!!!");
		// only one operation implemented for now !
		// throw new IllegalArgumentException("Incorrect operation: ");

		// send HEARTBEAT
		genHeartbeatMessage(getEndpoint().createExchange());

		return timeout;
	}

	private int processSearchDevices() throws Exception, Error {

		// Long timestamp;

		List<Device> hostsList;
		List<Device> itemsList;
		
		List<Device> hostgroupsList;
		List<Device> listFinal = new ArrayList<Device>();

		String eventsuri = endpoint.getConfiguration().getZabbixapiurl();
		String uri = String.format("%s", eventsuri);

		System.out.println("***************** URL: " + uri);

		logger.info("Try to get Hosts.");
		// logger.info("Get events URL: " + uri);

		//JsonObject json = null;

		DefaultZabbixApi zabbixApi = null;
		try {
			String zabbixapiurl = endpoint.getConfiguration().getZabbixapiurl();
			String username = endpoint.getConfiguration().getUsername();
			String password = endpoint.getConfiguration().getPassword();
			// String url = "http://192.168.90.102/zabbix/api_jsonrpc.php";
			zabbixApi = new DefaultZabbixApi(zabbixapiurl);
			zabbixApi.init();

			boolean login = zabbixApi.login(username, password);
			//System.err.println("login:" + login);
			if (!login) {
				
				throw new RuntimeException("Failed to login to Zabbix API.");
			}

		
			// Get all Hosts from Zabbix
			hostsList = getAllHosts(zabbixApi);
			if (hostsList != null)
				listFinal.addAll(hostsList);
			
			// Get all HostGroups from Zabbix
			hostgroupsList = getAllHostGroups(zabbixApi);
			if (hostgroupsList != null)
				listFinal.addAll(hostgroupsList);
			
			// Get all Items marked as CI from Zabbix
			// TODO Remove before send to production
			itemsList = getAllCiItems(zabbixApi);
			//itemsList = null;
			if (itemsList != null)
				listFinal.addAll(itemsList);


			for (Device aListFinal : listFinal) {
				logger.info("Create Exchange container");
				Exchange exchange = getEndpoint().createExchange();
				exchange.getIn().setBody(aListFinal, Device.class);
				exchange.getIn().setHeader("DeviceId", aListFinal.getId());
                exchange.getIn().setHeader("ParentId", aListFinal.getParentID());
				exchange.getIn().setHeader("DeviceType", aListFinal.getDeviceType());
				exchange.getIn().setHeader("queueName", "Devices");

				try {
					getProcessor().process(exchange);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			logger.info("Sended Devices: " + listFinal.size());
			
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(String.format("Error while get Devices from API: %s ", e));
			genErrorMessage(e.getMessage() + " " + e.toString());
			httpClient.close();
			return 0;
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(String.format("Error while get Devices from API: %s ", e));
			genErrorMessage(e.getMessage() + " " + e.toString());
			httpClient.close();
			if (zabbixApi != null) {
				zabbixApi.destory();
			}
			return 0;
		} finally {
			logger.debug(String.format(" **** Close zabbixApi Client: %s",
					zabbixApi != null ? zabbixApi.toString() : null));
			// httpClient.close();
			if (zabbixApi != null) {
				zabbixApi.destory();
			}
			// dataSource.close();
			// return 0;
		}

		return 1;
	}

	private List<Device> getAllCiItems(DefaultZabbixApi zabbixApi) {
		
		// zabbix_item_ke_search_pattern=[*]*
		String itemCiSearchPattern = endpoint.getConfiguration().getItemCiSearchPattern();
		
		logger.debug("*** Try to get Item CI using Pattern: " + itemCiSearchPattern);
		
		Request getRequest;
		JSONObject getResponse;
		// JsonObject params = new JsonObject();
		try {
			// String host1 = "172.20.14.68";
			// String host2 = "TGC1-ASODU2";
			JSONObject search = new JSONObject();
			// JSONObject output = new JSONObject();

			search.put("name", new String[] { itemCiSearchPattern });
			// output.put("output", new String[] { "hostid", "name", "host" });

			getRequest = RequestBuilder.newBuilder().method("item.get")
					.paramEntry("search", search)
					.paramEntry("output", new String[] { "itemid", "name", "key_", "description" })
					.paramEntry("monitored", true )
					.paramEntry("searchWildcardsEnabled", true )
					.paramEntry("selectHosts", new String[] { "name", "host", "hostid" })
					.build();

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed create JSON request for get all CI Items.");
		}

		// logger.info(" *** Get All host JSON params: " + params.toString());

		// JsonObject json = null;
		JSONArray hostitems;
		try {
			getResponse = zabbixApi.call(getRequest);
			//System.err.println(getResponse);

			hostitems = getResponse.getJSONArray("result");
			//System.err.println(hosts);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Failed get JSON response result for all CI Items.");
		}
		List<Device> deviceList = new ArrayList<>();
		
		List<Device> listFinal = new ArrayList<>();

		String device_type;
		String newcitname;
		logger.info("Finded Zabbix CI Items : " + hostitems.size());

		device_type = "item";
		newcitname = "";
		
		HashMap<String,Object> row = new HashMap<>();
		
		for (int y = 0; y < hostitems.size(); y++) {
			
			JSONObject item = hostitems.getJSONObject(y);
			
			JSONArray host = item.getJSONArray("hosts");
			
			String hostHost = host.getJSONObject(0).getString("host");
			String hostName = host.getJSONObject(0).getString("name");
			// check hostHost or hostName for aliases pattern host--alias
			String cihost = checkHostPattern(hostHost,hostName);
			// if host has no aliases pattern, default hostHost
			if (cihost == null) {
				cihost = hostHost;
			}

			String hostid = host.getJSONObject(0).getString("hostid");
			String name = item.get("name").toString();
			String key = item.get("key_").toString();
			
			// parse $1, $2 etc params in item name from key_ params 
			if (name.matches(".*\\$\\d+.*")){
				name = getTransformedItemName(name,	key);
			}

			// get hash (ciid) and parsed name for CI item
			// generate Device json message
			String[] checkreturn = checkItemForCi(name, cihost);
			if ( !checkreturn[0].isEmpty() ){
				String ciid = checkreturn[0];
				newcitname = checkreturn[1];
				
				//int columns = 2;
	        	//HashMap<String,Object> row = new HashMap<String, Object>();
	     
	        	//row.put(ciid,openids[i]);
				
				String[] devicearr = new String[] { cihost, ciid, device_type, newcitname, hostid };
				
				row.put(ciid, devicearr);
				
			}
			
			// JSONArray hostgroups = host.getJSONArray("groups");

			//logger.debug("******** Received JSON hostitem: " + hostitem.toString());
		}
		
		/* Display content using Iterator*/
		//String hostname = "", ciid = "", device_type = "", newcitname = "", hostid = "";
		for (Entry<String, Object> entry: row.entrySet()){
		    System.out.println(entry.getKey() + " = " + entry.getValue());

		    String[] devicearr = (String[]) entry.getValue();
		    //hostname = devicearr[0];


		    Device gendevice;
			gendevice = genHostObj(devicearr[0], devicearr[1], devicearr[2], devicearr[3], devicearr[4]);
		    deviceList.add(gendevice);
		}
		
		
		listFinal.addAll(deviceList);
		
		return listFinal;

	}

	private String checkHostPattern(String hostHost, String hostName) {
		String name = null;
		// Example: KRL-PHOBOSAU--MSSQL
		String[] hostreturn = new String[] { "", "" } ;
		Pattern p = Pattern.compile("(.*)--(.*)");

		logger.debug("*** Check hostName and hostHost for aliases: " + hostHost.toUpperCase());

		Matcher hostHostMatcher = p.matcher(hostHost.toUpperCase());
		Matcher hostNameMatcher = p.matcher(hostName.toUpperCase());
		//String output = "";
		if (hostHostMatcher.matches()){
			name = hostHost;
		}
		else if (hostNameMatcher.matches()){
			name = hostName;
		}
		return name;
	}

	private List<Device> getAllHostGroups(DefaultZabbixApi zabbixApi) {
		
		// zabbix_group_search_pattern=(*)*
		String groupSearchPattern = endpoint.getConfiguration().getGroupSearchPattern();
		
		Request getRequest;
		JSONObject getResponse;
		// JsonObject params = new JsonObject();
		
		logger.debug("*** Try to get Zabbix Groups using Pattern: " + groupSearchPattern);
		
		try {
			// String host1 = "172.20.14.68";
			// String host2 = "TGC1-ASODU2";
			 JSONObject search = new JSONObject();
			// JSONObject output = new JSONObject();

			 search.put("name", new String[] { groupSearchPattern });
			// output.put("output", new String[] { "hostid", "name", "host" });

			getRequest = RequestBuilder.newBuilder().method("hostgroup.get")
					.paramEntry("search", search)
					.paramEntry("output", "extend")
					.paramEntry("searchWildcardsEnabled", true )
					.build();

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed create JSON request for get all Groups.");
		}

		// logger.info(" *** Get All host JSON params: " + params.toString());

		// JsonObject json = null;
		JSONArray hostgroups;
		try {
			getResponse = zabbixApi.call(getRequest);
			logger.debug("Zabbix Groups getRequest: " + getRequest);
			logger.debug("Zabbix Groups getResponse: " + getResponse);

			hostgroups = getResponse.getJSONArray("result");
			//System.err.println(hostgroups);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Failed get JSON response result for all Hosts.");
		}
		List<Device> deviceList = new ArrayList<Device>();
		
		//List<Device> listFinal = new ArrayList<Device>();
		//List<Device> listFinal = new ArrayList<Device>();
		String device_type = "NodeGroup";
		//String ParentID = "";
		//String newhostname = "";
		logger.info("Finded Zabbix Groups: " + hostgroups.size());
		
		for (int i = 0; i < hostgroups.size(); i++) {
			//device_type = "group";

			JSONObject hostgroup = hostgroups.getJSONObject(i);
			String hostgroupname = hostgroup.getString("name");
			// Example: KRL-PHOBOSAU--MSSQL
			
			logger.debug("*** Received JSON Group: " + hostgroup.toString());
			
			Device gendevice;
			gendevice = genHostgroupObj(hostgroup, device_type, "");
			deviceList.add(gendevice);
			
		}
		
		return deviceList;
	}

	private List<Device> getAllHosts(DefaultZabbixApi zabbixApi) {

		Request getRequest;
		JSONObject getResponse;
		// JsonObject params = new JsonObject();
		try {
			// String host1 = "172.20.14.68";
			// String host2 = "TGC1-ASODU2";
			// JSONObject filter = new JSONObject();
			// JSONObject output = new JSONObject();

			// filter.put("host", new String[] { host1, host2 });
			// output.put("output", new String[] { "hostid", "name", "host" });

			getRequest = RequestBuilder.newBuilder().method("host.get")
					// .paramEntry("filter", filter)
					.paramEntry("output", new String[] { "hostid", "name", "host" })
					.paramEntry("selectMacros", new String[] { "hostmacroid", "macro", "value" })
					.paramEntry("selectGroups", "extend")
					.paramEntry("selectParentTemplates", new String[] { "templateid", "host", "name" })
					//.paramEntry("selectItems", new String[] { "itemid", "name", "key_", "description" })
					.build();

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed create JSON request for get all Hosts.");
		}

		// logger.info(" *** Get All host JSON params: " + params.toString());

		// JsonObject json = null;
		JSONArray hosts;
		try {
			getResponse = zabbixApi.call(getRequest);
			//System.err.println(getResponse);

			hosts = getResponse.getJSONArray("result");
			//System.err.println(hosts);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Failed get JSON response result for all Hosts.");
		}
		List<Device> deviceList = new ArrayList<>();
		
		List<Device> listFinal = new ArrayList<>();
		//List<Device> listFinal = new ArrayList<Device>();
		String device_type = "host";
		String ParentID = "";
		String newhostname = "";
		logger.info("Finded Zabbix Hosts: " + hosts.size());

		for (int i = 0; i < hosts.size(); i++) {
			device_type = "host";
			ParentID = "";

			// logger.debug(f.toString());
			// ZabbixAPIHost host = new ZabbixAPIHost();
			JSONObject host = hosts.getJSONObject(i);
			String hostHost = host.getString("host");
			String hostName = host.getString("name");
			newhostname = hostName;

			String hostid = host.getString("hostid");
			// Example: KRL-PHOBOSAU--MSSQL
			if (hostHost.matches("(.*)--(.*)") || hostName.matches("(.*)--(.*)") ){

				logger.info(String.format("Finded Zabbix Host with Aliases: %s (%s)", hostHost, hostName));
				String[] checkreturn = checkHostAliases(hosts, hostHost, hostName);
				ParentID = checkreturn[0];
				newhostname = checkreturn[1];
			}
			
			JSONArray hostgroups = host.getJSONArray("groups");
			JSONArray hosttemplates = host.getJSONArray("parentTemplates");
			//JSONArray hostitems = host.getJSONArray("items");
			JSONArray hostmacros = host.getJSONArray("macros");

			logger.debug("*** Received JSON Host: " + host.toString());
			
			// if ParentID is not set in host name
			// Example: 
			
			// zabbix_group_ci_pattern=\\((.*)\\)(.*)
			String groupCiSearchPattern = endpoint.getConfiguration().getGroupCiPattern();
						
			if (ParentID.equals("")){
				hostgroupsloop:
					for (int j = 0; j < hostgroups.size(); j++) {
						// logger.debug(f.toString());
						// ZabbixAPIHost host = new ZabbixAPIHost();
						JSONObject hostgroup = hostgroups.getJSONObject(j);
						String name = hostgroup.getString("name");
						//if (name.startsWith("[")) {
						if (name.matches(groupCiSearchPattern)) {
							logger.debug("************* Found ParentGroup hostgroup: " + name);
							ParentID = hostgroup.getString("groupid");
							logger.debug("************* Found ParentGroup hostgroup value: " + ParentID);
							break hostgroupsloop;
						}
						// JSONArray hostgroups = host.getJSONArray("groups");

						logger.debug("******** Received JSON Hostgroup: " + hostgroup.toString());
					}
			}
			

			for (int x = 0; x < hosttemplates.size(); x++) {
				// logger.debug(f.toString());
				// ZabbixAPIHost host = new ZabbixAPIHost();
				JSONObject hostgtemplate = hosttemplates.getJSONObject(x);
				// JSONArray hostgroups = host.getJSONArray("groups");

				logger.debug("******** Received JSON Hosttemplate: " + hostgtemplate.toString());
			}

			/*
			for (int y = 0; y < hostitems.size(); y++) {
				// logger.debug(f.toString());
				// ZabbixAPIHost host = new ZabbixAPIHost();
				JSONObject hostitem = hostitems.getJSONObject(y);
				
				String[] checkreturn = checkItemForCi(hostitem.get("name").toString(), hostname);
				if ( !checkreturn[0].isEmpty() ){
					
				}
				
				// JSONArray hostgroups = host.getJSONArray("groups");

				//logger.debug("******** Received JSON hostitem: " + hostitem.toString());
			}
			*/

			//hostmacrosloop:
			logger.debug("******** Check Host's type-macros ");
			device_type = findDeviceTypeFromMacros(hostmacros);

			// try to receive templates type-macros
			if (device_type.equals("")) {
				logger.debug("******** Check Template's type-macros ");
				device_type = getMacrosFromTemplates(zabbixApi, hosttemplates);
			}

			// set default type
			if (device_type.equals("")) {
				device_type = "host";
			}

			Device gendevice;
			String hostnameorig = host.getString("host");
			gendevice = genHostObj(hostnameorig, hostid, device_type, newhostname, ParentID);
			deviceList.add(gendevice);

			// fckey = host.getKey();

			// logger.info("Try to get fcSwitches for FCfabric " + fckey);
			// List<Device> fcSwitches = processSearchFcswitchesByFckey(fckey);
			// logger.info("Finded fcSwitches: "+ fcSwitches.size() + " for
			// FCfabric " + fckey);
			// fcSwitches = processEnrichPhysicalSwitches(fcSwitches,fckey);

			// listFinal.addAll(fcSwitches);

		}
		
		/*
		 * 
		 * if (hosts == null){ throw new RuntimeException(
		 * "Failed get JSON response result for all Hosts."); }
		 * 
		 * Gson gson = new
		 * GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
		 * 
		 * logger.info("Received " + hosts.size() + " Total Hosts." );
		 * 
		 * List<Device> deviceList = new ArrayList<Device>(); List<Device>
		 * listFinal = new ArrayList<Device>();
		 * 
		 * //String fckey;
		 * 
		 * //ZabbixAPIHost host;
		 * 
		 * for (JsonElement f : hosts) { logger.debug(f.toString());
		 * ZabbixAPIHost host = new ZabbixAPIHost(); host = gson.fromJson(f,
		 * ZabbixAPIHost.class);
		 * 
		 * logger.info("**** Received JSON Host: " + host.toString() );
		 * 
		 * //Device gendevice = new Device(); //gendevice = genHostObj( host,
		 * "host" ); //deviceList.add(gendevice);
		 * 
		 * //fckey = host.getKey();
		 * 
		 * //logger.info("Try to get fcSwitches for FCfabric " + fckey);
		 * //List<Device> fcSwitches = processSearchFcswitchesByFckey(fckey);
		 * //logger.info("Finded fcSwitches: "+ fcSwitches.size() +
		 * " for FCfabric " + fckey); //fcSwitches =
		 * processEnrichPhysicalSwitches(fcSwitches,fckey);
		 * 
		 * //listFinal.addAll(fcSwitches);
		 * 
		 * }
		 * 
		 * logger.info("Finded Hosts: "+ deviceList.size());
		 */
		listFinal.addAll(deviceList);
		return listFinal;
	}

	private String getMacrosFromTemplates(DefaultZabbixApi zabbixApi, JSONArray hosttemplates) {

		Request getRequest;
		JSONObject getResponse;
		// JsonObject params = new JsonObject();
		try {
			int arraySize = hosttemplates.size();

			String[] stringArray = new String[arraySize];

			for(int i=0; i<arraySize; i++) {
				stringArray[i] = hosttemplates.getJSONObject(i).getString("templateid");
			}
			getRequest = RequestBuilder.newBuilder().method("template.get")
					.paramEntry("templateids", stringArray)
					//.paramEntry("output", new String[] { "hostid", "name", "host" })
					.paramEntry("selectMacros", new String[] { "hostmacroid", "macro", "value" })
					//.paramEntry("selectItems", new String[] { "itemid", "name", "key_", "description" })
					.build();

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed create JSON request for get Template's macros.");
		}

		// logger.info(" *** Get All host JSON params: " + params.toString());

		// JsonObject json = null;
		JSONArray templates;
		try {
			getResponse = zabbixApi.call(getRequest);
			logger.debug("******** Template macros getRequest:  " + getRequest);
			logger.debug("******** Template macros getResponse:  " + getResponse);
			//System.err.println(getResponse);

			templates = getResponse.getJSONArray("result");
			//System.err.println(hosts);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Failed get JSON response result for get Template's macros.");
		}

		JSONArray allMacros = new JSONArray();
		//allMacros.
		for (int i = 0; i < templates.size(); i++) {
			JSONArray macros = templates.getJSONObject(i).getJSONArray("macros");
			logger.debug("******** Add macros to template macros array: " +
					macros);
			//allMacros.add(i, (JSONArray) macros);
			allMacros.addAll(macros);
			//allMacros = concatArray(allMacros , macros);
		}


		return findDeviceTypeFromMacros(allMacros);

	}

	private String findDeviceTypeFromMacros(JSONArray hostmacros){
		String device_type = "";
		for (int z = 0; z < hostmacros.size(); z++) {
			// logger.debug(f.toString());
			// ZabbixAPIHost host = new ZabbixAPIHost();
			JSONObject hostmacro = hostmacros.getJSONObject(z);
			String name = hostmacro.getString("macro");
			if (name.equals("{$TYPE}")) {
				logger.debug("************* Found DeviceType hostmacro: " + name);
				device_type = hostmacro.getString("value");
				logger.debug("************* Found DeviceType hostmacro value: " + device_type);
				break ;
			}
			// JSONArray hostgroups = host.getJSONArray("groups");

			logger.debug("******** Received JSON hostmacro: " + hostmacro.toString());
		}

		return device_type;
	}


	private String[] checkItemForCi(String itemname, String hostname) {
		
		//logger.debug("*** Received Zabbix Item : " + itemname);
		
		// Example item as CI : 
		// [test CI item] bla-bla
		Pattern p = Pattern.compile("\\[(.*)\\](.*)");
		Matcher matcher = p.matcher(itemname);
		
		String ciid = "";
		//String hostnameend = "";
		
		// if Item has CI pattern
		if (matcher.matches()) {
			
			logger.debug("*** Finded Zabbix Item with Pattern as CI: " + itemname);
			// save as ne CI name
			itemname = matcher.group(1).toUpperCase();

		    // get SHA-1 hash for hostname-item block for saving as ciid
		    // Example:
		    // KRL-PHOBOSAU--PHOBOS:KRL-PHOBOSAU:TEST CI ITEM
		    logger.debug(String.format("*** Trying to generate hash for Item with Pattern: %s:%s", hostname, itemname));
		    String hash = "";
			try {
				hash = hashString(String.format("%s:%s", hostname, itemname), "SHA-1");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    logger.debug("*** Generated Hash: " + hash );
		    ciid = hash;
		    
			//event.setParametr(itemname);
		}
		// if Item has no CI pattern
		else {
			// TODO
			
		}
		
		String[] hostreturn = new String[] { "", "", "" } ;
		hostreturn[0] = ciid;
		hostreturn[1] = itemname;
		//hostreturn[1] = hostnameend;
		
		logger.debug("New Zabbix Host CI ID: " + hostreturn[0]);
		logger.debug("New Zabbix Host CI Name: " + hostreturn[1]);
		
		return hostreturn;
	}

	private String[] checkHostAliases(JSONArray hosts, String hostHost, String hostName) {

		// Example: KRL-PHOBOSAU--MSSQL
		String[] hostreturn = new String[] { "", "" } ;
		Pattern p = Pattern.compile("(.*)--(.*)");
		
		logger.debug("*** Check hostname for aliases: " + hostHost.toUpperCase());

		String hostnamebegin = "";
		String hostnameend = "";
		//String output = "";

		String host = checkHostPattern(hostHost,hostName);
		Matcher hostMatcher = p.matcher(host.toUpperCase());
		if (hostMatcher.matches()){
			hostnameend = hostMatcher.group(2).toUpperCase();
			hostnamebegin = hostMatcher.group(1).toUpperCase();
			logger.debug("*** hostnamebegin: " + hostnamebegin);
			logger.debug("*** hostnameend: " + hostnameend);
		}

		//else return
		//hostgroupsloop: 
		String ParentID = "";
		int j = 0;
		while (j < hosts.size()) {
			// logger.debug(f.toString());
			// ZabbixAPIHost host = new ZabbixAPIHost();
			JSONObject host_a = hosts.getJSONObject(j);
			String compareHost = host_a.getString("host");
			logger.debug("*** Compare with hosthost: " + compareHost);
			if (compareHost.equalsIgnoreCase(hostnamebegin)) {
				ParentID =  host_a.getString("hostid");
				break;
			}

			String compareName = host_a.getString("name");
			logger.debug("*** Compare with hostname: " + compareName);
			if (compareName.equalsIgnoreCase(hostnamebegin)) {
				ParentID =  host_a.getString("hostid");
				break;

			}
			j++;
		}

		hostreturn[0] = ParentID;
		hostreturn[1] = hostnameend;
		
		logger.info("New Zabbix Host ParentID: " + hostreturn[0]);
		logger.info("New Zabbix Host Name: " + hostreturn[1]);
		
		return hostreturn;
	}

	private void genErrorMessage(String message) {

		long timestamp = System.currentTimeMillis();
		timestamp = timestamp / 1000;
		String textError = "Возникла ошибка при работе адаптера: ";
		Event genevent = new Event();
		genevent.setMessage(textError + message);
		genevent.setEventCategory("ADAPTER");
		genevent.setSeverity(PersistentEventSeverity.CRITICAL.name());
		genevent.setTimestamp(timestamp);
		genevent.setEventsource(String.format("%s", endpoint.getConfiguration().getSource()));
		genevent.setStatus("OPEN");
		genevent.setHost("adapter");

		logger.info(" **** Create Exchange for Error Message container");
		Exchange exchange = getEndpoint().createExchange();
		exchange.getIn().setBody(genevent, Device.class);

		exchange.getIn().setHeader("EventIdAndStatus", "Error_" + timestamp);
		exchange.getIn().setHeader("Timestamp", timestamp);
		exchange.getIn().setHeader("queueName", "Events");
		exchange.getIn().setHeader("Type", "Error");

		try {
			getProcessor().process(exchange);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void genHeartbeatMessage(Exchange exchange) {
		// TODO Auto-generated method stub
		long timestamp = System.currentTimeMillis();
		timestamp = timestamp / 1000;
		// String textError = "Возникла ошибка при работе адаптера: ";
		Event genevent = new Event();
		genevent.setMessage("Сигнал HEARTBEAT от адаптера");
		genevent.setEventCategory("ADAPTER");
		genevent.setObject("HEARTBEAT");
		genevent.setSeverity(PersistentEventSeverity.OK.name());
		genevent.setTimestamp(timestamp);
		genevent.setEventsource(String.format("%s", endpoint.getConfiguration().getAdaptername()));

		logger.info(" **** Create Exchange for Heartbeat Message container");
		// Exchange exchange = getEndpoint().createExchange();
		exchange.getIn().setBody(genevent, Event.class);

		exchange.getIn().setHeader("Timestamp", timestamp);
		exchange.getIn().setHeader("queueName", "Heartbeats");
		exchange.getIn().setHeader("Type", "Heartbeats");
		exchange.getIn().setHeader("Source", endpoint.getConfiguration().getAdaptername());

		try {
			// Processor processor = getProcessor();
			// .process(exchange);
			// processor.process(exchange);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
	}

	private Device genHostObj(String hostname, String Id, String device_type, String newhostname, String parentID) {
		Device genDevice;
		genDevice = new Device();
		
		if (newhostname.equals("")) {
			genDevice.setName(hostname);
		}
		else {
			genDevice.setName(newhostname);
		}
		
		genDevice.setId(Id);
		genDevice.setDeviceType(device_type);
		genDevice.setHostName(hostname);
		genDevice.setParentID(parentID);

		// gendevice.setDeviceState(host.getStatus());
		// gendevice.setDeviceState(host.getOperationalStatus());

		genDevice.setSource(String.format("%s", endpoint.getConfiguration().getSource()));
		
		logger.debug("Received device_type: " + device_type);
		logger.debug(genDevice.toString());

		return genDevice;

	}
	
	private String getTransformedItemName(String name, String key) {

		//String transformedname = "";
		//String webstep = "";
		
		// get params from key to item $1 placeholder
		// Example:
		// vfs.fs.size[/oracle,pfree]
		
		Pattern p = Pattern.compile("(.*)\\[(.*)\\]");
		Matcher matcher = p.matcher(key);
		
		String keyparams = "";
		//String webscenario = "";
		//String webstep = "";
		
		String[] params = new String[] { } ;
		
		// if Web Item has webscenario pattern
		// Example:
		// web.test.in[WebScenario,,bps]
		if (matcher.matches()) {
			
			logger.debug("*** Finded Zabbix Item key with Pattern: " + key);
			// save as ne CI name
			keyparams = matcher.group(2);
			
			// get scenario and step from key params
			//String[] params = new String[] { } ;
			params = keyparams.split(",");
			logger.debug(String.format("*** Finded Zabbix Item key params (size): %d ", params.length));
						
			//logger.debug(String.format("*** Finded Zabbix Item key params: %s:%s ", webscenario, webstep));


		}
		// if Item has no CI pattern
		else {
			
			
		}
		
		logger.debug("Item name: " + name);
		
		String param = "";
		int paramnumber;
		Matcher m = Pattern.compile("\\$\\d+").matcher(name);
		while (m.find()) {
			param = m.group(0);
			paramnumber = Integer.parseInt(param.substring(1));
			logger.debug("Found Param: " + paramnumber);
			logger.debug("Found Param Value: " + param);
			logger.debug("Found Param Value Replace: " + params[paramnumber-1]);
			
			name = name.replaceAll("\\$"+paramnumber, params[paramnumber-1]);

		}

		
		//logger.debug("New Zabbix Web Item Scenario: " + webelements[0]);
		logger.debug("New Zabbix Item Name: " + name);
		
		return name;

	}

	private Device genHostgroupObj(JSONObject hostgroup, String device_type, String newhostname) {
		
		Device gendevice;
		gendevice = new Device();
		
		String hostgroupName = hostgroup.getString("name");
		/*
		if (newhostname.equals("")) {
			gendevice.setName(hostgroupName);
		}
		else {
			gendevice.setName(newhostname);
		}
		*/
		
		// zabbix_group_ci_pattern=\\((.*)\\)(.*)
		String pattern = endpoint.getConfiguration().getGroupCiPattern();
		logger.debug("*** Zabbix Group Pattern: " + pattern);
		
		String newHostgroupName = ""; 
		String service = "";
		
		// Example item as CI : 
		// (Невский.СЭ)ТЭЦ-1
		Pattern p = Pattern.compile(pattern);
		Matcher matcher = p.matcher(hostgroupName);
		
		//String hostnameend = "";
		
		// if Item has CI pattern
		if (matcher.matches()) {
			logger.debug("*** Finded Zabbix Group with Pattern: " + hostgroupName);
			
			newHostgroupName = matcher.group(2);
			service = matcher.group(1);

		    logger.debug("*** newHostgroupName: " + newHostgroupName );
		    logger.debug("*** service: " + service );
			
		}
		
		// use parent host as CI for item
		else {
			newHostgroupName = hostgroupName;
		}
		
		gendevice.setId(hostgroup.getString("groupid"));
		gendevice.setDeviceType(device_type);
		gendevice.setService(service);
		gendevice.setName(newHostgroupName);

		gendevice.setSource(String.format("%s", endpoint.getConfiguration().getSource()));
		
		logger.debug("Received device_type: " + device_type);
		logger.debug(gendevice.toString());

		return gendevice;

	}

	/**
	 * @param message erer
	 * @param algorithm dfdfd
	 * @return SHA-1 hash String
	 * @throws Exception
	 */
	private static String hashString(String message, String algorithm)
            throws Exception {
 
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashedBytes = digest.digest(message.getBytes("UTF-8"));
 
            return convertByteArrayToHexString(hashedBytes);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException ex) {
            throw new RuntimeException(
                    "Could not generate hash from String", ex);
        }
	}
	
	private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuffer stringBuffer = new StringBuffer();
		for (byte arrayByte : arrayBytes) {
			stringBuffer.append(Integer.toString((arrayByte & 0xff) + 0x100, 16)
					.substring(1));
		}

        return stringBuffer.toString();
    }

}