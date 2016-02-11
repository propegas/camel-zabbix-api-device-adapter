package ru.atc.camel.zabbix.devices;

//import java.io.File;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.at_consulting.itsm.device.Device;

import javax.jms.ConnectionFactory;
import java.util.Objects;

//import org.apache.camel.processor.idempotent.FileIdempotentRepository;
//import ru.at_consulting.itsm.event.Event;

public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);
    public static String activemq_port = null;
    public static String activemq_ip = null;

    public static void main(String[] args) throws Exception {

        logger.info("Starting Custom Apache Camel component example");
        logger.info("Press CTRL+C to terminate the JVM");

        if (args.length == 2) {
            activemq_port = args[1];
            activemq_ip = args[0];
        }

        if (activemq_port == null || "".equals(activemq_port))
            activemq_port = "61616";
        if (activemq_ip == null || Objects.equals(activemq_ip, ""))
            activemq_ip = "172.20.19.195";

        logger.info("activemq_ip: " + activemq_ip);
        logger.info("activemq_port: " + activemq_port);

        org.apache.camel.main.Main main = new org.apache.camel.main.Main();
        main.enableHangupSupport();

        main.addRouteBuilder(new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                JsonDataFormat myJson = new JsonDataFormat();
                myJson.setPrettyPrint(true);
                myJson.setLibrary(JsonLibrary.Jackson);
                myJson.setJsonView(Device.class);
                //myJson.setPrettyPrint(true);

                PropertiesComponent properties = new PropertiesComponent();
                properties.setLocation("classpath:zabbixapi.properties");
                getContext().addComponent("properties", properties);

                ConnectionFactory connectionFactory = new ActiveMQConnectionFactory
                        ("tcp://" + activemq_ip + ":" + activemq_port);
                getContext().addComponent("activemq", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));


                // Heartbeats
                from("timer://foo?period={{heartbeatsdelay}}")
                        .process(new Processor() {
                            public void process(Exchange exchange) throws Exception {
                                ZabbixAPIConsumer.genHeartbeatMessage(exchange);
                            }
                        })
                        //.bean(WsdlNNMConsumer.class, "genHeartbeatMessage", exchange)
                        .marshal(myJson)
                        .to("activemq:{{heartbeatsqueue}}")
                        .log("*** Heartbeat: ${id}");

                from("zabbixapi://devices?"
                        + "delay={{delay}}&"
                        + "zabbixapiurl={{zabbixapiurl}}&"
                        + "username={{username}}&"
                        + "password={{password}}&"
                        + "adaptername={{adaptername}}&"
                        + "source={{source}}&"
                        + "groupCiPattern={{zabbix_group_ci_pattern}}&"
                        + "groupSearchPattern={{zabbix_group_search_pattern}}&"
                        + "itemCiPattern={{zabbix_item_ke_pattern}}&"
                        + "itemCiSearchPattern={{zabbix_item_ke_search_pattern}}&"
                        + "itemCiParentPattern={{zabbix_item_ci_parent_pattern}}&"
                        + "itemCiTypePattern={{zabbix_item_ci_type_pattern}}&"
                        + "zabbixip={{zabbixip}}")

                        .marshal(myJson)

                        .choice()
                        .when(header("queueName").isEqualTo("Devices"))
                        .to("activemq:{{devicesqueue}}")
                        .log("*** Device: ${header.DeviceId} ${header.DeviceType} ${header.ParentId} ")
                        .otherwise()
                        .to("activemq:{{eventsqueue}}")
                        .log("*** Error: ${id} ${header.DeviceId}")
                        .end()

                        .log("${id} ${header.DeviceId} ${header.DeviceType} ${header.ParentId} ");
                //.to("activemq:{{devicesqueue}}");
            }
        });

        main.run();
    }
}