package com.example.pentaho.component;


import org.springframework.context.annotation.Profile;

public class PentahoWebService  {

    /** 呼叫PentahoRepository上的.ktr */
    public final static String executeTrans= "/kettle/executeTrans/?rep=&trans=/home/ubuntu/samples/enableWebService.ktr&";

    public final static String executeJobs= "/kettle/executeJob/?rep=%s&job=%s&level=Debug&";

    public final static String jobStatusById ="/kettle/jobStatus/?name=&xml=Y&";
    /** 本地測試執行kjb */
    public final static String simpleExecuteJob ="/kettle/executeJob/?job=C:\\Users\\2212009\\Desktop\\moi\\ppppp\\pdi-ce-9.4.0.0-343\\data-integration\\jobIdTest.kjb&level=Debug";



}
