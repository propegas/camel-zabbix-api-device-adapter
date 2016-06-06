package ru.atc.camel.zabbix.devices;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.ScheduledPollConsumer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.at_consulting.itsm.device.Device;
import ru.at_consulting.itsm.event.Event;
import ru.atc.monitoring.zabbix.api.DefaultZabbixApi;
import ru.atc.monitoring.zabbix.api.Request;
import ru.atc.monitoring.zabbix.api.RequestBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.atc.zabbix.general.CiItems.checkHostAliases;
import static ru.atc.zabbix.general.CiItems.checkHostPattern;
import static ru.atc.zabbix.general.CiItems.checkItemForCi;
import static ru.atc.zabbix.general.CiItems.getTransformedItemName;
//import io.github.hengyunabc.zabbix.RequestBuilder;
//import io.github.hengyunabc.zabbix.api.DefaultZabbixApi;

public class ZabbixAPIConsumer extends ScheduledPollConsumer {

    private static final int CONNECT_TIMEOUT = 10000;
    private static final int SOCKET_TIMEOUT = 120000;
    private static final int MAX_CONN_PER_ROUTE = 40;
    private static final int MAX_CONN_TOTAL = 40;
    private static final int CONNECTION_TIME_TO_LIVE = 120;

    private static final Logger logger = LoggerFactory.getLogger("mainLogger");
    private static final Logger loggerErrors = LoggerFactory.getLogger("errorsLogger");

    private ZabbixAPIEndpoint endpoint;

    public ZabbixAPIConsumer(ZabbixAPIEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.endpoint = endpoint;
        this.setTimeUnit(TimeUnit.MINUTES);
        this.setInitialDelay(0);
        logger.info("This: " + this);
        logger.info("Endpoint: " + endpoint);
        logger.info("Set delay: " + endpoint.getConfiguration().getDelay());
        this.setDelay(endpoint.getConfiguration().getDelay());
    }

    public static void genHeartbeatMessage(Exchange exchange, String source) {

        long timestamp = System.currentTimeMillis();
        timestamp = timestamp / 1000;
        Event genevent = new Event();
        genevent.setMessage("Сигнал HEARTBEAT от адаптера");
        genevent.setEventCategory("ADAPTER");
        genevent.setObject("HEARTBEAT");
        genevent.setSeverity(PersistentEventSeverity.OK.name());
        genevent.setTimestamp(timestamp);

        genevent.setEventsource(String.format("%s", source));

        logger.info(" **** Create Exchange for Heartbeat Message container");

        exchange.getIn().setBody(genevent, Event.class);

        exchange.getIn().setHeader("Timestamp", timestamp);
        exchange.getIn().setHeader("queueName", "Heartbeats");
        exchange.getIn().setHeader("Type", "Heartbeats");

        exchange.getIn().setHeader("Source", source);

    }

    @Override
    protected int poll() throws Exception {

        logger.info("This: " + this);
        logger.info("Endpoint: " + this.endpoint);
        String operationPath = this.endpoint.getOperationPath();

        if ("devices".equals(operationPath)) {
            logger.info("'All' option");
            try {
                return processSearchDevices("all");
            } catch (Exception e) {
                logger.error("Error while process all devices", e);
            }
        } else if ("vmdevices".equals(operationPath)) {
            logger.info("'VM' option");
            try {
                return processSearchDevices("vm");
            } catch (Exception e) {
                logger.error("Error while process all VM", e);
            }
        }

        // only one operation implemented for now !
        throw new IllegalArgumentException("Incorrect operation: " + operationPath);
    }

    private String[] getVmwareTemplatesId(DefaultZabbixApi zabbixApi) {

        String vmwareSearchPattern = endpoint.getConfiguration().getZabbixDevicesVMwareTemplatePattern();

        Request getRequest;
        JSONObject getResponse;

        logger.info("*** Try to get VMware Devices from API... ");

        try {
            JSONObject search = new JSONObject();

            search.put("name", new String[]{vmwareSearchPattern});

            getRequest = RequestBuilder.newBuilder().method("template.get")
                    .paramEntry("search", search)
                    .paramEntry("output", new String[]{"templateid", "name"})
                    .paramEntry("searchWildcardsEnabled", 1)
                    .build();

        } catch (Exception ex) {
            logger.error("Failed create JSON request for get VMware Templates", ex);
            throw new RuntimeException("Failed create JSON request for get VMware Templates");
        }

        JSONArray templates;
        try {
            getResponse = zabbixApi.call(getRequest);
            logger.info("*** getRequest: " + getRequest);
            logger.info("*** getResponse: " + getResponse);

            templates = getResponse.getJSONArray("result");

        } catch (Exception e) {
            logger.error("Failed get JSON response result for VMware Templates.", e);
            throw new RuntimeException("Failed get JSON response result for VMware Templates.");
        }

        logger.info(String.format("*** Received VMware Templates from API: %d", templates.size()));

        String[] templateids = new String[]{};

        for (int i = 0; i < templates.size(); i++) {
            JSONObject template = templates.getJSONObject(i);
            String templateid = template.getString("templateid");

            templateids = (String[]) ArrayUtils.add(templateids, templateid);

            logger.debug("*** Received JSON VMware Template ID: " + templateid);
        }

        logger.debug("*** templateids: " + Arrays.toString(templateids));

        return templateids;
    }

    @Override
    public long beforePoll(long timeout) throws Exception {

        logger.info("*** Before Poll!!!");

        // send HEARTBEAT
        genHeartbeatMessage(getEndpoint().createExchange(), endpoint.getConfiguration().getAdaptername());

        return timeout;
    }

    private int processSearchDevices(String deviceType) {

        // Long timestamp;

        List<Device> listFinal;

        String eventsuri = endpoint.getConfiguration().getZabbixapiurl();
        String uri = String.format("%s", eventsuri);

        System.out.println("***************** URL: " + uri);

        logger.info("Try to get Hosts.");

        HttpClient httpClient2 = HttpClients.custom()
                .setConnectionTimeToLive(CONNECTION_TIME_TO_LIVE, TimeUnit.SECONDS)
                .setMaxConnTotal(MAX_CONN_TOTAL).setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout(CONNECT_TIMEOUT).build())
                .setRetryHandler(new DefaultHttpRequestRetryHandler(5, true))
                .build();

        DefaultZabbixApi zabbixApi = null;
        try {
            String zabbixapiurl = endpoint.getConfiguration().getZabbixapiurl();
            String username = endpoint.getConfiguration().getUsername();
            String password = endpoint.getConfiguration().getPassword();

            logger.debug("zabbixapiurl: " + zabbixapiurl);
            logger.debug("username: " + username);
            logger.debug("password: " + password);
            zabbixApi = new DefaultZabbixApi(zabbixapiurl, (CloseableHttpClient) httpClient2);
            zabbixApi.init();

            boolean login = zabbixApi.login(username, password);
            if (!login) {

                throw new RuntimeException("Failed to login to Zabbix API.");
            }

            listFinal = getAllDevicesByDeviceType(deviceType, zabbixApi);

            for (Device aListFinal : listFinal) {
                logger.info("Create Exchange container");

                String deviceUniqueHash = String.format("%s//%s//%s//%s//%s",
                        aListFinal.getId(),
                        aListFinal.getParentID(),
                        aListFinal.getDeviceType(),
                        aListFinal.getName(),
                        aListFinal.getHostName());

                Exchange exchange = getEndpoint().createExchange();
                exchange.getIn().setBody(aListFinal, Device.class);
                exchange.getIn().setHeader("DeviceId", aListFinal.getId());
                exchange.getIn().setHeader("ParentId", aListFinal.getParentID());
                exchange.getIn().setHeader("DeviceType", aListFinal.getDeviceType());
                exchange.getIn().setHeader("queueName", "Devices");
                exchange.getIn().setHeader("DeviceUniqueHash", deviceUniqueHash);

                try {
                    getProcessor().process(exchange);
                } catch (Exception e) {
                    logger.error("Error while process exchange", e);
                }
            }

            logger.info("Sended Devices: " + listFinal.size());

        } catch (NullPointerException e) {
            logger.error("Error while get Devices from API ", e);
            loggerErrors.error("Error while get Devices from API ", e);
            genErrorMessage(e.getMessage() + " " + e.toString());
            return 0;
        } catch (Exception e) {
            logger.error("Error while get Devices from API: %s ", e);
            loggerErrors.error("Error while get Devices from API ", e);
            genErrorMessage(e.getMessage() + " " + e.toString());
            return 0;
        } finally {
            logger.debug(String.format(" **** Close zabbixApi Client: %s",
                    zabbixApi != null ? zabbixApi.toString() : null));

            if (zabbixApi != null) {
                zabbixApi.destory();
            }

        }

        return 1;
    }

    private List<Device> getAllDevicesByDeviceType(String deviceType, DefaultZabbixApi zabbixApi) {
        List<Device> hostsList;
        List<Device> hostgroupsList;
        List<Device> itemsList;
        List<Device> listFinal = new ArrayList<>();
        if ("all".equals(deviceType)) {
            logger.info("Try to get all Devices...");
            // Get all Hosts from Zabbix
            hostsList = getAllHosts(zabbixApi, null);
            if (hostsList != null)
                listFinal.addAll(hostsList);

            // Get all HostGroups from Zabbix
            hostgroupsList = getAllHostGroups(zabbixApi);
            if (hostgroupsList != null)
                listFinal.addAll(hostgroupsList);

            // Get all Items marked as CI from Zabbix
            itemsList = getAllCiItems(zabbixApi);

            if (itemsList != null)
                listFinal.addAll(itemsList);

        } else if ("vm".equals(deviceType)) {
            logger.info("Try to get VM only Devices...");
            // Get VMware Devices
            String[] vmwareTemplatesIds = getVmwareTemplatesId(zabbixApi);
            logger.debug("vmwareTemplatesIds: " + Arrays.toString(vmwareTemplatesIds));
            logger.debug("vmwareTemplatesIds size: " + vmwareTemplatesIds.length);

            // Get all VM Hosts from Zabbix
            if (0 != vmwareTemplatesIds.length) {
                hostsList = getAllHosts(zabbixApi, vmwareTemplatesIds);
                if (hostsList != null)
                    listFinal.addAll(hostsList);
            }
        }
        return listFinal;
    }

    private List<Device> getAllCiItems(DefaultZabbixApi zabbixApi) {

        // zabbix_item_ke_search_pattern=[*]*
        String itemCiSearchPattern = endpoint.getConfiguration().getItemCiSearchPattern();

        logger.debug("*** Try to get Item CI using Pattern: " + itemCiSearchPattern);

        Request getRequest;
        JSONObject getResponse;

        try {
            JSONObject search = new JSONObject();

            search.put("name", new String[]{itemCiSearchPattern});

            getRequest = RequestBuilder.newBuilder().method("item.get")
                    .paramEntry("search", search)
                    .paramEntry("output", new String[]{"itemid", "name", "key_", "description"})
                    .paramEntry("monitored", true)
                    .paramEntry("searchWildcardsEnabled", true)
                    .paramEntry("selectHosts", new String[]{"name", "host", "hostid"})
                    .build();

        } catch (Exception ex) {
            logger.error("Failed create JSON request for get all CI Items.", ex);
            throw new RuntimeException("Failed create JSON request for get all CI Items.");
        }

        JSONArray hostitems;
        try {
            getResponse = zabbixApi.call(getRequest);
            hostitems = getResponse.getJSONArray("result");

        } catch (Exception e) {
            logger.error("Failed get JSON response result for all CI Items.", e);
            throw new RuntimeException("Failed get JSON response result for all CI Items.");
        }
        return getDevicesFromZabbixItems(hostitems);

    }

    List<Device> getDevicesFromZabbixItems(JSONArray hostitems) {
        List<Device> deviceList = new ArrayList<>();

        List<Device> generatedDevicesList = new ArrayList<>();

        String deviceType;
        String newCiName;
        logger.info("Finded Zabbix CI Items : " + hostitems.size());

        HashMap<String, Object> finalDevicesHashMap = new HashMap<>();

        for (int y = 0; y < hostitems.size(); y++) {

            // reset type fro every item
            deviceType = "item";
            JSONObject item = hostitems.getJSONObject(y);

            JSONArray host = item.getJSONArray("hosts");

            String hostHost = host.getJSONObject(0).getString("host");
            String hostName = host.getJSONObject(0).getString("name");

            // check hostHost or hostName for aliases pattern host--alias
            String ciHostAliasName = checkHostPattern(hostHost, hostName);
            // if host has no ci aliases pattern use default hostHost
            if (ciHostAliasName == null) {
                ciHostAliasName = hostHost;
            }

            String hostid = host.getJSONObject(0).getString("hostid");
            String name = item.get("name").toString();
            String key = item.get("key_").toString();

            // parse $1, $2 etc params in item name from key_ params
            if (name.matches(".*\\$\\d+.*")) {
                name = getTransformedItemName(name, key);
            }

            // get hash (ciid) and parsed name for CI item
            String[] returnCiArray = checkItemForCi(name, hostid, ciHostAliasName,
                    this.endpoint.getConfiguration().getItemCiPattern(),
                    this.endpoint.getConfiguration().getItemCiParentPattern(),
                    this.endpoint.getConfiguration().getItemCiTypePattern());
            if (!returnCiArray[0].isEmpty()) {
                String ciid = returnCiArray[0];
                newCiName = returnCiArray[1];
                //devicetype
                if (!"".equals(returnCiArray[2]))
                    deviceType = returnCiArray[2];
                //parentid
                if (!"".equals(returnCiArray[3]))
                    hostid = returnCiArray[3];

                // add pseudo-CI (as a group) for devicetype
                // String hostname, String Id, String deviceType, String Name, String parentID
                if (!"".equals(deviceType) && !"".equals(hostid)) {

                    // create link of CiGroup to host or item as a parent
                    String pseudoCiId = String.format("%s.%s", hostid, deviceType);

                    String[] deviceArray = new String[]{ciHostAliasName, pseudoCiId, "CiGroup", deviceType, hostid,
                            String.format("%s:%s", hostName, deviceType)};
                    logger.debug(String.format("*** Add Zabbix Pseudo CI (CiGroup) to HASH : %s %s",
                            deviceArray[0], pseudoCiId));
                    finalDevicesHashMap.put(pseudoCiId, deviceArray);

                    // re-write link of CI to new CiGroup as a parent
                    hostid = pseudoCiId;

                }

                // add device as an array to hash-map to exclude duplicates of ci-items
                // using ciid as a key
                // String hostname, String Id, String deviceType, String newhostname, String parentID
                String[] deviceArray = new String[]{ciHostAliasName, ciid, deviceType, newCiName, hostid,
                        String.format("%s:%s", hostName, newCiName)};
                logger.debug(String.format("*** Add Zabbix CI Item to HASH : %s %s",
                        deviceArray[0], ciid));
                finalDevicesHashMap.put(ciid, deviceArray);

            }

        }

        for (Entry<String, Object> deviceHashEntry : finalDevicesHashMap.entrySet()) {
            logger.debug(deviceHashEntry.getKey() + " = " + deviceHashEntry.getValue());

            String[] deviceArray = (String[]) deviceHashEntry.getValue();

            // generate Device json message
            Device generatedDevice;
            generatedDevice = genHostObj(deviceArray[0], deviceArray[1], deviceArray[2],
                    deviceArray[3], deviceArray[4], "", deviceArray[5]);
            logger.debug("*** NEW DEVICE: ", generatedDevice.toString());
            deviceList.add(generatedDevice);
        }

        generatedDevicesList.addAll(deviceList);

        return generatedDevicesList;
    }

    private List<Device> getAllHostGroups(DefaultZabbixApi zabbixApi) {

        // zabbix_group_search_pattern=(*)*
        String groupSearchPattern = endpoint.getConfiguration().getGroupSearchPattern();

        Request getRequest;
        JSONObject getResponse;
        // JsonObject params = new JsonObject();

        logger.debug("*** Try to get Zabbix Groups using Pattern: " + groupSearchPattern);

        try {

            JSONObject search = new JSONObject();
            search.put("name", new String[]{groupSearchPattern});

            getRequest = RequestBuilder.newBuilder().method("hostgroup.get")
                    .paramEntry("search", search)
                    .paramEntry("output", "extend")
                    .paramEntry("searchWildcardsEnabled", true)
                    .build();

        } catch (Exception ex) {
            logger.error("Failed create JSON request for get all Groups.", ex);
            loggerErrors.error("Failed create JSON request for get all Groups.", ex);
            throw new RuntimeException("Failed create JSON request for get all Groups.");
        }

        JSONArray hostgroups;
        try {
            getResponse = zabbixApi.call(getRequest);
            logger.debug("Zabbix Groups getRequest: " + getRequest);
            logger.debug("Zabbix Groups getResponse: " + getResponse);

            hostgroups = getResponse.getJSONArray("result");

        } catch (Exception e) {
            logger.error("Failed get JSON response result for all Hosts.", e);
            loggerErrors.error("Failed get JSON response result for all Hosts.", e);
            throw new RuntimeException("Failed get JSON response result for all Hosts.");
        }
        List<Device> deviceList = new ArrayList<>();

        // set "NodeGroup" type for groups
        String deviceType = "NodeGroup";

        logger.info("Finded Zabbix Groups: " + hostgroups.size());

        for (int i = 0; i < hostgroups.size(); i++) {
            JSONObject hostgroup = hostgroups.getJSONObject(i);
            // Example: KRL-PHOBOSAU--MSSQL

            logger.debug("*** Received JSON Group: " + hostgroup.toString());

            Device gendevice;
            gendevice = genHostgroupObj(hostgroup, deviceType);
            deviceList.add(gendevice);
        }

        return deviceList;
    }

    private List<Device> getAllHosts(DefaultZabbixApi zabbixApi, String[] templateIds) {

        RequestBuilder getRequestBuilder;
        Request getRequest;
        JSONObject getResponse;
        try {

            getRequestBuilder = RequestBuilder.newBuilder().method("host.get")
                    // .paramEntry("filter", filter)
                    .paramEntry("output", new String[]{"hostid", "name", "host"})
                    .paramEntry("selectMacros", new String[]{"hostmacroid", "macro", "value"})
                    .paramEntry("selectGroups", "extend")
                    .paramEntry("selectParentTemplates", new String[]{"templateid", "host", "name"});

            if (templateIds != null && 0 != templateIds.length) {
                logger.info("This: " + this);
                logger.info("Using templateids filter...");
                getRequestBuilder = getRequestBuilder.paramEntry("templateids", templateIds);
            } else {
                logger.info("This: " + this);
                logger.info("Don't use templateids filter...");
            }

            getRequest = getRequestBuilder.build();

        } catch (Exception ex) {
            loggerErrors.error("Failed create JSON request for get all Hosts.", ex);
            throw new RuntimeException("Failed create JSON request for get all Hosts.");
        }

        JSONArray hosts;
        try {
            getResponse = zabbixApi.call(getRequest);

            hosts = getResponse.getJSONArray("result");

        } catch (Exception e) {
            logger.error("Failed get JSON response result for all Hosts.", e);
            loggerErrors.error("Failed get JSON response result for all Hosts.", e);
            throw new RuntimeException("Failed get JSON response result for all Hosts.");
        }
        return getDevicesFromZabbixHosts(zabbixApi, hosts);
    }

    protected List<Device> getDevicesFromZabbixHosts(DefaultZabbixApi zabbixApi, JSONArray hosts) {
        List<Device> deviceList = new ArrayList<>();

        List<Device> listFinal = new ArrayList<>();
        String deviceType;
        String parentID;
        String deviceName;

        logger.info("Finded Zabbix Hosts: " + hosts.size());
        logger.info("*** Generating Devices... ");
        for (int i = 0; i < hosts.size(); i++) {

            parentID = "";
            String service = "";

            JSONObject host = hosts.getJSONObject(i);
            String hostOriginHost = host.getString("host");
            String hostNameVisible = host.getString("name");
            deviceName = hostNameVisible;

            String hostid = host.getString("hostid");
            String hostNameBeginPart = "";
            String hostNameEndPart = "";
            // Example: KRL-PHOBOSAU--MSSQL
            if (hostOriginHost.matches("(.*)--(.*)") || hostNameVisible.matches("(.*)--(.*)")) {

                logger.debug(String.format("Finded Zabbix Host with Aliases: %s (%s)", hostOriginHost, hostNameVisible));
                String[] checkreturn = checkHostAliases(hosts, hostOriginHost, hostNameVisible);
                parentID = checkreturn[0];

                // add first part of host to name of CI
                deviceName = String.format("%s:%s", checkreturn[2], checkreturn[1]);
                hostNameBeginPart = checkreturn[2];
                hostNameEndPart = checkreturn[1];
            }

            JSONArray hostgroups = host.getJSONArray("groups");
            JSONArray hosttemplates = host.getJSONArray("parentTemplates");
            JSONArray hostmacros = host.getJSONArray("macros");

            try {
                logger.debug("*** Received JSON Host: " + host.toString());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Error while print JSON host.");
            }
            HostsAndGroups hostsAndGroups = new HostsAndGroups(parentID, service, hostgroups).invoke();
            parentID = hostsAndGroups.getParentID();
            service = hostsAndGroups.getService();

            boolean isVm = false;
            for (int x = 0; x < hosttemplates.size(); x++) {
                JSONObject hostTemplate = hosttemplates.getJSONObject(x);

                logger.debug("******** Received JSON Hosttemplate: " + hostTemplate.toString());

                if (hostTemplate.get("name").toString().contains("--VMware Guest--"))
                    isVm = true;
            }

            // try to receive host type-macros
            logger.debug("******** Check Host's type-macros ");
            deviceType = findDeviceTypeFromMacros(hostmacros);

            logger.debug("******** 1 deviceType: " + deviceType);

            // try to receive host's templates type-macros
            if ("".equals(deviceType) && zabbixApi != null) {
                logger.debug("******** Check Template's type-macros ");
                deviceType = getMacrosFromTemplates(zabbixApi, hosttemplates);
            }

            logger.debug("******** 2 deviceType: " + deviceType);

            // set default type
            if ("".equals(deviceType)) {
                deviceType = "host";
            }

            logger.debug("******** 3 deviceType: " + deviceType);

            Device genDevice;
            String newDeviceHostName;
            // set default type
            // 172.20.22.115--Tgc1-proxy-mgr
            if ("vm".equals(deviceType) || isVm) {
                newDeviceHostName = hostNameEndPart;
                deviceName = hostNameEndPart;
            } else {
                newDeviceHostName = hostNameBeginPart;
            }
            genDevice = genHostObj(newDeviceHostName, hostid, deviceType,
                    deviceName, parentID, service, hostNameVisible);
            deviceList.add(genDevice);

        }

        logger.info("*** Generated successfully: " + listFinal.size());
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

            for (int i = 0; i < arraySize; i++) {
                stringArray[i] = hosttemplates.getJSONObject(i).getString("templateid");
            }
            getRequest = RequestBuilder.newBuilder().method("template.get")
                    .paramEntry("templateids", stringArray)
                    //.paramEntry("output", new String[] { "hostid", "name", "host" })
                    .paramEntry("selectMacros", new String[]{"hostmacroid", "macro", "value"})
                    //.paramEntry("selectItems", new String[] { "itemid", "name", "key_", "description" })
                    .build();

        } catch (Exception ex) {
            logger.error("Failed create JSON request for get Template's macros.", ex);
            loggerErrors.error("Failed create JSON request for get Template's macros.", ex);
            throw new RuntimeException("Failed create JSON request for get Template's macros.");
        }

        JSONArray templates;
        try {
            getResponse = zabbixApi.call(getRequest);
            logger.debug("******** Template macros getRequest:  " + getRequest);
            logger.debug("******** Template macros getResponse:  " + getResponse);
            //System.err.println(getResponse);

            templates = getResponse.getJSONArray("result");
            //System.err.println(hosts);

        } catch (Exception e) {
            logger.error("Failed get JSON response result for get Template's macros.", e);
            loggerErrors.error("Failed get JSON response result for get Template's macros.", e);
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

    private String findDeviceTypeFromMacros(JSONArray hostmacros) {
        String deviceType = "";
        for (int z = 0; z < hostmacros.size(); z++) {

            JSONObject hostmacro = hostmacros.getJSONObject(z);
            String name = hostmacro.getString("macro");
            if ("{$TYPE}".equals(name)) {
                logger.debug("************* Found DeviceType hostmacro: " + name);
                deviceType = hostmacro.getString("value");
                logger.debug("************* Found DeviceType hostmacro value: " + deviceType);
                break;
            }

            logger.debug("******** Received JSON hostmacro: " + hostmacro.toString());
        }

        return deviceType;
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

    private Device genHostObj(String hostname, String ciId, String deviceType,
                              String ciName, String parentID, String service, String hostVisibleName) {
        Device genDevice;
        genDevice = new Device();

        if ("".equals(ciName)) {
            genDevice.setName(hostname);
        } else {
            genDevice.setName(ciName);
        }

        String parentid = parentID;
        if ("0".equals(parentid) || parentid.isEmpty() || "".equals(parentid))
            parentid = null;
        else
            parentid = String.format("%s:%s", endpoint.getConfiguration().getSource(), parentid);

        genDevice.setId(String.format("%s:%s", endpoint.getConfiguration().getSource(), ciId));
        genDevice.setDeviceType(deviceType);
        genDevice.setHostName(hostname);
        genDevice.setVisibleName(hostVisibleName);
        genDevice.setParentID(parentid);

        if (!"".equals(service))
            genDevice.setService(service);

        // gendevice.setDeviceState(host.getStatus());
        // gendevice.setDeviceState(host.getOperationalStatus());

        genDevice.setSource(String.format("%s", endpoint.getConfiguration().getSource()));

        logger.debug("Received ciid: " + ciId);
        logger.debug("Received deviceType: " + deviceType);
        logger.debug(genDevice.toString());

        return genDevice;

    }

    private Device genHostgroupObj(JSONObject hostgroup, String deviceType) {

        Device gendevice;
        gendevice = new Device();

        String hostgroupName = hostgroup.getString("name");

        // zabbix_group_ci_pattern=\\((.*)\\)(.*)
        String groupCiPattern = endpoint.getConfiguration().getGroupCiPattern();
        logger.debug("*** Zabbix Group Pattern: " + groupCiPattern);

        String newHostgroupName;
        String service = "";

        // Example group :
        // (Невский.СЭ)ТЭЦ-1
        Pattern p = Pattern.compile(groupCiPattern);
        Matcher matcher = p.matcher(hostgroupName);

        // if Group has Service CI pattern
        if (matcher.matches()) {
            logger.debug("*** Finded Zabbix Group with Service Pattern: " + hostgroupName);

            newHostgroupName = matcher.group(2);
            service = matcher.group(1);

            logger.debug("*** newHostgroupName: " + newHostgroupName);
            logger.debug("*** service: " + service);
        } else {
            // use parent host as CI for item
            newHostgroupName = hostgroupName;
        }

        gendevice.setId(String.format("%s:%s", endpoint.getConfiguration().getSource(), hostgroup.getString("groupid")));
        gendevice.setDeviceType(deviceType);
        gendevice.setService(service);
        gendevice.setName(newHostgroupName);

        gendevice.setSource(String.format("%s", endpoint.getConfiguration().getSource()));

        logger.debug("Received deviceType: " + deviceType);
        logger.debug(gendevice.toString());

        return gendevice;

    }

    public enum PersistentEventSeverity {
        OK, INFO, WARNING, MINOR, MAJOR, CRITICAL;

        public static PersistentEventSeverity fromValue(String v) {
            return valueOf(v);
        }

        public String value() {
            return name();
        }
    }

    private final class HostsAndGroups {
        private String parentID;
        private String service;
        private JSONArray hostgroups;

        private HostsAndGroups(String parentID, String service, JSONArray hostgroups) {
            this.parentID = parentID;
            this.service = service;
            this.hostgroups = hostgroups;
        }

        public String getParentID() {
            return parentID;
        }

        public String getService() {
            return service;
        }

        public HostsAndGroups invoke() {
            // if parentID is not set in host name
            // Example:

            // zabbix_group_ci_pattern=\\((.*)\\)(.*)
            String groupCiPattern = endpoint.getConfiguration().getGroupCiPattern();

            if ("".equals(parentID)) {
                hostgroupsloop:
                for (int j = 0; j < hostgroups.size(); j++) {
                    JSONObject hostgroup = hostgroups.getJSONObject(j);
                    String hostegroupName = hostgroup.getString("name");

                    // Example group :
                    // (Невский.СЭ)ТЭЦ-1
                    Pattern p = Pattern.compile(groupCiPattern);
                    Matcher matcher = p.matcher(hostegroupName);

                    // if Group has Service CI pattern
                    if (matcher.matches()) {
                        service = matcher.group(1);
                        logger.debug(String.format("************* Found ParentGroup hostgroup: %s and Service: %s", hostegroupName, service));
                        parentID = hostgroup.getString("groupid");
                        logger.debug(String.format("************* Found ParentGroup hostgroup ID: %s", parentID));
                        break hostgroupsloop;
                    }
                    // JSONArray hostgroups = host.getJSONArray("groups");

                    logger.debug("******** Received JSON Hostgroup: " + hostgroup.toString());
                }
            }
            return this;
        }
    }
}