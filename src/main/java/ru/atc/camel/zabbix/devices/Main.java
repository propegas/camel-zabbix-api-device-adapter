package ru.atc.camel.zabbix.devices;

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
import ru.atc.adapters.type.Device;

import javax.jms.ConnectionFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static ru.atc.adapters.message.CamelMessageManager.genHeartbeatMessage;

public final class Main {

    private static final Logger logger = LoggerFactory.getLogger("mainLogger");
    private static String activemqPort;
    private static String activemqIp;
    private static String adaptername;

    private Main() {

    }

    public static void main(String[] args) throws Exception {

        logger.info("Starting Custom Apache Camel component example");
        logger.info("Press CTRL+C to terminate the JVM");

        try {
            // get Properties from file
            InputStream input = new FileInputStream("zabbixapi.properties");
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            adaptername = prop.getProperty("adaptername");
            activemqIp = prop.getProperty("activemq.ip");
            activemqPort = prop.getProperty("activemq.port");
        } catch (IOException ex) {
            logger.error("Error while open and parsing properties file", ex);
        }

        logger.info("**** adaptername: " + adaptername);
        logger.info("activemqIp: " + activemqIp);
        logger.info("activemqPort: " + activemqPort);
        org.apache.camel.main.Main main = new org.apache.camel.main.Main();

        //CHECKSTYLE:OFF: checkstyle:anoninnerlength
        main.addRouteBuilder(new IntegrationRoute());

        main.run();
    }

    private static class IntegrationRoute extends RouteBuilder {
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
                    "tcp://" + activemqIp + ":" + activemqPort);
            getContext().addComponent("activemq", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

            // Heartbeats
            from("timer://foo?period={{heartbeatsdelay}}")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            genHeartbeatMessage(exchange, adaptername);
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
                    .append("hostAliasPattern={{hostAliasPattern}}&")
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
                    .append("hostAliasPattern={{hostAliasPattern}}&")
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
    }

    //CHECKSTYLE:ON: checkstyle:anoninnerlength
}