metadata {
	definition (name: "Geekbes Smart Switch", namespace: "tuya", author: "traub") {
        capability "Switch"
        capability "Refresh"
		capability "Polling"

	}

	preferences {
		input("tuyaEmail", "email", title:"Tuya user email", description: "Please enter your Tuya registered email", required: true, displayDuringSetup: true)
		input("tuyaPassword", "password", title:"Tuya password", description: "Please enter your Tuya password", required: true, displayDuringSetup: true)
		input("tuyaDeviceId", "string", title:"Tuya Device ID", description: "With the device displayed in the Tuya App click the menu button then Device Info", displayDuringSetup: true)
	}
	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 3, height: 2, canChangeIcon: true) {
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			state "on", label: '${name}', action: "switch.off", backgroundColor: "#79b821",icon: "st.switches.switch.on"
		}
		standardTile("refresh", "device.refresh", width: 1, height: 1, inactiveLabel: false, decoration: "flat") {
			state "default", action:"refresh", icon:"st.secondary.refresh-icon"
		}
		main "switch"
			details (["switch","refresh"])
	}
}
def on() {
	log.debug "Turn On"
    setDeviceStatus(performLogin(), "true")
    sendEvent(name: "switch", value: "on")
}

def off() {
	log.debug "Turn off"
    setDeviceStatus(performLogin(), "false")
    sendEvent(name: "switch", value: "off")
}

def poll() {
	log.debug "Start Poll"
	refresh()
    log.debug "End Poll"
}

def refresh() {
    log.debug "Start Refresh"
    if(getCurrentStatus(performLogin(), "1"))
    {
    	sendEvent(name: "switch", value: "on")
    }else{
    	sendEvent(name: "switch", value: "off")
    }
    log.debug "End Refresh"
}
def Boolean setDeviceStatus(String sidString, String status)
{
    def postDataString = "postData={\"devId\": \"" + tuyaDeviceId + "\",\"dps\":{\"1\": "+status+"}}"
    
    def queryString = createQueryString(getApiNameString("setDeviceStatus"), postDataString, sidString)
        
	def uriString = "https://a1.tuyaus.com/api.json"
	
    def successString = ""
    try {
    	httpPost(uriString, queryString) { resp ->
            successString = resp.data.success
    	}
	} catch (e) {
    	log.debug "Unable to set device status"
	}
    if(successString.equals("true"))
    {
    	return true
    }else{
    	return false
    }
}
def Boolean getCurrentStatus(String sidString, String key)
{
 	def apiNameString = "a=tuya.cloud.device.get"

	def postDataString = "postData={\"devId\": \"" + tuyaDeviceId + "\"}"
    
    def queryString = createQueryString(getApiNameString("getCurrentStatus"), postDataString, sidString)
        
	def uriString = "https://a1.tuyaus.com/api.json"
	def String dpsResultString = ""
    def String debugString
    try {
    	httpPost(uriString, queryString) { resp ->
        debugString = resp.data
        	log.debug debugString
            dpsResultString = resp.data.result.dps
    	}
	} catch (e) {
    	log.debug "Unable to get device status"
	}
    def keyValue = ""
    def dpsStrings = dpsResultString.replace("{","").replace("}","").split(",")
    for (int i = 0; i < dpsStrings.size(); i++)
    {
    	if(dpsStrings[i].split("=")[0].equals(key))
        {
        	keyValue = dpsStrings[i].split("=")[1]
            break
        }
    }
    if(keyValue.equals("true"))
    {
    	return true
    }else{
  		return false
    }
}
def String performLogin()
{
    def countryCodeString = "{\"countryCode\":\"US\","
    def emailString = "\"email\":\""+ tuyaEmail +"\","
    def pwdString = "\"passwd\":\"" + tuyaPassword.encodeAsMD5() + "\",}"

	def postDataString = "postData=" + countryCodeString + emailString + pwdString
    
    def queryString = createQueryString(getApiNameString("performLogin"), postDataString, "")
        
	def uriString = "https://a1.tuyaus.com/api.json"
    
    def returnSid = ""
    try {
    	httpPost(uriString, queryString) { resp ->
            returnSid = resp.data.result.sid
    	}
	} catch (e) {
    	log.debug "Unable to login to Tuya"
	}
    return "sid=" + returnSid
}
def String createQueryHashString(String apiNameString, String timeString, String postDataString, String sidString)
{
	def stringToSign = "qouytrhkgxejhur83jfkq8buqnkd8p9r" 
    	stringToSign += apiNameString 
        stringToSign += "|" 
        stringToSign += getClientID() 
        stringToSign += "|"
        stringToSign += getLangString()
        stringToSign += "|"
        stringToSign += getOSString()
        stringToSign += "|"
        stringToSign += postDataString
        stringToSign += "|"
        if(!sidString.equals(""))
        {
        	stringToSign += sidString
        	stringToSign += "|"
        }
        stringToSign += timeString
        stringToSign += "|"
        stringToSign += getApiVersionString()

	def hashString = stringToSign.encodeAsMD5()
    
    def queryStringHash = "sign=" + hashString
    
    return queryStringHash
}
def String createQueryString(String apiNameString, String postDataString, String sidString)
{
	def seconds = new Date().getTime()/1000
    def timeString = "time=" + seconds.intValue().toString()
    
	def queryString = apiNameString
    	queryString += "&"
        if(!sidString.equals(""))
        {
        	queryString += sidString
        	queryString += "&"
        }
        queryString += timeString
        queryString += "&"
        queryString += getLangString()
        queryString += "&"
        queryString += getApiVersionString()
        queryString += "&"
        queryString += getOSString()
        queryString += "&"
        queryString += getClientID()
        queryString += "&"
        queryString += createQueryHashString(apiNameString, timeString, postDataString, sidString)
        queryString += "&"
        queryString += postDataString
    
    	return queryString
}
def String getApiNameString(String function)
{
	def apiNameString = ""
	if(function.equals("performLogin"))
    {
    	apiNameString = "a=tuya.m.user.email.password.login"
    }else if(function.equals("setDeviceStatus")){
    	apiNameString = "a=tuya.cloud.device.dp.publish"
    }else if(function.equals("getCurrentStatus")){
    	apiNameString = "a=tuya.cloud.device.get"
    }
    return apiNameString
}
def String getClientID()
{
	return "clientId=98u9ft5jh2hwdkjhikut"
}
def String getLangString()
{
	return "lang=en"
}
def String getOSString()
{
	return "os=centOS-6"
}
def String getApiVersionString()
{
	return "v=1.0"
}