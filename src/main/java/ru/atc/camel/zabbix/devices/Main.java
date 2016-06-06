package ru.atc.camel.zabbix.devices;

//import java.io.File;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

//import org.apache.camel.processor.idempotent.FileIdempotentRepository;
//import ru.at_consulting.itsm.event.Event;

public final class Main {

    private static final Logger logger = LoggerFactory.getLogger("mainLogger");
    private static String activemq_port;
    private static String activemq_ip;
    private static String source;
    private static String adaptername;

    private Main() {

    }

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

        // get Properties from file
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream("zabbixapi.properties");

            // load a properties file
            prop.load(input);

            source = prop.getProperty("source");
            adaptername = prop.getProperty("adaptername");

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        logger.info("**** adaptername: " + adaptername);
        logger.info("activemq_ip: " + activemq_ip);
        logger.info("activemq_port: " + activemq_port);
        org.apache.camel.main.Main main = new org.apache.camel.main.Main();
        //main.enableHangupSupport();

        //CHECKSTYLE:OFF: checkstyle:anoninnerlength
        main.addRouteBuilder(new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                JsonDataFormat myJson = new JsonDataFormat();
                myJson.setPrettyPrint(true);
                myJson.setLibrary(JsonLibrary.Jackson);
                myJson.setJsonView(Device.class);

                PropertiesComponent properties = new PropertiesComponent();
                properties.setLocation("classpath:zabbixapi.properties");
                getContext().addComponent("properties", properties);

                ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                        "tcp://" + activemq_ip + ":" + activemq_port);
                getContext().addComponent("activemq", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

                // Heartbeats
                from("timer://foo?period={{heartbeatsdelay}}")
                        .process(new Processor() {
                            public void process(Exchange exchange) throws Exception {
                                ZabbixAPIConsumer.genHeartbeatMessage(exchange, adaptername);
                            }
                        })
                        .marshal(myJson)
                        .to("activemq:{{heartbeatsqueue}}")
                        .log(LoggingLevel.DEBUG, logger, "*** Heartbeat: ${id}")
                        .log(LoggingLevel.DEBUG, logger, "***HEARTBEAT BODY: ${in.body}");

                from(new StringBuilder()
                        .append("zabbixapi://devices?")
                        .append("delay={{delay}}&")
                        .append("zabbixapiurl={{zabbixapiurl}}&")
                        .append("username={{username}}&")
                        .append("password={{password}}&")
                        .append("adaptername={{adaptername}}&")
                        .append("source={{source}}&")
                        .append("groupCiPattern={{zabbix_group_ci_pattern}}&")
                        .append("groupSearchPattern={{zabbix_group_search_pattern}}&")
                        .append("itemCiPattern={{zabbix_item_ke_pattern}}&")
                        .append("itemCiSearchPattern={{zabbix_item_ke_search_pattern}}&")
                        .append("itemCiParentPattern={{zabbix_item_ci_parent_pattern}}&")
                        .append("itemCiTypePattern={{zabbix_item_ci_type_pattern}}&")
                        .append("zabbixip={{zabbixip}}")
                        .toString())

                        .marshal(myJson)

                        .choice()
                        .when(header("queueName").isEqualTo("Devices"))
                        .to("activemq:{{devicesqueue}}")
                        .log(LoggingLevel.DEBUG, logger, "*** Device: ${header.DeviceId} (${header.DeviceType}) ParentId: ${header.ParentId}")
                        .log(LoggingLevel.DEBUG, logger, "*** NEW DEVICE BODY: ${in.body}")
                        .endChoice()
                        .otherwise()
                        .to("activemq:{{errorsqueue}}")
                        .log(LoggingLevel.DEBUG, logger, "*** Error: ${id} ${header.DeviceId}")
                        .end()

                        .log("${id} ${header.DeviceId} ${header.DeviceType} ${header.ParentId} ");

                from(new StringBuilder()
                        .append("zabbixapi://vmdevices?")
                        .append("delay={{vmdelay}}&")
                        .append("zabbixapiurl={{zabbixapiurl}}&")
                        .append("username={{username}}&")
                        .append("password={{password}}&")
                        .append("adaptername={{adaptername}}&")
                        .append("source={{source}}&")
                        .append("groupCiPattern={{zabbix_group_ci_pattern}}&")
                        .append("groupSearchPattern={{zabbix_group_search_pattern}}&")
                        .append("itemCiPattern={{zabbix_item_ke_pattern}}&")
                        .append("itemCiSearchPattern={{zabbix_item_ke_search_pattern}}&")
                        .append("itemCiParentPattern={{zabbix_item_ci_parent_pattern}}&")
                        .append("itemCiTypePattern={{zabbix_item_ci_type_pattern}}&")
                        .append("zabbixDevicesVMwareTemplatePattern={{zabbixDevicesVMwareTemplatePattern}}&")
                        .append("zabbixip={{zabbixip}}")
                        .toString())

                        .marshal(myJson)

                        .choice()
                        .when(header("queueName").isEqualTo("Devices"))
                        .to("activemq:{{devicesqueue}}")
                        .log(LoggingLevel.DEBUG, logger, "*** Device: ${header.DeviceId} (${header.DeviceType}) ParentId: ${header.ParentId}")
                        .log(LoggingLevel.DEBUG, logger, "*** NEW DEVICE BODY: ${in.body}")
                        .endChoice()
                        .otherwise()
                        .to("activemq:{{errorsqueue}}")
                        .log(LoggingLevel.DEBUG, logger, "*** Error: ${id} ${header.DeviceId}")
                        .end()

                        .log(LoggingLevel.DEBUG, logger, "${id} ${header.DeviceId} ${header.DeviceType} ${header.ParentId} ");
            }
        });

        //CHECKSTYLE:ON: checkstyle:anoninnerlength

        main.run();
    }
}