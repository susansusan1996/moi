package com.example.pentaho.component;

import org.springframework.stereotype.Component;

public class PentahoWebService  {

    public final static String executeTrans= "/kettle/executeTrans/?rep=PentahoRepository&trans=/home/addr/dev/ADDR_FULL_FLOW/1_PRE_PROCESS/test_20240216.ktr&";

    public final static String executeJobs= "/kettle/executeJobs/?rep=PentahoRepository&jobs=/home/addr/dev/ADDR_FULL_FLOW/1_PRE_PROCESS/test_20240216.ktr&";



}
