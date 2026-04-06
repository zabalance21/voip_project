package sip;

import java.util.LinkedHashMap;
import java.util.Map;

public class SipMessage {
    public String startLine;
    public Map<String, String> headers = new LinkedHashMap<>();
    public String body;

    public String rawSIP(){
        StringBuilder sb = new StringBuilder();
        sb.append(startLine).append("\r\n");
        for(Map.Entry<String, String> e : headers.entrySet()){
            sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
        }
        sb.append("Content Length: ").append(body.getBytes().length).append("\r\n");
        sb.append("\r\n").append(body);
        return sb.toString();
    }

    public static SipMessage parse(String raw){
        SipMessage message = new SipMessage();
        String[] parts = raw.split("\r\n\r\n", 2);
        String[] lines = parts[0].split("\r\n");
        message.startLine = lines[0];

        for(int i = 0; i < lines.length; i++){
            int colon =  lines[i].indexOf(':');
            if(colon > 0){
                String key = lines[i].substring(0, colon).trim();
                String value = lines[i].substring(colon+1).trim();
                message.headers.put(key, value);
            }
        }
        if(parts.length > 1){message.body = parts[1];}
        return message;
    }

    public boolean isRequest(){return !startLine.startsWith("SIP");}

    public boolean isResponse(){return startLine.startsWith("SIP/2.0");}

    public int getStatusCode(){
        return isRequest() ? Integer.parseInt(startLine.split(" ")[1]) : -1;
    }

    public String getMethod(){
        return isRequest() ? startLine.split(" ")[0] : "";
    }



}