package com.example.pentaho.component;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pentaho")
public class PentahoComponent {

    private String webTarget;

    private String userName;

    private String password;

    private String repositoryName;

    private String jobPath;


    public String getWebTarget() {
        return webTarget;
    }

    public void setWebTarget(String webTarget) {
        this.webTarget = webTarget;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getJobPath() {
        return jobPath;
    }

    public void setJobPath(String jobPath) {
        this.jobPath = jobPath;
    }
}
