﻿/**
 *	Copyright 2015 SmartThings
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
	definition (name: "Dawon Z-Wave Metering Switch", namespace: "JayN", author: "JayN", ocfDeviceType: "oic.d.switch", runLocally: true, minHubCoreVersion: '000.017.0012', executeCommandsLocally: false, genericHandler: "Z-Wave") {
		capability "Energy Meter"
		capability "Actuator"
		capability "Switch"
		capability "Power Meter"
		capability "Refresh"
		capability "Configuration"
		capability "Sensor"
		capability "Light"
		capability "Health Check"

		command "reset"

		fingerprint mfr: "018C", prod: "0042", model: "0005", deviceJoinName: "Dawon Smart Plug", ocfDeviceType: "oic.d.smartplug"
		fingerprint mfr: "018C", prod: "0042", model: "0008", deviceJoinName: "Dawon Smart Multitab", ocfDeviceType: "oic.d.smartplug"
		
	}

	// simulator metadata
	simulator {
		status "on":  "command: 2003, payload: FF"
		status "off": "command: 2003, payload: 00"

		for (int i = 0; i <= 10000; i += 1000) {
			status "power  ${i} W": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 4, scale: 2, size: 4).incomingMessage()
		}
		for (int i = 0; i <= 100; i += 10) {
			status "energy	${i} kWh": new physicalgraph.zwave.Zwave().meterV1.meterReport(
				scaledMeterValue: i, precision: 3, meterType: 0, scale: 0, size: 4).incomingMessage()
		}

		// reply messages
		reply "2001FF,delay 100,2502": "command: 2503, payload: FF"
		reply "200100,delay 100,2502": "command: 2503, payload: 00"
	}

    preferences {
        input name: "resetKWH", title:"Reset Energy(KWH)(초기화 주의!)", type:"enum", options: ["Yes", "No"], description: "에너지 소비량을 초기화합니다.(KWH)", required: true, defaultValue: "No"
    }
    
	// tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState("on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState:"turningOff")
				attributeState("off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState:"turningOn")
				attributeState("turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff")
				attributeState("turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn")
			}
		}
		valueTile("power", "device.power", width: 2, height: 2) {
			state "default", label:'${currentValue} W'
		}
		valueTile("energy", "device.energy", width: 2, height: 2) {
			state "default", label:'${currentValue} kWh'
		}
		standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'reset kWh', action:"reset"
		}
		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main(["switch","power","energy"])
		details(["switch","power","energy","refresh","reset"])
	}
}

def installed() {
	log.debug "installed()"
	// Device-Watch simply pings if no device events received for 32min(checkInterval)
	initialize()
	
	//if (zwaveInfo?.mfr?.equals("0063") || zwaveInfo?.mfr?.equals("014F")) { // These old GE devices have to be polled. GoControl Plug refresh status every 15 min.
	//	runEvery15Minutes("poll", [forceForLocallyExecuting: true])
	//}
}

def updated() {
	// Device-Watch simply pings if no device events received for 32min(checkInterval)
	log.debug "updated()"
	initialize()
	
	//에너지 소비량을 초기화 한다.
    if (resetKWH == "Yes") {
        log.debug "에너지 소비량(Energy(KWH))을 초기화 합니다."
        reset()
    }
    
	try {
		if (!state.MSR) {
			response(zwave.manufacturerSpecificV2.manufacturerSpecificGet().format())
		}
	} catch (e) {
		log.debug e
	}
}

def initialize() {
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
	
	if ((zwaveInfo.mfr == "018C" && zwaveInfo.prod == "0042") && (zwaveInfo.model == "0005" || zwaveInfo.model == "0008")) {   //Dawon Smart Plug추가
        log.debug "Set schdule : runEvery1Minute(poll)"
	    unschedule()
        runEvery1Minute(poll)
    }
}

def getCommandClassVersions() {
	[
		0x20: 1,  // Basic
		0x32: 3,  // Meter
		0x56: 1,  // Crc16Encap
		0x70: 1,  // Configuration
		0x72: 2,  // ManufacturerSpecific
	]
}

// parse events into attributes
def parse(String description) {
	log.debug "parse() - description: "+description
	def result = null
	if (description != "updated") {
		def cmd = zwave.parse(description, commandClassVersions)
		if (cmd) {
			result = zwaveEvent(cmd)
			log.debug("'$description' parsed to $result")
		} else {
			log.debug("Couldn't zwave.parse '$description'")
		}
	}
	result
}

def handleMeterReport(cmd){
	if (cmd.meterType == 1) {
		if (cmd.scale == 0) {
			createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kWh")
		} else if (cmd.scale == 1) {
			createEvent(name: "energy", value: cmd.scaledMeterValue, unit: "kVAh")
		} else if (cmd.scale == 2) {
			createEvent(name: "power", value: Math.round(cmd.scaledMeterValue), unit: "W")
		}
	}
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd) {
	log.debug "v3 Meter report: "+cmd
	handleMeterReport(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd)
{
	log.debug "Basic report: "+cmd
	def value = (cmd.value ? "on" : "off")
	def evt = createEvent(name: "switch", value: value, type: "physical", descriptionText: "$device.displayName was turned $value")
	if (evt.isStateChange) {
		[evt, response(["delay 3000", meterGet(scale: 2).format()])]
	} else {
		evt
	}
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd)
{
	log.debug "Switch binary report: "+cmd
	def value = (cmd.value ? "on" : "off")
	createEvent(name: "switch", value: value, type: "digital", descriptionText: "$device.displayName was turned $value")
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	def result = []

	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	log.debug "msr: $msr"
	updateDataValue("MSR", msr)

	result << createEvent(descriptionText: "$device.displayName MSR: $msr", isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd){
    if ((cmd.notificationType == 0x08) && zwaveInfo?.mfr?.equals("018C")) {
        if (cmd.event == 0x02) {
            createEvent(name: "switch", value: "off")
        } else if (cmd.event == 0x03) {
            createEvent(name: "switch", value: "on")
        }
    }
}
def zwaveEvent(physicalgraph.zwave.Command cmd) {
	log.debug "${device.displayName}: Unhandled: $cmd"
	[:]
}

def on() {
	encapSequence([
		zwave.basicV1.basicSet(value: 0xFF),
		zwave.switchBinaryV1.switchBinaryGet(),
		meterGet(scale: 2)
	], 3000)
}

def off() {
	encapSequence([
		zwave.basicV1.basicSet(value: 0x00),
		zwave.switchBinaryV1.switchBinaryGet(),
		meterGet(scale: 2)
	], 3000)
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	log.debug "ping()"
	refresh()
}

def poll() {
	log.debug "poll()"
	sendHubCommand(refresh())
}

def refresh() {
	log.debug "refresh()"
	
    /* 로그에 energy, power, energy 순으로 연속 3개 나온다.
    encapSequence([
		zwave.switchBinaryV1.switchBinaryGet(),
		meterGet(scale: 0), //에너지 소비량(KWH) 
		meterGet(scale: 2)  //소비전력
	])
    */
    // 밑의 하나를 삭제하니 energy, power 연속 두가지만 나온다.
    encapSequence([
		//zwave.switchBinaryV1.switchBinaryGet(),
        meterGet(scale: 0), //에너지 소비량(KWH)
        meterGet(scale: 2)  //소비전력
	])
}

def configure() {
	log.debug "configure()"
	def result = []

	log.debug "Configure zwaveInfo: "+zwaveInfo
        
	/*
	if (zwaveInfo.mfr == "0086") {	// Aeon Labs meter
		result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 80, size: 1, scaledConfigurationValue: 2)))	// basic report cc
		result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 12)))	// report power in watts
		result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 300)))	 // every 5 min
	} else if (zwaveInfo.mfr == "010F" && zwaveInfo.prod == "1801" && zwaveInfo.model == "1000") { // Fibaro Wall Plug UK
		result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 11, size: 1, scaledConfigurationValue: 2))) // 2% power change results in report
		result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 13, size: 2, scaledConfigurationValue: 5*60))) // report every 5 minutes
	} else if (zwaveInfo.mfr == "014F" && zwaveInfo.prod == "5053" && zwaveInfo.model == "3531") {
		result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 13, size: 2, scaledConfigurationValue: 15))) //report kWH every 15 min
	} else if (zwaveInfo.mfr == "0154" && zwaveInfo.prod == "0003" && zwaveInfo.model == "000A") {
		result << response(encap(zwave.configurationV1.configurationSet(parameterNumber: 25, size: 1, scaledConfigurationValue: 1))) //report every 1W change
	}
	*/
	result << response(encap(meterGet(scale: 0)))
	result << response(encap(meterGet(scale: 2)))
	result
}

def reset() {
    log.debug "reset()"
    encapSequence([
		meterReset(),
		meterGet(scale: 0)
	])
}

def meterGet(map)
{
    return zwave.meterV2.meterGet(map)
}

def meterReset()
{
	return zwave.meterV2.meterReset()
}

/*
 * Security encapsulation support:
 */
def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(commandClassVersions)
	if (encapsulatedCommand) {
		log.debug "Parsed SecurityMessageEncapsulation into: ${encapsulatedCommand}"
		zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Unable to extract Secure command from $cmd"
	}
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	def version = commandClassVersions[cmd.commandClass as Integer]
	def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
	def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
	if (encapsulatedCommand) {
		log.debug "Parsed Crc16Encap into: ${encapsulatedCommand}"
		zwaveEvent(encapsulatedCommand)
	} else {
		log.warn "Unable to extract CRC16 command from $cmd"
	}
}

private secEncap(physicalgraph.zwave.Command cmd) {
	log.debug "encapsulating command using Secure Encapsulation, command: $cmd"
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
	log.debug "encapsulating command using CRC16 Encapsulation, command: $cmd"
	zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

private encap(physicalgraph.zwave.Command cmd) {
	if (zwaveInfo?.zw?.contains("s")) {
		secEncap(cmd)
	} else if (zwaveInfo?.cc?.contains("56")){
		crcEncap(cmd)
	} else {
		log.debug "no encapsulation supported for command: $cmd"
		cmd.format()
	}
}

private encapSequence(cmds, Integer delay=250) {
	delayBetween(cmds.collect{ encap(it) }, delay)
}