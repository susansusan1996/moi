package com.example.pentaho.component;


public class PentahoWebService  {

    public final static String executeTrans= "/kettle/executeTrans/?rep=&trans=/home/ubuntu/samples/enableWebService.ktr&";
    public final static String executeJobs= "/kettle/executeJob/?rep=PentahoRepository&job=/home/addr/ADDR_FULL_FLOW/MAIN.kjb&level=Debug&";

    public final static String jobStatusById ="/kettle/jobStatus/?name=&xml=Y&";
    public final static String simpleExecuteJob ="/kettle/executeJob/?job=C:\\Users\\2212009\\Desktop\\moi\\ppppp\\pdi-ce-9.4.0.0-343\\data-integration\\jobIdTest.kjb&level=Debug";


}
