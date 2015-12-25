package ru.atc.camel.zabbix.devices;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

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

	private int processSearchDevices() throws ClientProtocolException, IOException, Exception {

		// Long timestamp;

		List<Device> hostsList = new ArrayList<Device>();
		List<Device> hostgroupsList = new ArrayList<Device>();
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
			System.err.println("login:" + login);

		
			// Get all Hosts from Zabbix
			hostsList = getAllHosts(zabbixApi);
			if (hostsList != null)
				listFinal.addAll(hostsList);
			
			// Get all HostGroups from Zabbix
			hostgroupsList = getAllHostGroups(zabbixApi);
			if (hostgroupsList != null)
				listFinal.addAll(hostgroupsList);
			
		
			for (int i = 0; i < listFinal.size(); i++) {
				logger.info("Create Exchange container");
				Exchange exchange = getEndpoint().createExchange();
				exchange.getIn().setBody(listFinal.get(i), Device.class);
				exchange.getIn().setHeader("DeviceId", listFinal.get(i).getId());
				exchange.getIn().setHeader("DeviceType", listFinal.get(i).getDeviceType());
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
		} catch (Error e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(String.format("Error while get Devices from API: %s ", e));
			genErrorMessage(e.getMessage() + " " + e.toString());
			httpClient.close();
			zabbixApi.destory();
			return 0;
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error(String.format("Error while get Devices from API: %s ", e));
			genErrorMessage(e.getMessage() + " " + e.toString());
			httpClient.close();
			zabbixApi.destory();
			return 0;
		} finally {
			logger.debug(String.format(" **** Close zabbixApi Client: %s", zabbixApi.toString()));
			// httpClient.close();
			zabbixApi.destory();
			// dataSource.close();
			// return 0;
		}

		return 1;
	}

	private List<Device> getAllHostGroups(DefaultZabbixApi zabbixApi) {
		// TODO Auto-generated method stub
		return null;
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
					.paramEntry("selectParentTemplates", new String[] { "templateeid", "host", "name" })
					.paramEntry("selectItems", new String[] { "itemid", "name", "key_", "description" }).build();

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed create JSON request for get all Hosts.");
		}

		// logger.info(" *** Get All host JSON params: " + params.toString());

		// JsonObject json = null;
		JSONArray hosts;
		try {
			getResponse = zabbixApi.call(getRequest);
			System.err.println(getResponse);

			hosts = getResponse.getJSONArray("result");
			System.err.println(hosts);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException("Failed get JSON response result for all Hosts.");
		}
		List<Device> deviceList = new ArrayList<Device>();
		
		List<Device> listFinal = new ArrayList<Device>();
		//List<Device> listFinal = new ArrayList<Device>();
		String device_type = "host";
		String ParentID = "";
		String newhostname = "";
		logger.info("Finded Zabbix Hosts: " + hosts.size());

		for (int i = 0; i < hosts.size(); i++) {
			device_type = "host";
			ParentID = "";
			newhostname = "";
			// logger.debug(f.toString());
			// ZabbixAPIHost host = new ZabbixAPIHost();
			JSONObject host = hosts.getJSONObject(i);
			String hostname = host.getString("host");
			// Example: KRL-PHOBOSAU--MSSQL
			if (hostname.matches("(.*)--(.*)")){
				
				logger.info("Finded Zabbix Host with Aliases: " + hostname);
				String[] checkreturn = checkHostAliases(hosts, hostname);
				ParentID = checkreturn[0];
				newhostname = checkreturn[1];
			}
			
			JSONArray hostgroups = host.getJSONArray("groups");
			JSONArray hosttemplates = host.getJSONArray("parentTemplates");
			JSONArray hostitems = host.getJSONArray("items");
			JSONArray hostmacros = host.getJSONArray("macros");

			logger.debug("*** Received JSON Host: " + host.toString());
			
			// if ParentID is not set in host name
			// Example: KRL-PHOBOSAU--MSSQL
			if (ParentID.equals("")){
				hostgroupsloop:
					for (int j = 0; j < hostgroups.size(); j++) {
						// logger.debug(f.toString());
						// ZabbixAPIHost host = new ZabbixAPIHost();
						JSONObject hostgroup = hostgroups.getJSONObject(j);
						String name = hostgroup.getString("name");
						if (name.startsWith("[")) {
							logger.debug("************* Found ParentGroup hostgroup: " + name);
							ParentID = hostgroup.getString("groupid").toString();
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

			for (int y = 0; y < hostitems.size(); y++) {
				// logger.debug(f.toString());
				// ZabbixAPIHost host = new ZabbixAPIHost();
				JSONObject hostitem = hostitems.getJSONObject(y);
				// JSONArray hostgroups = host.getJSONArray("groups");

				//logger.debug("******** Received JSON hostitem: " + hostitem.toString());
			}
			
			hostmacrosloop:
			for (int z = 0; z < hostmacros.size(); z++) {
				// logger.debug(f.toString());
				// ZabbixAPIHost host = new ZabbixAPIHost();
				JSONObject hostmacro = hostmacros.getJSONObject(z);
				String name = hostmacro.getString("macro");
				if (name.equals("{$TYPE}")) {
					logger.debug("************* Found DeviceType hostmacro: " + name);
					device_type = hostmacro.getString("value");
					logger.debug("************* Found DeviceType hostmacro value: " + device_type);
					break hostmacrosloop;
				}
				// JSONArray hostgroups = host.getJSONArray("groups");

				logger.debug("******** Received JSON hostmacro: " + hostmacro.toString());
			}

			Device gendevice = new Device();
			gendevice = genHostObj(host, device_type, newhostname, ParentID);
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

	private String[] checkHostAliases(JSONArray hosts, String hostname) {
		// TODO Auto-generated method stub
		// Example: KRL-PHOBOSAU--MSSQL
		String[] hostreturn = new String[] { "", "" } ;
		Pattern p = Pattern.compile("(.*)--(.*)");
		Matcher matcher = p.matcher(hostname.toUpperCase());
		String hostnamebegin = "";
		String hostnameend = "";
		//String output = "";
		if (matcher.matches()){
			hostnameend = matcher.group(2).toString().toUpperCase();
			hostnamebegin = matcher.group(1).toString().toUpperCase();
		}
		//else return 
		//hostgroupsloop: 
		String ParentID = "";
		for (int j = 0; j < hosts.size(); j++) {
			// logger.debug(f.toString());
			// ZabbixAPIHost host = new ZabbixAPIHost();
			JSONObject host_a = hosts.getJSONObject(j);
			String name = host_a.getString("name");
			if (name.equalsIgnoreCase(hostnamebegin)) {
				ParentID =  host_a.getString("hostid");
				
			}
		}
		
		hostreturn[0] = ParentID;
		hostreturn[1] = hostnameend;
		
		logger.info("New Zabbix Host ParentID: " + hostreturn[0]);
		logger.info("New Zabbix Host Name: " + hostreturn[1]);
		
		return hostreturn;
	}

	private void genErrorMessage(String message) {
		// TODO Auto-generated method stub
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

	private Device genHostObj(JSONObject host, String device_type, String newhostname, String parentID) {
		Device gendevice = null;
		gendevice = new Device();
		
		if (newhostname.equals("")) {
			gendevice.setName(host.getString("host"));
		}
		else {
			gendevice.setName(newhostname);
		}
		
		gendevice.setId(host.getString("hostid"));
		gendevice.setDeviceType(device_type);
		gendevice.setParentID(parentID);

		// gendevice.setDeviceState(host.getStatus());
		// gendevice.setDeviceState(host.getOperationalStatus());

		gendevice.setSource(String.format("%s", endpoint.getConfiguration().getSource()));
		
		logger.debug("Received device_type: " + device_type);
		logger.debug(gendevice.toString());

		return gendevice;

	}

	private CloseableHttpClient HTTPinit(RequestConfig globalConfig, CookieStore cookieStore) {

		SSLContext sslContext = null;
		// HttpClient client = HttpClientBuilder.create().build();
		HttpClientBuilder cb = HttpClientBuilder.create();
		SSLContextBuilder sslcb = new SSLContextBuilder();
		try {
			sslcb.loadTrustMaterial(KeyStore.getInstance(KeyStore.getDefaultType()), new TrustSelfSignedStrategy());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			cb.setSslcontext(sslcb.build());
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			sslContext = sslcb.build();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		@SuppressWarnings("deprecation")
		// RequestConfig globalConfig =
		// RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
		// CookieStore cookieStore = new BasicCookieStore();
		// HttpClientContext context = HttpClientContext.create();
		// context.setCookieStore(cookieStore);
		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext,
				SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		CloseableHttpClient httpclient = HttpClients.custom().setUserAgent("Mozilla/5.0")
				.setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).setSSLSocketFactory(sslsf)
				.build();

		// logger.debug("*** Received cookies: " +
		// context.getCookieStore().getCookies());

		return httpclient;
	}

	/*
	 * private int processSearchFeeds() throws Exception {
	 * 
	 * String query = endpoint.getConfiguration().getQuery(); String uri =
	 * String.format("login?query=%s", query); JsonObject json =
	 * performGetRequest(uri);
	 * 
	 * //JsonArray feeds = (JsonArray) json.get("results"); JsonArray feeds =
	 * (JsonArray) json.get("ServerName"); List<Feed2> feedList = new
	 * ArrayList<Feed2>(); Gson gson = new
	 * GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create(); for
	 * (JsonElement f : feeds) { //logger.debug(gson.toJson(i)); Feed2 feed =
	 * gson.fromJson(f, Feed2.class); feedList.add(feed); }
	 * 
	 * Exchange exchange = getEndpoint().createExchange();
	 * exchange.getIn().setBody(feedList, ArrayList.class);
	 * getProcessor().process(exchange);
	 * 
	 * return 1; }
	 */

}