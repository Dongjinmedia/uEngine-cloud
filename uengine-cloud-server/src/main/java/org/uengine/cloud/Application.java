package org.uengine.cloud;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.uengine.cloud.app.AppAccessLevelRepository;
import org.uengine.cloud.app.AppEntity;
import org.uengine.cloud.tenant.TenantContext;
import org.uengine.iam.client.model.OauthUser;
import org.uengine.iam.util.HttpUtils;
import org.uengine.iam.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.uengine.cloud.app.DcosApi;
import org.uengine.cloud.scheduler.CronTable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by uengine on 2017. 10. 5..
 */
@SpringBootApplication
@RestController
@Configuration
@ComponentScan
@EnableEurekaClient
@EnableAutoConfiguration
@EnableScheduling
public class Application {

    @Autowired
    private CronTable cronTable;

    @Autowired
    private DcosApi dcosApi;

    @Autowired
    private AppAccessLevelRepository appAccessLevelRepository;

    //findAll

    @RequestMapping(value = "/fetchData", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public Map fetchData(HttpServletRequest request,
                         HttpServletResponse response
    ) throws Exception {
        Map dcosData = cronTable.getDcosData();

        //어세스 레벨 매핑
        OauthUser oauthUser = TenantContext.getThreadLocalInstance().getUser();
        List<AppEntity> appEntityList = appAccessLevelRepository.findAll();

        //어드민일 경우 전부 리턴, 아닐 경우 어세스레벨 별로 리턴
        if (oauthUser == null) {
            appEntityList = new ArrayList<>();
        } else if ("admin".equals(oauthUser.getMetaData().get("acl"))) {

        } else {
            List<AppEntity> list = new ArrayList<>();
            for (int i = 0; i < appEntityList.size(); i++) {
                if (appEntityList.get(i).getAccessLevel() > 0) {
                    list.add(appEntityList.get(i));
                }
            }
            appEntityList = list;
        }

        Map apps = new HashMap();
        for (int i = 0; i < appEntityList.size(); i++) {
            apps.put(appEntityList.get(i).getName(), appEntityList.get(i));
        }
        dcosData.put("devopsApps", apps);
        return dcosData;
    }

    @RequestMapping(value = "/refreshRoute", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    public void refreshRoute(HttpServletRequest request,
                             HttpServletResponse response
    ) throws Exception {
        dcosApi.refreshRoute();
        response.setStatus(200);
    }

    @RequestMapping("/health")
    public String health() {
        return "";
    }

    public static void main(String[] args) {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        new SpringApplicationBuilder(Application.class).web(true).run(args);
    }

}
