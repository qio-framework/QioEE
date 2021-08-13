<img src="http://qioframework.io/media/qio.png" width="290px" />

<img src="http://qioframework.io/media/qio-screen.png" width="390px"/>

Qio is a full-fledged Java Web Framework. 
It is built around the Java EE specification. 

## Getting Started

1. Download the starter application

```
git clone https://github.com/qio-framework/QioJavaEEStarter.git
```

2. then run:

```
mvn package jetty:run
```

3. finally browse to : 

```
http://localhost:3000/q
```



### What a HttpHandler looks like:

```
package dev.web;

import com.google.gson.Gson;
import qio.annotate.HttpHandler;
import qio.annotate.JsonOutput;
import qio.annotate.verbs.Get;
import qio.model.web.ResponseData;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@HttpHandler
public class HelloHandler {

    Gson gson = new Gson();

    @JsonOutput
    @Get("/")
    public String hi(HttpServletRequest request,
                     HttpServletResponse response,
                     ResponseData data){
        Map<String, String> output = new HashMap<>();
        output.put("message", "java is great! thank you sun, " +
                "oracle, mysql, h2 and grails!");
        return gson.toJson(output);
    }

}
```

For a walk through of a sample Todo application visit www.qioframework.io.


built by zqo
