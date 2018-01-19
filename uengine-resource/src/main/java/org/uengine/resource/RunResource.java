package org.uengine.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.MapConfiguration;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.uengine.iam.util.FileUtils;
import org.uengine.iam.util.JsonUtils;
import org.uengine.iam.util.StringUtils;
import org.uengine.resource.templates.MustacheTemplateEngine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by uengine on 2018. 1. 16..
 */
public class RunResource {
    public static void main(String[] args) throws Exception {
        String baseDir = args[0];
        System.out.println(baseDir);

        File file = new File(baseDir + "/config.yml");
        String configYml = new String(Files.readAllBytes(Paths.get(file.getPath())));
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Map configMap = yamlReader.readValue(configYml, Map.class);

        File propTemplateFile = new File(baseDir + "/config.properties.yml.template");
        String propTemplateString = new String(Files.readAllBytes(Paths.get(propTemplateFile.getPath())));
        MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();
        String body = templateEngine.executeTemplateText(propTemplateString, configMap);


        //클라우드 패키지 배포 프로퍼티스를 생성한다.
        org.apache.commons.io.FileUtils.writeStringToFile(
                new File(baseDir + "/config.properties.yml"),
                body,
                "UTF-8"
        );

        Map serverPrivate = (Map) ((Map) configMap.get("server")).get("private");

        //Map gracefullyRemoveAgents = (Map) serverPrivate.get("gracefully-remove-agent");
        //Map serverPublic = (Map) ((Map) configMap.get("server")).get("public");


        //etc-host 파일을 생성한다.
        Map hosts = new HashMap();
        String[] baseHosts = new String[]{"bootstrap", "gitlab", "ci", "public"};
        for (String baseHost : baseHosts) {
            if (serverPrivate.containsKey(baseHost)) {
                try {
                    hosts.put(serverPrivate.get(baseHost).toString(), baseHost);
                } catch (Exception ex) {

                }
            }
        }

        String[] nodeHosts = new String[]{"master", "agent", "add-agent"};
        for (String nodeHost : nodeHosts) {
            try {
                Map nodes = (Map) serverPrivate.get(nodeHost);
                addHosts(hosts, nodes);
            } catch (Exception ex) {

            }
        }

        //hosts 맵을 변환
        StringBuilder builder = new StringBuilder();
        builder.append("127.0.0.1   localhost localhost.localdomain localhost4 localhost4.localdomain4\n" +
                "::1         localhost localhost.localdomain localhost6 localhost6.localdomain6\n\n\n");
        List<String> ips = new ArrayList<>(hosts.keySet());
        if (!ips.isEmpty()) {
            for (String ip : ips) {
                builder.append(ip + " " + hosts.get(ip).toString() + "\n");
            }
        }

        org.apache.commons.io.FileUtils.writeStringToFile(
                new File(baseDir + "/install-files/hosts"),
                builder.toString(),
                "UTF-8"
        );

        //앤시블 호스트 파일을 생성한다.
        String[] ansibleHosts = new String[]{"master", "agent", "add-agent", "gracefully-remove-agent", "uninstall"};
        for (String ansibleHost : ansibleHosts) {
            try {
                Map nodes = (Map) serverPrivate.get(ansibleHost);
                addAnsibleIps(configMap, nodes, ansibleHost);
            } catch (Exception ex) {

            }
        }
        File ansibleHostTemplateFile = new File(baseDir + "/install-files/ansible-hosts.yml.template");
        String ansibleHostTemplateString = new String(Files.readAllBytes(Paths.get(ansibleHostTemplateFile.getPath())));
        String ansibleHostBody = templateEngine.executeTemplateText(ansibleHostTemplateString, configMap);
        org.apache.commons.io.FileUtils.writeStringToFile(
                new File(baseDir + "/install-files/ansible-hosts.yml"),
                ansibleHostBody,
                "UTF-8"
        );


        //dcos 클러스터 yml 을 작성한다.
        Map cluseterMap = new HashMap();
        cluseterMap.put("agent_list", getCluseterIpList((Map) serverPrivate.get("agent")));

        cluseterMap.put("bootstrap_url", "file:///opt/dcos_install_tmp");
        cluseterMap.put("cluster_name", configMap.get("cluster_name").toString());
        cluseterMap.put("exhibitor_storage_backend", "static");
        cluseterMap.put("master_discovery", "static");
        cluseterMap.put("ip_detect_public_filename", "genconf/ip-detect");
        cluseterMap.put("ip_detect_path", "genconf/ip-detect");
        cluseterMap.put("master_list", getCluseterIpList((Map) serverPrivate.get("master")));
        cluseterMap.put("process_timeout", 10000);

        ArrayList<String> list = new ArrayList<>();
        list.add(serverPrivate.get("public").toString());
        cluseterMap.put("public_agent_list", list);

        cluseterMap.put("resolvers", configMap.get("resolvers"));

        try {
            if (configMap.containsKey("dns_search") && !StringUtils.isEmpty(configMap.get("dns_search").toString())) {
                cluseterMap.put("dns_search", configMap.get("dns_search"));
            }
        } catch (Exception ex) {

        }
        cluseterMap.put("ssh_key_path", "genconf/ssh_key");
        cluseterMap.put("ssh_port", configMap.get("ssh_port"));
        cluseterMap.put("ssh_user", configMap.get("ansible_user"));
        cluseterMap.put("telemetry_enabled", true);
        cluseterMap.put("oauth_enabled", true);

        String cluseteryml = yamlReader.writeValueAsString(cluseterMap);
        org.apache.commons.io.FileUtils.writeStringToFile(
                new File(baseDir + "/install-files/genconf/config.yaml"),
                cluseteryml,
                "UTF-8"
        );
    }

    private static List<String> getCluseterIpList(Map source) {
        List<String> ips = new ArrayList<>();
        Iterator iterator = source.keySet().iterator();
        while (iterator.hasNext()) {
            String nodeName = (String) iterator.next();
            ips.add(source.get(nodeName).toString());
        }
        return ips;
    }

    private static void addAnsibleIps(Map configMap, Map source, String ansibleHost) {
        Iterator iterator = source.keySet().iterator();
        StringBuilder builder = new StringBuilder();
        while (iterator.hasNext()) {
            String nodeName = (String) iterator.next();
            builder.append(source.get(nodeName) + "\n");
        }
        configMap.put(ansibleHost, builder.toString());
    }

    private static void addHosts(Map hosts, Map source) {

        Iterator iterator = source.keySet().iterator();
        while (iterator.hasNext()) {
            String nodeName = (String) iterator.next();
            hosts.put(source.get(nodeName).toString(), nodeName);
        }

    }
}
