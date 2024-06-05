package com.example.pentaho.utils;
//
//import redis.clients.*;
//import redis.clients.jedis.Jedis;
//import redis.clients.jedis.JedisPool;
//import redis.clients.jedis.Pipeline;

public class JedisUtils {


//    private Jedis jedis = null;
//
//    private JedisPool jedisPool = null;
//
//    Pipeline pipeline = null;
//
//    //批次提交輛
//    int cache_size = 10000;
//
//    //當前數據緩存量
//    int cur_size = 0;
//
//    boolean first = true;
//
//
////    public boolean processRow(StepMetaInterFace smi,StepDataInterFace sdi) throws KettleException {
////    }
//
//    public boolean processRow() {
//        if(first){
//            first = false;
//            //connect to redis server
//            String redis_ip = "34.211.215.66";
//            String redis_port = "8005";
//            String redis_pwd="1qaz@WSX";
////          logBasic("connect to:"+redis_ip+":"+redis_port);
//
//
//            //
//            jedis =  new Jedis(redis_port,Integer.valueOf(redis_port));
//            jedis.auth(redis_pwd);
//            jedis.select(4);
//
//            //
//            pipeline = jedis.pipelined();
////          logBasic("server is running:"+jedis.ping());
//        }
//              pipeline.sadd("","")
//    }


}
