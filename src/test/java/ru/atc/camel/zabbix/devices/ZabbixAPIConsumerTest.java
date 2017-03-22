package ru.atc.camel.zabbix.devices;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.atc.adapters.type.Device;

import java.util.List;

/**
 * Created by vgoryachev on 31.05.2016.
 * Package: ru.atc.camel.zabbix.devices.
 */
public class ZabbixAPIConsumerTest {
    private static final Logger logger = LoggerFactory.getLogger("mainLogger");
    private static final Logger loggerErrors = LoggerFactory.getLogger("errorsLogger");

    //CHECKSTYLE:OFF
    @Test
    public void testCiItemNaming2() throws Exception {

        ZabbixAPIComponent zabbixAPIComponent = new ZabbixAPIComponent();

        Processor processor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {

            }
        };

        ZabbixAPIConsumer testCons;
        ZabbixAPIEndpoint zabbixAPIEndpoint = new ZabbixAPIEndpoint("", "", zabbixAPIComponent);
        ZabbixAPIConfiguration zabbixAPIConfiguration = new ZabbixAPIConfiguration();
        zabbixAPIConfiguration.setDelay(5);
        zabbixAPIConfiguration.setItemCiParentPattern("(.*)::(.*)");
        zabbixAPIConfiguration.setItemCiPattern("\\[(.*)\\](.*)");
        zabbixAPIConfiguration.setItemCiTypePattern("(.*)\\((.*)\\)");
        zabbixAPIConfiguration.setHostAliasPattern("(.*)--(.*)");
        zabbixAPIConfiguration.setSource("Zabbix");
        zabbixAPIConfiguration.setZabbixapiurl("http://172.20.19.195/zabbix/api_jsonrpc.php");
        zabbixAPIConfiguration.setPassword("zabbix");
        zabbixAPIConfiguration.setUsername("Admin");
        zabbixAPIEndpoint.setConfiguration(zabbixAPIConfiguration);
        testCons = new ZabbixAPIConsumer(zabbixAPIEndpoint, processor);

//        String stringHostFromZabbix = "[{\"hostid\":\"11889\",\"name\":\"172.20.22.115--Tgc1-proxy-mgr\",\"host\":\"50198c58-249a-60a1-f520-475047fc0d77\",\"groups\":[{\"groupid\":\"6\",\"name\":\"Virtual machines\",\"internal\":\"0\",\"flags\":\"0\"}],\"parentTemplates\":[{\"templateid\":\"10089\",\"host\":\"Template Virt --VMware Guest--\",\"name\":\"Template Virt --VMware Guest--\"},{\"templateid\":\"11313\",\"host\":\"Template Virt --VMware Guest-- additional stats\",\"name\":\"Template Virt --VMware Guest-- additional stats\"},{\"templateid\":\"11311\",\"host\":\"Template Virt --VMware Guest-- additional config\",\"name\":\"Template Virt --VMware Guest-- additional config\"}],\"macros\":[{\"hostmacroid\":\"5133\",\"macro\":\"{$PASSWORD}\",\"value\":\"Mzoning2\"},{\"hostmacroid\":\"5134\",\"macro\":\"{$URL}\",\"value\":\"https:\\/\\/172.20.22.50\\/sdk\"},{\"hostmacroid\":\"5135\",\"macro\":\"{$USERNAME}\",\"value\":\"zsm\"},{\"hostmacroid\":\"5136\",\"macro\":\"{$VC.IP}\",\"value\":\"172.20.22.50\"}]}]";
        String stringHostFromZabbix = "[{\"itemid\":\"359327\",\"name\":\"[engine-2-1 (Шасси Vplex)::mgmt-module-a | /engines/engine-1-1/mgmt-modules/mgmt-module-a (Модули Vplex)] Operational Status\",\"key_\":\"vplex.stat.module.operational-status[/engines/engine-2-1/mgmt-modules/mgmt-module-a]\",\"description\":\"[FOR_INTEGRATION]\",\"hosts\":[{\"hostid\":\"12081\",\"name\":\"172.20.14.135\",\"host\":\"172.20.14.135\"}]}]";
        JSONArray jsonDeviceFromZabbix = (JSONArray) JSON.parse(stringHostFromZabbix);

        List<Device> devicesFromJson = testCons.getDevicesFromZabbixItems(jsonDeviceFromZabbix);

        // main device from item
        Device deviceFromJson = devicesFromJson.get(0);

        Assert.assertThat(deviceFromJson.getHostName(), CoreMatchers.is("172.20.14.135"));
        Assert.assertThat(deviceFromJson.getId(), CoreMatchers.is("Zabbix:172.20.14.135:MGMT-MODULE-A | /ENGINES/ENGINE-1-1/MGMT-MODULES/MGMT-MODULE-A (МОДУЛИ VPLEX)"));
        Assert.assertThat(deviceFromJson.getName(), CoreMatchers.is("mgmt-module-a".toUpperCase()));
        Assert.assertThat(deviceFromJson.getParentID(), CoreMatchers.is("Zabbix:172.20.14.135:ENGINE-2-1 (ШАССИ VPLEX).МОДУЛИ VPLEX"));
        Assert.assertThat(deviceFromJson.getVisibleName(), CoreMatchers.is("172.20.14.135:mgmt-module-a".toUpperCase()));
        Assert.assertThat(deviceFromJson.getDeviceType(), CoreMatchers.is("Модули Vplex".toUpperCase()));

        // pseudo-ci as device type
        Device device2FromJson = devicesFromJson.get(1);

        Assert.assertThat(device2FromJson.getHostName(), CoreMatchers.is("172.20.14.135"));
        Assert.assertThat(device2FromJson.getId(), CoreMatchers.is("Zabbix:172.20.14.135:ENGINE-2-1 (ШАССИ VPLEX).МОДУЛИ VPLEX"));
        Assert.assertThat(device2FromJson.getName(), CoreMatchers.is("Модули Vplex".toUpperCase()));
        Assert.assertThat(device2FromJson.getParentID(), CoreMatchers.is("Zabbix:172.20.14.135:ENGINE-2-1 (ШАССИ VPLEX)"));
        Assert.assertThat(device2FromJson.getVisibleName(), CoreMatchers.is("172.20.14.135:Модули Vplex".toUpperCase()));
        Assert.assertThat(device2FromJson.getDeviceType(), CoreMatchers.is("CiGroup"));

    }

    @Test
    public void testCiItemNaming3() throws Exception {

        ZabbixAPIComponent zabbixAPIComponent = new ZabbixAPIComponent();

        Processor processor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {

            }
        };

        ZabbixAPIConsumer testCons;
        ZabbixAPIEndpoint zabbixAPIEndpoint = new ZabbixAPIEndpoint("", "", zabbixAPIComponent);
        ZabbixAPIConfiguration zabbixAPIConfiguration = new ZabbixAPIConfiguration();
        zabbixAPIConfiguration.setDelay(5);
        zabbixAPIConfiguration.setItemCiParentPattern("(.*)::(.*)");
        zabbixAPIConfiguration.setItemCiPattern("\\[(.*)\\](.*)");
        zabbixAPIConfiguration.setItemCiTypePattern("(.*)\\((.*)\\)");
        zabbixAPIConfiguration.setHostAliasPattern("(.*)--(.*)");
        zabbixAPIConfiguration.setSource("Zabbix");
        zabbixAPIConfiguration.setZabbixapiurl("http://172.20.19.195/zabbix/api_jsonrpc.php");
        zabbixAPIConfiguration.setPassword("zabbix");
        zabbixAPIConfiguration.setUsername("Admin");
        zabbixAPIEndpoint.setConfiguration(zabbixAPIConfiguration);
        testCons = new ZabbixAPIConsumer(zabbixAPIEndpoint, processor);

//        String stringHostFromZabbix = "[{\"hostid\":\"11889\",\"name\":\"172.20.22.115--Tgc1-proxy-mgr\",\"host\":\"50198c58-249a-60a1-f520-475047fc0d77\",\"groups\":[{\"groupid\":\"6\",\"name\":\"Virtual machines\",\"internal\":\"0\",\"flags\":\"0\"}],\"parentTemplates\":[{\"templateid\":\"10089\",\"host\":\"Template Virt --VMware Guest--\",\"name\":\"Template Virt --VMware Guest--\"},{\"templateid\":\"11313\",\"host\":\"Template Virt --VMware Guest-- additional stats\",\"name\":\"Template Virt --VMware Guest-- additional stats\"},{\"templateid\":\"11311\",\"host\":\"Template Virt --VMware Guest-- additional config\",\"name\":\"Template Virt --VMware Guest-- additional config\"}],\"macros\":[{\"hostmacroid\":\"5133\",\"macro\":\"{$PASSWORD}\",\"value\":\"Mzoning2\"},{\"hostmacroid\":\"5134\",\"macro\":\"{$URL}\",\"value\":\"https:\\/\\/172.20.22.50\\/sdk\"},{\"hostmacroid\":\"5135\",\"macro\":\"{$USERNAME}\",\"value\":\"zsm\"},{\"hostmacroid\":\"5136\",\"macro\":\"{$VC.IP}\",\"value\":\"172.20.22.50\"}]}]";
        String stringHostFromZabbix = "[{\"itemid\":\"267178\",\"name\":\"[engine-1-1 (Шасси Vplex)::mgmt-module-a (Модули Vplex)] Operational Status\",\"key_\":\"vplex.stat.module.operational-status[/engines/engine-1-1/mgmt-modules/mgmt-module-a]\",\"description\":\"[FOR_INTEGRATION]\",\"hosts\":[{\"hostid\":\"12081\",\"name\":\"172.20.14.135\",\"host\":\"172.20.14.135\"}]}]";
        JSONArray jsonDeviceFromZabbix = (JSONArray) JSON.parse(stringHostFromZabbix);

        List<Device> devicesFromJson = testCons.getDevicesFromZabbixItems(jsonDeviceFromZabbix);

        // main device from item
        Device deviceFromJson = devicesFromJson.get(1);

        Assert.assertThat(deviceFromJson.getHostName(), CoreMatchers.is("172.20.14.135"));
        Assert.assertThat(deviceFromJson.getId(), CoreMatchers.is("Zabbix:172.20.14.135:MGMT-MODULE-A (МОДУЛИ VPLEX)"));
        Assert.assertThat(deviceFromJson.getName(), CoreMatchers.is("mgmt-module-a".toUpperCase()));
        Assert.assertThat(deviceFromJson.getParentID(), CoreMatchers.is("Zabbix:172.20.14.135:ENGINE-1-1 (ШАССИ VPLEX).МОДУЛИ VPLEX"));
        Assert.assertThat(deviceFromJson.getVisibleName(), CoreMatchers.is("172.20.14.135:mgmt-module-a".toUpperCase()));
        Assert.assertThat(deviceFromJson.getDeviceType(), CoreMatchers.is("Модули Vplex".toUpperCase()));

        // pseudo-ci as device type
        Device device2FromJson = devicesFromJson.get(0);

        Assert.assertThat(device2FromJson.getHostName(), CoreMatchers.is("172.20.14.135"));
        Assert.assertThat(device2FromJson.getId(), CoreMatchers.is("Zabbix:172.20.14.135:ENGINE-1-1 (ШАССИ VPLEX).МОДУЛИ VPLEX"));
        Assert.assertThat(device2FromJson.getName(), CoreMatchers.is("Модули Vplex".toUpperCase()));
        Assert.assertThat(device2FromJson.getParentID(), CoreMatchers.is("Zabbix:172.20.14.135:ENGINE-1-1 (ШАССИ VPLEX)"));
        Assert.assertThat(device2FromJson.getVisibleName(), CoreMatchers.is("172.20.14.135:Модули Vplex".toUpperCase()));
        Assert.assertThat(device2FromJson.getDeviceType(), CoreMatchers.is("CiGroup"));

    }

    //CHECKSTYLE:OFF
    @Test
    public void testCiItemNaming4() throws Exception {

        ZabbixAPIComponent zabbixAPIComponent = new ZabbixAPIComponent();

        Processor processor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {

            }
        };

        ZabbixAPIConsumer testCons;
        ZabbixAPIEndpoint zabbixAPIEndpoint = new ZabbixAPIEndpoint("", "", zabbixAPIComponent);
        ZabbixAPIConfiguration zabbixAPIConfiguration = new ZabbixAPIConfiguration();
        zabbixAPIConfiguration.setDelay(5);
        zabbixAPIConfiguration.setItemCiParentPattern("(.*)::(.*)");
        zabbixAPIConfiguration.setItemCiPattern("\\[(.*)\\](.*)");
        zabbixAPIConfiguration.setItemCiTypePattern("(.*)\\((.*)\\)");
        zabbixAPIConfiguration.setHostAliasPattern("(.*)--(.*)");
        zabbixAPIConfiguration.setSource("Zabbix");
        zabbixAPIConfiguration.setZabbixapiurl("http://172.20.19.195/zabbix/api_jsonrpc.php");
        zabbixAPIConfiguration.setPassword("zabbix");
        zabbixAPIConfiguration.setUsername("Admin");
        zabbixAPIEndpoint.setConfiguration(zabbixAPIConfiguration);
        testCons = new ZabbixAPIConsumer(zabbixAPIEndpoint, processor);

//        String stringHostFromZabbix = "[{\"hostid\":\"11889\",\"name\":\"172.20.22.115--Tgc1-proxy-mgr\",\"host\":\"50198c58-249a-60a1-f520-475047fc0d77\",\"groups\":[{\"groupid\":\"6\",\"name\":\"Virtual machines\",\"internal\":\"0\",\"flags\":\"0\"}],\"parentTemplates\":[{\"templateid\":\"10089\",\"host\":\"Template Virt --VMware Guest--\",\"name\":\"Template Virt --VMware Guest--\"},{\"templateid\":\"11313\",\"host\":\"Template Virt --VMware Guest-- additional stats\",\"name\":\"Template Virt --VMware Guest-- additional stats\"},{\"templateid\":\"11311\",\"host\":\"Template Virt --VMware Guest-- additional config\",\"name\":\"Template Virt --VMware Guest-- additional config\"}],\"macros\":[{\"hostmacroid\":\"5133\",\"macro\":\"{$PASSWORD}\",\"value\":\"Mzoning2\"},{\"hostmacroid\":\"5134\",\"macro\":\"{$URL}\",\"value\":\"https:\\/\\/172.20.22.50\\/sdk\"},{\"hostmacroid\":\"5135\",\"macro\":\"{$USERNAME}\",\"value\":\"zsm\"},{\"hostmacroid\":\"5136\",\"macro\":\"{$VC.IP}\",\"value\":\"172.20.22.50\"}]}]";
        String stringHostFromZabbix = "[{\"itemid\":\"304117\",\"name\":\"[Контроллер A (Контроллеры)::Expander Port: Enclosure ID 1, Controller A, Phy 0, PHY index 24, Type SC-1 | enc.0.24.sc-1 (IO порты)] Element Status\",\"key_\":\"hp.p2000.stats[ioports,_1_b_0_sc-0,elem-status-numeric]\",\"description\":\"[FOR_INTEGRATION]\",\"hosts\":[{\"hostid\":\"10511\",\"name\":\"MSA2040-C2-2\",\"host\":\"MSA2040-C2-2\"}]}]";
        JSONArray jsonDeviceFromZabbix = (JSONArray) JSON.parse(stringHostFromZabbix);

        List<Device> devicesFromJson = testCons.getDevicesFromZabbixItems(jsonDeviceFromZabbix);

        // main device from item
        Device deviceFromJson = devicesFromJson.get(1);

        Assert.assertThat(deviceFromJson.getHostName(), CoreMatchers.is("MSA2040-C2-2"));
        Assert.assertThat(deviceFromJson.getId(), CoreMatchers.is("Zabbix:MSA2040-C2-2:EXPANDER PORT: ENCLOSURE ID 1, CONTROLLER A, PHY 0, PHY INDEX 24, TYPE SC-1 | ENC.0.24.SC-1 (IO ПОРТЫ)"));
        Assert.assertThat(deviceFromJson.getName(), CoreMatchers.is("EXPANDER PORT: ENCLOSURE ID 1, CONTROLLER A, PHY 0, PHY INDEX 24, TYPE SC-1".toUpperCase()));
        Assert.assertThat(deviceFromJson.getParentID(), CoreMatchers.is("Zabbix:MSA2040-C2-2:КОНТРОЛЛЕР A (КОНТРОЛЛЕРЫ).IO ПОРТЫ"));
        Assert.assertThat(deviceFromJson.getVisibleName(), CoreMatchers.is("MSA2040-C2-2:EXPANDER PORT: ENCLOSURE ID 1, CONTROLLER A, PHY 0, PHY INDEX 24, TYPE SC-1".toUpperCase()));
        Assert.assertThat(deviceFromJson.getDeviceType(), CoreMatchers.is("IO ПОРТЫ".toUpperCase()));

        // pseudo-ci as device type
        Device device2FromJson = devicesFromJson.get(0);

        Assert.assertThat(device2FromJson.getHostName(), CoreMatchers.is("MSA2040-C2-2"));
        Assert.assertThat(device2FromJson.getId(), CoreMatchers.is("Zabbix:MSA2040-C2-2:КОНТРОЛЛЕР A (КОНТРОЛЛЕРЫ).IO ПОРТЫ"));
        Assert.assertThat(device2FromJson.getName(), CoreMatchers.is("IO ПОРТЫ".toUpperCase()));
        Assert.assertThat(device2FromJson.getParentID(), CoreMatchers.is("Zabbix:MSA2040-C2-2:КОНТРОЛЛЕР A (КОНТРОЛЛЕРЫ)"));
        Assert.assertThat(device2FromJson.getVisibleName(), CoreMatchers.is("MSA2040-C2-2:IO ПОРТЫ".toUpperCase()));
        Assert.assertThat(device2FromJson.getDeviceType(), CoreMatchers.is("CiGroup"));

    }

    @Test
    public void testCiItemNaming4_1() throws Exception {

        ZabbixAPIComponent zabbixAPIComponent = new ZabbixAPIComponent();

        Processor processor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {

            }
        };

        ZabbixAPIConsumer testCons;
        ZabbixAPIEndpoint zabbixAPIEndpoint = new ZabbixAPIEndpoint("", "", zabbixAPIComponent);
        ZabbixAPIConfiguration zabbixAPIConfiguration = new ZabbixAPIConfiguration();
        zabbixAPIConfiguration.setDelay(5);
        zabbixAPIConfiguration.setItemCiParentPattern("(.*)::(.*)");
        zabbixAPIConfiguration.setItemCiPattern("\\[(.*)\\](.*)");
        zabbixAPIConfiguration.setItemCiTypePattern("(.*)\\((.*)\\)");
        zabbixAPIConfiguration.setHostAliasPattern("(.*)--(.*)");
        zabbixAPIConfiguration.setSource("Zabbix");
        zabbixAPIConfiguration.setZabbixapiurl("http://172.20.19.195/zabbix/api_jsonrpc.php");
        zabbixAPIConfiguration.setPassword("zabbix");
        zabbixAPIConfiguration.setUsername("Admin");
        zabbixAPIEndpoint.setConfiguration(zabbixAPIConfiguration);
        testCons = new ZabbixAPIConsumer(zabbixAPIEndpoint, processor);

//        String stringHostFromZabbix = "[{\"hostid\":\"11889\",\"name\":\"172.20.22.115--Tgc1-proxy-mgr\",\"host\":\"50198c58-249a-60a1-f520-475047fc0d77\",\"groups\":[{\"groupid\":\"6\",\"name\":\"Virtual machines\",\"internal\":\"0\",\"flags\":\"0\"}],\"parentTemplates\":[{\"templateid\":\"10089\",\"host\":\"Template Virt --VMware Guest--\",\"name\":\"Template Virt --VMware Guest--\"},{\"templateid\":\"11313\",\"host\":\"Template Virt --VMware Guest-- additional stats\",\"name\":\"Template Virt --VMware Guest-- additional stats\"},{\"templateid\":\"11311\",\"host\":\"Template Virt --VMware Guest-- additional config\",\"name\":\"Template Virt --VMware Guest-- additional config\"}],\"macros\":[{\"hostmacroid\":\"5133\",\"macro\":\"{$PASSWORD}\",\"value\":\"Mzoning2\"},{\"hostmacroid\":\"5134\",\"macro\":\"{$URL}\",\"value\":\"https:\\/\\/172.20.22.50\\/sdk\"},{\"hostmacroid\":\"5135\",\"macro\":\"{$USERNAME}\",\"value\":\"zsm\"},{\"hostmacroid\":\"5136\",\"macro\":\"{$VC.IP}\",\"value\":\"172.20.22.50\"}]}]";
        String stringHostFromZabbix = "[{\"itemid\":\"335892\",\"name\":\"[Полка 1 (Полки)] Description\",\"key_\":\"hp.p2000.stats[enclosures,enclosure_1,description]\",\"description\":\"[FOR_INTEGRATION]\",\"hosts\":[{\"hostid\":\"10511\",\"name\":\"MSA2040-C2-2\",\"host\":\"MSA2040-C2-2\"}]}]";
        JSONArray jsonDeviceFromZabbix = (JSONArray) JSON.parse(stringHostFromZabbix);

        List<Device> devicesFromJson = testCons.getDevicesFromZabbixItems(jsonDeviceFromZabbix);

        // main device from item
        Device deviceFromJson = devicesFromJson.get(1);

        Assert.assertThat(deviceFromJson.getHostName(), CoreMatchers.is("MSA2040-C2-2"));
        Assert.assertThat(deviceFromJson.getId(), CoreMatchers.is("Zabbix:MSA2040-C2-2:ПОЛКА 1 (ПОЛКИ)"));
        Assert.assertThat(deviceFromJson.getName(), CoreMatchers.is("ПОЛКА 1".toUpperCase()));
        Assert.assertThat(deviceFromJson.getParentID(), CoreMatchers.is("Zabbix:10511.ПОЛКИ"));
        Assert.assertThat(deviceFromJson.getVisibleName(), CoreMatchers.is("MSA2040-C2-2:ПОЛКА 1".toUpperCase()));
        Assert.assertThat(deviceFromJson.getDeviceType(), CoreMatchers.is("ПОЛКИ".toUpperCase()));

        // pseudo-ci as device type
        Device device2FromJson = devicesFromJson.get(0);

        Assert.assertThat(device2FromJson.getHostName(), CoreMatchers.is("MSA2040-C2-2"));
        Assert.assertThat(device2FromJson.getId(), CoreMatchers.is("Zabbix:10511.ПОЛКИ"));
        Assert.assertThat(device2FromJson.getName(), CoreMatchers.is("ПОЛКИ".toUpperCase()));
        Assert.assertThat(device2FromJson.getParentID(), CoreMatchers.is("Zabbix:10511"));
        Assert.assertThat(device2FromJson.getVisibleName(), CoreMatchers.is("MSA2040-C2-2:ПОЛКИ".toUpperCase()));
        Assert.assertThat(device2FromJson.getDeviceType(), CoreMatchers.is("CiGroup"));

    }

    @Test
    public void testCiItemNaming4_2() throws Exception {

        ZabbixAPIComponent zabbixAPIComponent = new ZabbixAPIComponent();

        Processor processor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {

            }
        };

        ZabbixAPIConsumer testCons;
        ZabbixAPIEndpoint zabbixAPIEndpoint = new ZabbixAPIEndpoint("", "", zabbixAPIComponent);
        ZabbixAPIConfiguration zabbixAPIConfiguration = new ZabbixAPIConfiguration();
        zabbixAPIConfiguration.setDelay(5);
        zabbixAPIConfiguration.setItemCiParentPattern("(.*)::(.*)");
        zabbixAPIConfiguration.setItemCiPattern("\\[(.*)\\](.*)");
        zabbixAPIConfiguration.setItemCiTypePattern("(.*)\\((.*)\\)");
        zabbixAPIConfiguration.setHostAliasPattern("(.*)--(.*)");
        zabbixAPIConfiguration.setSource("Zabbix");
        zabbixAPIConfiguration.setZabbixapiurl("http://172.20.19.195/zabbix/api_jsonrpc.php");
        zabbixAPIConfiguration.setPassword("zabbix");
        zabbixAPIConfiguration.setUsername("Admin");
        zabbixAPIEndpoint.setConfiguration(zabbixAPIConfiguration);
        testCons = new ZabbixAPIConsumer(zabbixAPIEndpoint, processor);

//        String stringHostFromZabbix = "[{\"hostid\":\"11889\",\"name\":\"172.20.22.115--Tgc1-proxy-mgr\",\"host\":\"50198c58-249a-60a1-f520-475047fc0d77\",\"groups\":[{\"groupid\":\"6\",\"name\":\"Virtual machines\",\"internal\":\"0\",\"flags\":\"0\"}],\"parentTemplates\":[{\"templateid\":\"10089\",\"host\":\"Template Virt --VMware Guest--\",\"name\":\"Template Virt --VMware Guest--\"},{\"templateid\":\"11313\",\"host\":\"Template Virt --VMware Guest-- additional stats\",\"name\":\"Template Virt --VMware Guest-- additional stats\"},{\"templateid\":\"11311\",\"host\":\"Template Virt --VMware Guest-- additional config\",\"name\":\"Template Virt --VMware Guest-- additional config\"}],\"macros\":[{\"hostmacroid\":\"5133\",\"macro\":\"{$PASSWORD}\",\"value\":\"Mzoning2\"},{\"hostmacroid\":\"5134\",\"macro\":\"{$URL}\",\"value\":\"https:\\/\\/172.20.22.50\\/sdk\"},{\"hostmacroid\":\"5135\",\"macro\":\"{$USERNAME}\",\"value\":\"zsm\"},{\"hostmacroid\":\"5136\",\"macro\":\"{$VC.IP}\",\"value\":\"172.20.22.50\"}]}]";
        String stringHostFromZabbix = "[{\"itemid\":\"240720\",\"name\":\"[Полка 1 (Полки)::Контроллер A (Контроллеры)] CPU load\",\"key_\":\"hp.p2000.stats[controllers,controller_a,cpu-load]\",\"description\":\"[FOR_INTEGRATION]\",\"hosts\":[{\"hostid\":\"10511\",\"name\":\"MSA2040-C2-2\",\"host\":\"MSA2040-C2-2\"}]}]";
        JSONArray jsonDeviceFromZabbix = (JSONArray) JSON.parse(stringHostFromZabbix);

        List<Device> devicesFromJson = testCons.getDevicesFromZabbixItems(jsonDeviceFromZabbix);

        // main device from item
        Device deviceFromJson = devicesFromJson.get(1);

        Assert.assertThat(deviceFromJson.getHostName(), CoreMatchers.is("MSA2040-C2-2"));
        Assert.assertThat(deviceFromJson.getId(), CoreMatchers.is("Zabbix:MSA2040-C2-2:КОНТРОЛЛЕР A (КОНТРОЛЛЕРЫ)"));
        Assert.assertThat(deviceFromJson.getName(), CoreMatchers.is("Контроллер A".toUpperCase()));
        Assert.assertThat(deviceFromJson.getParentID(), CoreMatchers.is("Zabbix:MSA2040-C2-2:ПОЛКА 1 (ПОЛКИ).КОНТРОЛЛЕРЫ"));
        Assert.assertThat(deviceFromJson.getVisibleName(), CoreMatchers.is("MSA2040-C2-2:КОНТРОЛЛЕР A".toUpperCase()));
        Assert.assertThat(deviceFromJson.getDeviceType(), CoreMatchers.is("КОНТРОЛЛЕРЫ".toUpperCase()));

        // pseudo-ci as device type
        Device device2FromJson = devicesFromJson.get(0);

        Assert.assertThat(device2FromJson.getHostName(), CoreMatchers.is("MSA2040-C2-2"));
        Assert.assertThat(device2FromJson.getId(), CoreMatchers.is("Zabbix:MSA2040-C2-2:ПОЛКА 1 (ПОЛКИ).КОНТРОЛЛЕРЫ"));
        Assert.assertThat(device2FromJson.getName(), CoreMatchers.is("КОНТРОЛЛЕРЫ".toUpperCase()));
        Assert.assertThat(device2FromJson.getParentID(), CoreMatchers.is("Zabbix:MSA2040-C2-2:ПОЛКА 1 (ПОЛКИ)"));
        Assert.assertThat(device2FromJson.getVisibleName(), CoreMatchers.is("MSA2040-C2-2:КОНТРОЛЛЕРЫ".toUpperCase()));
        Assert.assertThat(device2FromJson.getDeviceType(), CoreMatchers.is("CiGroup"));

    }

    @Test
    public void testCiItemNaming5() throws Exception {

        ZabbixAPIComponent zabbixAPIComponent = new ZabbixAPIComponent();

        Processor processor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {

            }
        };

        ZabbixAPIConsumer testCons;
        ZabbixAPIEndpoint zabbixAPIEndpoint = new ZabbixAPIEndpoint("", "", zabbixAPIComponent);
        ZabbixAPIConfiguration zabbixAPIConfiguration = new ZabbixAPIConfiguration();
        zabbixAPIConfiguration.setDelay(5);
        zabbixAPIConfiguration.setItemCiParentPattern("(.*)::(.*)");
        zabbixAPIConfiguration.setItemCiPattern("\\[(.*)\\](.*)");
        zabbixAPIConfiguration.setItemCiTypePattern("(.*)\\((.*)\\)");
        zabbixAPIConfiguration.setHostAliasPattern("(.*)--(.*)");
        zabbixAPIConfiguration.setSource("Zabbix");
        zabbixAPIConfiguration.setGroupCiPattern("\\((.*)\\)(.*)");
        zabbixAPIConfiguration.setZabbixapiurl("http://172.20.19.195/zabbix/api_jsonrpc.php");
        zabbixAPIConfiguration.setPassword("zabbix");
        zabbixAPIConfiguration.setUsername("Admin");
        zabbixAPIEndpoint.setConfiguration(zabbixAPIConfiguration);
        testCons = new ZabbixAPIConsumer(zabbixAPIEndpoint, processor);

        System.out.println("hostAliasPattern: " +
                zabbixAPIConfiguration.getHostAliasPattern());

        String stringHostFromZabbix = "[{\"hostid\":\"11889\",\"name\":\"172.20.22.115--Tgc1-proxy-mgr\",\"host\":\"50198c58-249a-60a1-f520-475047fc0d77\",\"groups\":[{\"groupid\":\"6\",\"name\":\"Virtual machines\",\"internal\":\"0\",\"flags\":\"0\"}],\"parentTemplates\":[{\"templateid\":\"10089\",\"host\":\"Template Virt --VMware Guest--\",\"name\":\"Template Virt --VMware Guest--\"},{\"templateid\":\"11313\",\"host\":\"Template Virt --VMware Guest-- additional stats\",\"name\":\"Template Virt --VMware Guest-- additional stats\"},{\"templateid\":\"11311\",\"host\":\"Template Virt --VMware Guest-- additional config\",\"name\":\"Template Virt --VMware Guest-- additional config\"}],\"macros\":[{\"hostmacroid\":\"5133\",\"macro\":\"{$PASSWORD}\",\"value\":\"Mzoning2\"},{\"hostmacroid\":\"5134\",\"macro\":\"{$URL}\",\"value\":\"https:\\/\\/172.20.22.50\\/sdk\"},{\"hostmacroid\":\"5135\",\"macro\":\"{$USERNAME}\",\"value\":\"zsm\"},{\"hostmacroid\":\"5136\",\"macro\":\"{$VC.IP}\",\"value\":\"172.20.22.50\"}]}," +
                "{\"groups\":[{\"flags\":\"0\",\"groupid\":\"7\",\"internal\":\"0\",\"name\":\"Hypervisors\"},{\"flags\":\"4\",\"groupid\":\"187\",\"internal\":\"0\",\"name\":\"C7000-1\"},{\"flags\":\"0\",\"groupid\":\"96\",\"internal\":\"0\",\"name\":\"(Невский.ВВС)Прочее\"}],\"host\":\"33373336-3239-5a43-3231-343630324a59\",\"hostid\":\"11331\",\"macros\":[{\"hostmacroid\":\"2919\",\"macro\":\"{$PASSWORD}\",\"value\":\"Mzoning2\"},{\"hostmacroid\":\"2920\",\"macro\":\"{$URL}\",\"value\":\"https://172.20.22.50/sdk\"},{\"hostmacroid\":\"2921\",\"macro\":\"{$USERNAME}\",\"value\":\"zsm\"},{\"hostmacroid\":\"2922\",\"macro\":\"{$VC.IP}\",\"value\":\"172.20.22.50\"}],\"name\":\"172.20.22.115\",\"parentTemplates\":[{\"host\":\"Template Virt --VMware Hypervisor--\",\"name\":\"Template Virt --VMware Hypervisor--\",\"templateid\":\"10091\"}]}" +
                "]";
        //       String stringHostFromZabbix = "[{\"itemid\":\"304117\",\"name\":\"[Контроллер A (Контроллеры)::Expander Port: Enclosure ID 1, Controller A, Phy 0, PHY index 24, Type SC-1 (IO порты)] Element Status\",\"key_\":\"hp.p2000.stats[ioports,_1_b_0_sc-0,elem-status-numeric]\",\"description\":\"[FOR_INTEGRATION]\",\"hosts\":[{\"hostid\":\"10511\",\"name\":\"MSA2040-C2-2\",\"host\":\"MSA2040-C2-2\"}]}]";
        JSONArray jsonDeviceFromZabbix = (JSONArray) JSON.parse(stringHostFromZabbix);

        System.out.println("SIZE: " + jsonDeviceFromZabbix.size());

        List<Device> devicesFromJson = testCons.getDevicesFromZabbixHosts(null, jsonDeviceFromZabbix);

        // main device from item
        Device deviceFromJson = devicesFromJson.get(0);

        Assert.assertThat(deviceFromJson.getHostName(), CoreMatchers.is("TGC1-PROXY-MGR"));
        Assert.assertThat(deviceFromJson.getId(), CoreMatchers.is("Zabbix:11889"));
        Assert.assertThat(deviceFromJson.getName(), CoreMatchers.is("Tgc1-proxy-mgr".toUpperCase()));
        Assert.assertThat(deviceFromJson.getParentID(), CoreMatchers.is("Zabbix:11331"));
        Assert.assertThat(deviceFromJson.getVisibleName(), CoreMatchers.is("172.20.22.115--Tgc1-proxy-mgr"));
        Assert.assertThat(deviceFromJson.getDeviceType(), CoreMatchers.is("host"));

    }

    @Test
    public void testCiItemNaming6() throws Exception {

        ZabbixAPIComponent zabbixAPIComponent = new ZabbixAPIComponent();

        Processor processor = new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {

            }
        };

        ZabbixAPIConsumer testCons;
        ZabbixAPIEndpoint zabbixAPIEndpoint = new ZabbixAPIEndpoint("", "", zabbixAPIComponent);
        ZabbixAPIConfiguration zabbixAPIConfiguration = new ZabbixAPIConfiguration();
        zabbixAPIConfiguration.setDelay(5);
        zabbixAPIConfiguration.setItemCiParentPattern("(.*)::(.*)");
        zabbixAPIConfiguration.setItemCiPattern("\\[(.*)\\](.*)");
        zabbixAPIConfiguration.setItemCiTypePattern("(.*)\\((.*)\\)");
        zabbixAPIConfiguration.setHostAliasPattern("(.*)--(.*)");
        zabbixAPIConfiguration.setSource("Zabbix");
        zabbixAPIConfiguration.setZabbixapiurl("http://172.20.19.195/zabbix/api_jsonrpc.php");
        zabbixAPIConfiguration.setPassword("zabbix");
        zabbixAPIConfiguration.setUsername("Admin");
        zabbixAPIConfiguration.setGroupCiPattern("\\((.*)\\)(.*)");
        zabbixAPIEndpoint.setConfiguration(zabbixAPIConfiguration);
        testCons = new ZabbixAPIConsumer(zabbixAPIEndpoint, processor);
        //testCons.setEndpoint(zabbixAPIEndpoint);
        //ZabbixAPIConsumer.end

        //testCons.getEndpoint().getEndpointConfiguration().g

        String stringHostFromZabbix = "[{\"groups\":[{\"flags\":\"0\",\"groupid\":\"40\",\"internal\":\"0\",\"name\":\"(Невский.СТС)ТЭЦ-1\"},{\"flags\":\"0\",\"groupid\":\"8\",\"internal\":\"0\",\"name\":\"Невский Nortel\"}],\"host\":\"172.20.150.83--NORTEL\",\"hostid\":\"10161\",\"macros\":[{\"hostmacroid\":\"13\",\"macro\":\"{$SNMP_COMMUNITY}\",\"value\":\"otm123\"},{\"macro\":\"{$TYPE}\",\"value\":\"Nortel\"}],\"name\":\"Диспетчерская АТС ТЭЦ-1--NORTEL\",\"parentTemplates\":[{\"host\":\"Template --SNMP Traps Nortel--\",\"name\":\"Template --SNMP Traps Nortel--\",\"templateid\":\"10108\"}]}," +
                "{\"groups\":[{\"flags\":\"0\",\"groupid\":\"40\",\"internal\":\"0\",\"name\":\"(Невский.СТС)ТЭЦ-1\"},{\"flags\":\"0\",\"groupid\":\"8\",\"internal\":\"0\",\"name\":\"Невский Nortel\"}],\"host\":\"172.20.150.83\",\"hostid\":\"10109\",\"macros\":[{\"hostmacroid\":\"130\",\"macro\":\"{$TYPE}\",\"value\":\"phone\"},{\"hostmacroid\":\"11\",\"macro\":\"{$SNMP_COMMUNITY}\",\"value\":\"otm123\"}],\"name\":\"Диспетчерская АТС ТЭЦ-1\",\"parentTemplates\":[{\"host\":\"Template --ICMP Ping--\",\"name\":\"Template --ICMP Ping--\",\"templateid\":\"10104\"}]}]";
        //       String stringHostFromZabbix = "[{\"itemid\":\"304117\",\"name\":\"[Контроллер A (Контроллеры)::Expander Port: Enclosure ID 1, Controller A, Phy 0, PHY index 24, Type SC-1 (IO порты)] Element Status\",\"key_\":\"hp.p2000.stats[ioports,_1_b_0_sc-0,elem-status-numeric]\",\"description\":\"[FOR_INTEGRATION]\",\"hosts\":[{\"hostid\":\"10511\",\"name\":\"MSA2040-C2-2\",\"host\":\"MSA2040-C2-2\"}]}]";
        JSONArray jsonDeviceFromZabbix = (JSONArray) JSON.parse(stringHostFromZabbix);

        System.out.println("SIZE: " + jsonDeviceFromZabbix.size());

        List<Device> devicesFromJson = testCons.getDevicesFromZabbixHosts(null, jsonDeviceFromZabbix);

        // main device from item
        Device deviceFromJson = devicesFromJson.get(0);

        Assert.assertThat(deviceFromJson.getHostName(), CoreMatchers.is("172.20.150.83"));
        Assert.assertThat(deviceFromJson.getId(), CoreMatchers.is("Zabbix:10161"));
        Assert.assertThat(deviceFromJson.getName(), CoreMatchers.is("172.20.150.83:NORTEL".toUpperCase()));
        Assert.assertThat(deviceFromJson.getParentID(), CoreMatchers.is("Zabbix:10109"));
        Assert.assertThat(deviceFromJson.getVisibleName(), CoreMatchers.is("Диспетчерская АТС ТЭЦ-1--NORTEL"));
        Assert.assertThat(deviceFromJson.getDeviceType(), CoreMatchers.is("Nortel"));

    }

}