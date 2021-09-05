/**
 *  Zemismart Button V0.5
 *
 *  Copyright 2020 YSB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import groovy.json.JsonOutput
import physicalgraph.zigbee.clusters.iaszone.ZoneStatus
import physicalgraph.zigbee.zcl.DataType

metadata 
{
   definition (name: "Zemismart Button(JayN)", namespace: "JayN", author: "YooSangBeom/JayN", ocfDeviceType: "x.com.st.d.remotecontroller", mcdSync: true)
   {
      capability "Actuator"
      capability "Battery"
      capability "Button"
      capability "Holdable Button"
      capability "Refresh"
      capability "Sensor"
      capability "Health Check"
      
      fingerprint inClusters: "0000, 0001, 0006",       outClusters: "0019, 000A", manufacturer: "_TZ3400_keyjqthh", model: "TS0041", deviceJoinName: "Zemismart Button",   mnmn: "SmartThings", vid: "generic-2-button"
      fingerprint inClusters: "0000, 0001, 0006",       outClusters: "0019, 000A", manufacturer: "_TZ3400_tk3s5tyg", model: "TS0041", deviceJoinName: "Zemismart Button",   mnmn: "SmartThings", vid: "generic-2-button"
      fingerprint inClusters: "0000, 0001, 0006",       outClusters: "0019",       manufacturer: "_TYZB02_keyjhapk", model: "TS0042", deviceJoinName: "Zemismart Button 2", mnmn: "SmartThings", vid: "generic-2-button"
      fingerprint inClusters: "0000, 0001, 0006",       outClusters: "0019",       manufacturer: "_TZ3400_keyjhapk", model: "TS0042", deviceJoinName: "Zemismart Button 2", mnmn: "SmartThings", vid: "generic-2-button"
      
      fingerprint inClusters: "0000, 0001, 0006",       outClusters: "0019, 000A", manufacturer: "_TZ3400_key8kk7r", model: "TS0043", deviceJoinName: "Zemismart Button 3", mnmn: "SmartThings", vid: "generic-4-button"
      fingerprint inClusters: "0000, 0001, 0006",       outClusters: "0019",       manufacturer: "_TYZB02_key8kk7r", model: "TS0043", deviceJoinName: "Zemismart Button 3", mnmn: "SmartThings", vid: "generic-4-button"
      fingerprint inClusters: "0000, 0001, 0006",       outClusters: "0019, 000A", manufacturer: "_TZ3000_qzjcsmar", model: "TS0043", deviceJoinName: "Zemismart Button 3", mnmn: "SmartThings", vid: "generic-4-button"    
      
      fingerprint inClusters: "0000, 000A, 0001, 0006", outClusters: "0019",       manufacturer: "_TZ3000_vp6clf9d", model: "TS0044", deviceJoinName: "Zemismart Button 4", mnmn: "SmartThings", vid: "generic-4-button"
      
      fingerprint inClusters: "0000, 0001, 0006",       outClusters: "0019, 000A", manufacturer: "_TZ3000_dku2cfsc", model: "TS0044", deviceJoinName: "Zemismart Button 4", mnmn: "SmartThings", vid: "generic-4-button"
       
   }

   tiles(scale: 2)
   {  
      multiAttributeTile(name: "button", type: "generic", width: 2, height: 2) 
      {
         tileAttribute("device.button", key: "PRIMARY_CONTROL") 
         {
            attributeState "pushed", label: "Pressed", icon:"st.Weather.weather14", backgroundColor:"#53a7c0"
            attributeState "double", label: "Pressed Twice", icon:"st.Weather.weather11", backgroundColor:"#53a7c0"
            attributeState "held", label: "Held", icon:"st.Weather.weather13", backgroundColor:"#53a7c0"
         }
      }
      valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) 
      {
         state "battery", label: '${currentValue}% battery', unit: ""
      }
      standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) 
      {
         state "default", action: "refresh.refresh", icon: "st.secondary.refresh"
      }

      main(["button"])
      details(["button","battery", "refresh"])
   }
}

private getAttrid_Battery() { 0x0020 } //
private getCLUSTER_GROUPS() { 0x0004 }
private getCLUSTER_SCENES() { 0x0005 }
private getCLUSTER_WINDOW_COVERING() { 0x0102 }

private boolean isZemismart1gang() 
{
   device.getDataValue("model") == "TS0041"
}

private boolean isZemismart2gang() 
{
   device.getDataValue("model") == "TS0042"
}

private boolean isZemismart3gang() 
{
   device.getDataValue("model") == "TS0043"
}

private boolean isZemismart4gang() 
{
   device.getDataValue("model") == "TS0044"
}

private boolean isZemismart6gang() 
{
   device.getDataValue("model") == "TS0601"
}

/*
private Map getBatteryEvent(value) 
{
   def result = [:]
   //result.value = value
   //Always value 0
   result.value = 100
   result.name = 'battery'
   result.descriptionText = "${device.displayName} battery was ${result.value}%"
   return result
}
*/

private channelNumber(String dni) 
{
   dni.split(":")[-1] as Integer
}

// parse() is called when the hub receives a message from a device.
def parse(String description) 
{
   log.debug "description is [$description]"
   def event = zigbee.getEvent(description) //정상이면 event is [:]
   log.debug "getEvent is [$event]" 
   
   if (event) //non-standard 
   {
       sendEvent(event)
       log.debug "sendEvent $event"
   }
   else //정상 (event값은 "[:]")
   {
      if ((description?.startsWith("catchall:")) || (description?.startsWith("read attr -"))) 
      {
         def descMap = zigbee.parseDescriptionAsMap(description) 
         
         log.debug "descMap(parseDescriptionAsMap) is $descMap"
         
         //배터리 정보 체크(description이 "read attr -"로 시작하면 보통 배터리 정보가 넘어온다.
         //----------------
         //description is [read attr - raw:32B501000110210020C82000201E, dni:32B5, endpoint:01, cluster:0001, size:16, attrId:0021, 
         //result:success, encoding:20, value: 1e200020c8]
         //
         //descMap(parseDescriptionAsMap) is [raw:32B501000110210020C82000201E, dni:32B5, endpoint:01, cluster:0001, size:16, attrId:0021, 
         //result:success, encoding:20, value:c8, additionalAttrs:[[attrId:0020, attrInt:32, encoding:20, value:1e, isValidForDataType:true, 
         //consumedBytes:8]], isValidForDataType:true, clusterInt:1, attrInt:33]
         //----------------
         if (descMap.clusterInt == 0x0001 && descMap.commandInt != 0x07 && descMap?.value) {
			if (descMap.attrInt == 0x0021) {
				event = getBatteryPercentageResult(Integer.parseInt(descMap.value,16))
			} else {
				event = getBatteryResult(Integer.parseInt(descMap.value, 16))
            }
         }
         //버튼 정보 체크(description이 "catchall:"로 시작하면 보통 버튼 정보가 넘어온다.
         //-----------------
         //description is [catchall: 0104 0006 01 01 0000 00 7F6B 01 00 0000 FD 00 00]
         //
         //descMap(parseDescriptionAsMap) is [raw:0104 0006 01 01 0000 00 7F6B 01 00 0000 FD 00 00, profileId:0104, clusterId:0006, 
         //sourceEndpoint:01, destinationEndpoint:01, options:0000, messageType:00, dni:7F6B, isClusterSpecific:true, 
         //isManufacturerSpecific:false, manufacturerId:0000, command:FD, direction:00, data:[00], clusterInt:6, commandInt:253]
         if (descMap.clusterInt == 0x0006) 
         {
            event = parseNonIasButtonMessage(descMap)
         }
            
      }
      return createEvent( event )
   }
      /*
      def result = []
      if (event) 
      {
         log.debug "Creating event: ${event}"
	     result = createEvent(event)
      } 
      else if (isBindingTableMessage(description))         
      {
         Integer groupAddr = getGroupAddrFromBindingTable(description)
         if (groupAddr != null) 
         {
            List cmds = addHubToGroup(groupAddr)
            result = cmds?.collect 
            { 
               new physicalgraph.device.HubAction(it) 
            }
         } 
         else 
         {
            groupAddr = 0x0000
            List cmds = addHubToGroup(groupAddr) +
            zigbee.command(CLUSTER_GROUPS, 0x00, "${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr, 4))} 00")
            result = cmds?.collect 
            { 
               new physicalgraph.device.HubAction(it) 
            }
         }
      }
      return result
      
   }
   log.debug "allevent $event"
   */
   
}

def getBatteryPercentageResult(rawValue) {
	log.debug "attrInt == 0x0021 : getBatteryPercentageResult"
    log.debug "Battery Percentage rawValue = ${rawValue} -> ${rawValue / 2}%"
	def result = [:]

	if (0 <= rawValue && rawValue <= 200) {
		result.name = 'battery'
		result.translatable = true
		result.value = Math.round(rawValue / 2)
		result.descriptionText = "${device.displayName} battery was ${result.value}%"
               
        //sendEvent(name: "battery", value: result.value , descriptionText: result.descriptionText, isStateChange: true)
	}

	//return result
    return [ name: 'battery', value: result.value, descriptionText: result.descriptionText, isStateChange: true ]
}

private Map getBatteryResult(rawValue) {
    log.debug "getBatteryResult"
	log.debug 'Battery'
	def linkText = getLinkText(device)

  def result = [:]

	def volts = rawValue / 10
	if (!(rawValue == 0 || rawValue == 255)) {
		def minVolts = 2.1
		def maxVolts = 3.0
		def pct = (volts - minVolts) / (maxVolts - minVolts)
		def roundedPct = Math.round(pct * 100)
		if (roundedPct <= 0)
			roundedPct = 1
		result.name = 'battery'
        result.value = Math.min(100, roundedPct)
		result.descriptionText = "${linkText} battery was ${result.value}%"		
                
        //sendEvent(name: "battery", value: result.value , descriptionText: result.descriptionText, isStateChange: true)

	}

	//return result
    return [ name: 'battery', value: result.value, descriptionText: result.descriptionText, isStateChange: true ]
}

private Map parseNonIasButtonMessage(Map descMap)
{
    def buttonState
    def buttonNumber = 0
    Map result = [:]
   
   if (descMap.clusterInt == 0x0006) 
   {
      switch(descMap.sourceEndpoint) 
      {
         case "01":
            buttonNumber = 1
            break
         case "02":
            buttonNumber = 2
            break
         case "03":
            buttonNumber = 3
            break        
         case "04":
            buttonNumber = 4
            break
         case "05":
            buttonNumber = 5
            break
         case "06":
            buttonNumber = 6
            break
      }
      switch(descMap.data)
      {
         case "[00]":
            buttonState = "pushed"
            break
         case "[01]":
            buttonState = "double"
            break
         case "[02]":
            buttonState = "held"
            break
      }
      if (buttonNumber !=0) 
      {
         def descriptionText = "button $buttonNumber was $buttonState"
         
         // Create old style
         //-----------------
         //수정 전
         //result = [name: "button", value: buttonState, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true]
         
         //수정 후
         //밑의 줄과 (*1)을 주석처리 하면 List Events에 나타나지를 않아 클래식앱에서 보이지를 않는다. 
         //클래식앱에서 recently에 보여지려면 displayed: true로 놓아야 한다.
         result = [name: "button", value: buttonState, data: [buttonNumber: buttonNumber], descriptionText: descriptionText, isStateChange: true, displayed: false] 
         
         // Create and send component event
         sendButtonEvent(buttonNumber, buttonState)
         
      }
      result //(*1)
   }
}

private sendButtonEvent(buttonNumber, buttonState) 
{
   def child = childDevices?.find { channelNumber(it.deviceNetworkId) == buttonNumber }
   if (child)
   {
      def descriptionText = "$child.displayName was $buttonState" // TODO: Verify if this is needed, and if capability template already has it handled
      //log.debug "Child $child($descriptionText)"
      log.debug "[$descriptionText]"
      
      //몇번 버튼이 눌려졌는지 알기위해서는 밑의 문장으로 처리해야 한다. 만약 밑의 문장을 주석처리하고 
      //위의 (*1)처럼 하면 뉴앱에서는 몇번 버튼인지 안나오고 그냥 누름, 두번 누름 이렇게 나온다.
      //----------------
      child?.sendEvent([name: "button", value: buttonState, data: [buttonNumber: 1], descriptionText: descriptionText, isStateChange: true])
   } 
   else 
   {
      log.debug "Child device $buttonNumber not found!"
   }
}

def refresh() 
{
    //log.debug "Refreshing Battery"
    updated()
    //return zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, getAttrid_Battery()) 
}

def configure() 
{
    log.debug "Configuring Reporting, IAS CIE, and Bindings."
    def cmds = []

    return //zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, getAttrid_Battery(), DataType.UINT8, 30, 21600, 0x01) +
           zigbee.enrollResponse() +
           //zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, getAttrid_Battery()) +
           //zigbee.addBinding(zigbee.ONOFF_CLUSTER) +
           readDeviceBindingTable() // Need to read the binding table to see what group it's using            
           cmds
}

private getButtonName(buttonNum) 
{
   return "${device.displayName} " + buttonNum
}

private void createChildButtonDevices(numberOfButtons) 
{
   state.oldLabel = device.label
   log.debug "Creating $numberOfButtons"
   log.debug "Creating $numberOfButtons children"
   
   for (i in 1..numberOfButtons) 
   {
      log.debug "Creating child $i"
      def child = addChildDevice("smartthings", "Child Button", "${device.deviceNetworkId}:${i}", device.hubId,[completedSetup: true, label: getButtonName(i),
				 isComponent: true, componentName: "button$i", componentLabel: "buttton ${i}"])
      child.sendEvent(name: "supportedButtonValues",value: ["pushed","held","double"].encodeAsJSON(), displayed: false)
      child.sendEvent(name: "numberOfButtons", value: 1, displayed: false)
      child.sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], displayed: false)
   }
}

def installed() 
{
    def numberOfButtons
    if (isZemismart1gang()) 
    {
       numberOfButtons = 1
    } 
    else if (isZemismart2gang()) 
    {
       numberOfButtons = 2
    } 
    else if (isZemismart3gang()) 
    {
       numberOfButtons = 3
    }
    else if (isZemismart4gang()) 
    {
       numberOfButtons = 4
    }
    else if (isZemismart6gang()) //*****추가 6구
    {
       numberOfButtons = 6
    }
    
   
    createChildButtonDevices(numberOfButtons) //Todo
    
    sendEvent(name: "supportedButtonValues", value: ["pushed","held","double"].encodeAsJSON(), displayed: false)
    sendEvent(name: "numberOfButtons", value: numberOfButtons , displayed: false)
    //sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], displayed: false)

    // Initialize default states
    numberOfButtons.times 
    {
        sendEvent(name: "button", value: "pushed", data: [buttonNumber: it+1], displayed: false)
    }
    
    // Set an initial checkInterval of twenty-four hours for the Health Check. Change to
    // two hours and ten minutes when the regular 50-60 minutes battery reports start.
    //sendEvent( name: 'checkInterval', value: 86400, displayed: false, data: [ protocol: 'zigbee', hubHardwareId: device.hub.hardwareID ] )
    sendEvent(name: 'checkInterval', value: 7800, data: [ protocol: 'zigbee', hubHardwareId: device.hub.hardwareID ] )
    
    // These devices don't report regularly so they should only go OFFLINE when Hub is OFFLINE
    sendEvent(name: "DeviceWatch-Enroll", value: JsonOutput.toJson([protocol: "zigbee", scheme:"untracked"]), displayed: false)
}

def updated() 
{
   log.debug "childDevices $childDevices"
   if (childDevices && device.label != state.oldLabel) 
   {
      childDevices.each 
      {
         def newLabel = getButtonName(channelNumber(it.deviceNetworkId))
	     it.setLabel(newLabel)
      }
      state.oldLabel = device.label
    }
}

/*
private Integer getGroupAddrFromBindingTable(description) 
{
   log.info "Parsing binding table - '$description'"
   def btr = zigbee.parseBindingTableResponse(description)
   def groupEntry = btr?.table_entries?.find { it.dstAddrMode == 1 }
   if (groupEntry != null) 
   {
      log.info "Found group binding in the binding table: ${groupEntry}"
      Integer.parseInt(groupEntry.dstAddr, 16)
   } 
   else 
   {
      log.info "The binding table does not contain a group binding"
      null
    }
}

private List addHubToGroup(Integer groupAddr) 
{
   ["st cmd 0x0000 0x01 ${CLUSTER_GROUPS} 0x00 {${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr,4))} 00}","delay 200"]
}

private List readDeviceBindingTable() 
{
   ["zdo mgmt-bind 0x${device.deviceNetworkId} 0","delay 200"]
}
*/