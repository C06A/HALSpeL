package hal.spel.resl;

import com.github.kittinunf.fuel.core.Headers;
import hal.spel.Answer;
import hal.spel.HalKt;
import hal.spel.Link;

import java.util.HashMap;
import java.util.Map;

public class IpApi {
    static public void main(String... args) {
        Link entrance = HalKt.halSpeL("https://ipapi.co{/ip,field}", null, true, "entry");

        Map<String, String> entrancePlaceholders = new HashMap();
        entrancePlaceholders.put("ip", "8.8.8.8");
        entrancePlaceholders.put("field", "json");
        Headers jsonHeaders = new Headers();
        jsonHeaders.append("Accept", "application/json");
        Answer answer = entrance.GET(entrancePlaceholders, jsonHeaders);
        System.out.println("URL: " + answer.getRequest().getUrl());
        System.out.println(answer.getStatus().getCode());
        if (answer.getBody() != null) {
            System.out.println(answer.getBody().invoke());
            System.out.println(answer.getBody().get("ip"));
        }

        entrancePlaceholders.put("ip", "1.2.3.4");
        answer = entrance.GET(entrancePlaceholders, jsonHeaders);
        System.out.println("URL: " + answer.getRequest().getUrl());
        System.out.println(answer.getStatus().getCode());
        if (answer.getBody() != null) {
            System.out.println(answer.getBody().invoke());
            System.out.println(answer.getBody().get("ip"));
        }
    }
}
