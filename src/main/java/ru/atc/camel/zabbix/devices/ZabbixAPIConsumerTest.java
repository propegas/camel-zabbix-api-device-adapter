package ru.atc.camel.zabbix.devices;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import ru.at_consulting.itsm.device.Device;

import java.util.List;

/**
 * Created by vgoryachev on 31.05.2016.
 * Package: ru.atc.camel.zabbix.devices.
 */
public class ZabbixAPIConsumerTest {

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
        zabbixAPIConfiguration.setSource("Zabbix");
        zabbixAPIEndpoint.setConfiguration(zabbixAPIConfiguration);
        testCons = new ZabbixAPIConsumer(zabbixAPIEndpoint, processor);

//        String stringHostFromZabbix = "[{\"hostid\":\"11889\",\"name\":\"172.20.22.115--Tgc1-proxy-mgr\",\"host\":\"50198c58-249a-60a1-f520-475047fc0d77\",\"groups\":[{\"groupid\":\"6\",\"name\":\"Virtual machines\",\"internal\":\"0\",\"flags\":\"0\"}],\"parentTemplates\":[{\"templateid\":\"10089\",\"host\":\"Template Virt --VMware Guest--\",\"name\":\"Template Virt --VMware Guest--\"},{\"templateid\":\"11313\",\"host\":\"Template Virt --VMware Guest-- additional stats\",\"name\":\"Template Virt --VMware Guest-- additional stats\"},{\"templateid\":\"11311\",\"host\":\"Template Virt --VMware Guest-- additional config\",\"name\":\"Template Virt --VMware Guest-- additional config\"}],\"macros\":[{\"hostmacroid\":\"5133\",\"macro\":\"{$PASSWORD}\",\"value\":\"Mzoning2\"},{\"hostmacroid\":\"5134\",\"macro\":\"{$URL}\",\"value\":\"https:\\/\\/172.20.22.50\\/sdk\"},{\"hostmacroid\":\"5135\",\"macro\":\"{$USERNAME}\",\"value\":\"zsm\"},{\"hostmacroid\":\"5136\",\"macro\":\"{$VC.IP}\",\"value\":\"172.20.22.50\"}]}]";
        String stringHostFromZabbix = "[{\"itemid\":\"304117\",\"name\":\"[Контроллер A (Контроллеры)::Expander Port: Enclosure ID 1, Controller A, Phy 0, PHY index 24, Type SC-1 (IO порты)] Element Status\",\"key_\":\"hp.p2000.stats[ioports,_1_b_0_sc-0,elem-status-numeric]\",\"description\":\"[FOR_INTEGRATION]\",\"hosts\":[{\"hostid\":\"10511\",\"name\":\"MSA2040-C2-2\",\"host\":\"MSA2040-C2-2\"}]}]";
        JSONArray jsonDeviceFromZabbix = (JSONArray) JSON.parse(stringHostFromZabbix);

        List<Device> devicesFromJson = testCons.getDevicesFromZabbixItems(jsonDeviceFromZabbix);

        // main device from item
        Device deviceFromJson = devicesFromJson.get(1);

        Assert.assertThat(deviceFromJson.getHostName(), CoreMatchers.is("MSA2040-C2-2"));
        Assert.assertThat(deviceFromJson.getId(), CoreMatchers.is("Zabbix:7841fbf43ab580036e7e54be79dcd29769d0627e"));
        Assert.assertThat(deviceFromJson.getName(), CoreMatchers.is("EXPANDER PORT: ENCLOSURE ID 1, CONTROLLER A, PHY 0, PHY INDEX 24, TYPE SC-1".toUpperCase()));
        Assert.assertThat(deviceFromJson.getParentID(), CoreMatchers.is("Zabbix:19f58ea90979cbe93e7c130c748a772f107a0edb.IO ПОРТЫ"));
        Assert.assertThat(deviceFromJson.getVisibleName(), CoreMatchers.is("MSA2040-C2-2:EXPANDER PORT: ENCLOSURE ID 1, CONTROLLER A, PHY 0, PHY INDEX 24, TYPE SC-1".toUpperCase()));
        Assert.assertThat(deviceFromJson.getDeviceType(), CoreMatchers.is("IO ПОРТЫ".toUpperCase()));

        // pseudo-ci as device type
        Device device2FromJson = devicesFromJson.get(0);

        Assert.assertThat(device2FromJson.getHostName(), CoreMatchers.is("MSA2040-C2-2"));
        Assert.assertThat(device2FromJson.getId(), CoreMatchers.is("Zabbix:19f58ea90979cbe93e7c130c748a772f107a0edb.IO ПОРТЫ"));
        Assert.assertThat(device2FromJson.getName(), CoreMatchers.is("IO ПОРТЫ".toUpperCase()));
        Assert.assertThat(device2FromJson.getParentID(), CoreMatchers.is("Zabbix:19f58ea90979cbe93e7c130c748a772f107a0edb"));
        Assert.assertThat(device2FromJson.getVisibleName(), CoreMatchers.is("MSA2040-C2-2:IO ПОРТЫ".toUpperCase()));
        Assert.assertThat(device2FromJson.getDeviceType(), CoreMatchers.is("CiGroup"));

    }


}