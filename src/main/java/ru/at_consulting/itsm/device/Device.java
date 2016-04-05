package ru.at_consulting.itsm.device;

import java.io.Serializable;
//import java.util.Collection;
//import java.util.List;

//import org.apache.camel.Consume;

//import javax.persistence.Column;
//import javax.persistence.Entity;
//import javax.persistence.GeneratedValue;
//import javax.persistence.Id;
//import javax.persistence.Table;
//import javax.persistence.ElementCollection;

public class Device implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    //   @Consume(uri="activemq:Events.pojo")
    //@Column(name = "Mc_host", nullable = true)
    private String modelNumber;
    //     @Id
//     @GeneratedValue
    //@Column(name = "id")
    private String id;
    //@Column(name = "Date_reception")
    private String deviceType;
    private String source;
    private String service;
    private String deviceState;
    //@Column(name = "Severity", nullable = true)
    private String serialNumber;
    //@Column(name = "Msg", nullable = true)
    private String commState;
    //@Column(name = "Status", nullable = true)
    private String modelName;
    //@Column(name = "Mc_object", nullable = true)
    private String location;

    private String hostName;

    private String name;

    private String ipAddress;

    private String parentID;

    private String visibleName;

    @Override
    public String toString() {
        return "Device: " + this.getId() +
                " ParentId: " + this.getParentID() +
                " on host: " + this.getHostName() +
                " name: " + this.getName() +
                " serial number: " + this.getSerialNumber() +
                " location: " + this.getLocation() +
                " device state: " + this.getDeviceState() +
                " comm state: " + this.getCommState();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getCommState() {
        return commState;
    }

    public void setCommState(String commState) {
        this.commState = commState;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceState() {
        return deviceState;
    }

    public void setDeviceState(String deviceState) {
        this.deviceState = deviceState;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentID() {
        return parentID;
    }

    public void setParentID(String parentID) {
        this.parentID = parentID;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public String getVisibleName() {
        return visibleName;
    }

    public void setVisibleName(String visibleName) {
        this.visibleName = visibleName;
    }
}
