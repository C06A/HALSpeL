package hal.spel.resl;

import com.github.kittinunf.fuel.core.Headers;
import hal.spel.Answer;
import hal.spel.HalKt;
import hal.spel.Link;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ipDashApi {
    static public void main(String... args) {
        Link entrance = HalKt.halSpeL("http://ip-api.com{/format}{/query}{?fields,lang}"
                , null, true, "entry");

        Map<String, Object> entrancePlaceholders = new HashMap();
        entrancePlaceholders.put("format", "json");
        Headers jsonHeaders = new Headers();
        jsonHeaders.append("Accept", "application/json");
        Answer answer = entrance.GET(entrancePlaceholders, jsonHeaders);
        System.out.println("URL: " + answer.getRequest().getUrl());
        System.out.println(answer.getStatus().getCode());
        System.out.println(answer.getBody().invoke());
        System.out.println(answer.getBody().get("query"));

        List<String> fieldList = new LinkedList();
        fieldList.add("status");
        fieldList.add("query");
        fieldList.add("message");
        fieldList.add("continent");
        fieldList.add("continentCode");
        fieldList.add("country");
        fieldList.add("countryCode");
        fieldList.add("region");
        fieldList.add("regionName");
        fieldList.add("city");
        fieldList.add("district");
        fieldList.add("zip");
        fieldList.add("lat");
        fieldList.add("lon");
        fieldList.add("timezone");
        fieldList.add("currency");
        fieldList.add("isp");
        fieldList.add("org");
        fieldList.add("as");
        fieldList.add("asname");
        fieldList.add("reverse");
        fieldList.add("mobile");
        fieldList.add("proxy");
        fieldList.add("hosting");

        entrancePlaceholders.put("fields", fieldList);
        answer = entrance.GET(entrancePlaceholders, jsonHeaders);
        System.out.println("URL: " + answer.getRequest().getUrl());
        System.out.println(answer.getStatus().getCode());
        System.out.println(answer.getBody().invoke());
        System.out.println(answer.getBody().get("query"));

        entrancePlaceholders.put("fields", fieldList.subList(1, 9));
        entrancePlaceholders.put("lang", "ru");
        answer = entrance.GET(entrancePlaceholders, jsonHeaders);
        System.out.println("URL: " + answer.getRequest().getUrl());
        System.out.println(answer.getStatus().getCode());
        System.out.println(answer.getBody().invoke());
        System.out.println(answer.getBody().get("query"));

        entrancePlaceholders.clear();
        entrancePlaceholders.put("format", "json");
        entrancePlaceholders.put("query", "apple.com");
        answer = entrance.GET(entrancePlaceholders, jsonHeaders);
        System.out.println("URL: " + answer.getRequest().getUrl());
        System.out.println(answer.getStatus().getCode());
        System.out.println(answer.getBody().invoke());
        System.out.println(answer.getBody().get("query"));

        entrancePlaceholders.put("query", "8.8.8.8");
        answer = entrance.GET(entrancePlaceholders, jsonHeaders);
        System.out.println("URL: " + answer.getRequest().getUrl());
        System.out.println(answer.getStatus().getCode());
        System.out.println(answer.getBody().invoke());
        System.out.println(answer.getBody().get("query"));

        entrancePlaceholders.clear();
        entrancePlaceholders.put("format", "xml");
        answer = entrance.GET(entrancePlaceholders, jsonHeaders);
        System.out.println("URL: " + answer.getRequest().getUrl());
        System.out.println(answer.getStatus().getCode());
        System.out.println(answer.getResult());

        entrancePlaceholders.put("format", "csv");
        answer = entrance.GET(entrancePlaceholders, jsonHeaders);
        System.out.println("URL: " + answer.getRequest().getUrl());
        System.out.println(answer.getStatus().getCode());
        System.out.println(answer.getResult());
    }
}
