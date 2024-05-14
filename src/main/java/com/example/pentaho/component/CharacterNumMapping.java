package com.example.pentaho.component;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CharacterNumMapping {

    public List<Map<String,Integer>> characterMapping = new ArrayList(){{
       HashMap<String, Integer> mapB = new HashMap<>();
       mapB.put("B",1);
       HashMap<String, Integer> mapC = new HashMap<>();
       mapC.put("C",2);
       HashMap<String, Integer> mapD = new  HashMap<String,Integer>();
       mapD.put("D",3);
       HashMap<String, Integer> mapF = new  HashMap<String,Integer>();
       mapF.put("F",4);
       HashMap<String, Integer> mapG = new  HashMap<String,Integer>();
       mapG.put("G",5);
       HashMap<String, Integer> mapH = new  HashMap<String,Integer>();
       mapH.put("H",6);
       HashMap<String, Integer> mapJ = new  HashMap<String,Integer>();
       mapJ.put("J",7);
       HashMap<String, Integer> mapK = new  HashMap<String,Integer>();
       mapK.put("K",8);
       HashMap<String, Integer> mapL = new  HashMap<String,Integer>();
       mapL.put("L",9);
       HashMap<String, Integer> mapM = new  HashMap<String,Integer>();
       mapM.put("M",10);
       HashMap<String, Integer> mapN = new  HashMap<String,Integer>();
       mapN.put("N",11);
       HashMap<String, Integer> mapP = new  HashMap<String,Integer>();
       mapP.put("P",12);
       HashMap<String, Integer> mapQ = new  HashMap<String,Integer>();
       mapQ.put("Q",13);
       HashMap<String, Integer> mapR = new  HashMap<String,Integer>();
       mapR.put("R",14);
       HashMap<String, Integer> mapS = new  HashMap<String,Integer>();
       mapS.put("S",15);
       HashMap<String, Integer> mapT = new  HashMap<String,Integer>();
       mapT.put("T",16);
       HashMap<String, Integer> mapV = new  HashMap<String,Integer>();
       mapV.put("V",17);
       HashMap<String, Integer> mapW = new  HashMap<String,Integer>();
       mapW.put("W",18);
       HashMap<String, Integer> mapX = new  HashMap<String,Integer>();
       mapX.put("X",19);
       HashMap<String, Integer> mapY = new  HashMap<String,Integer>();
       mapY.put("Y",20);
       HashMap<String, Integer> mapZ = new  HashMap<String,Integer>();
       mapZ.put("Z",21);

        add(mapB);
        add(mapC);
        add(mapD);
        add(mapF);
        add(mapG);
        add(mapH);
        add(mapJ);
        add(mapK);
        add(mapL);
        add(mapM);
        add(mapN);
        add(mapP);
        add(mapQ);
        add(mapR);
        add(mapS);
        add(mapT);
        add(mapV);
        add(mapW);
        add(mapX);
        add(mapY);
        add(mapZ);
    }};
    public List<Map<String,Integer>> numMapping = new ArrayList(){{
        HashMap<String, Integer> map0 = new HashMap<>();
        map0.put("0",0);
        HashMap<String, Integer> map1 = new HashMap<>();
        map1.put("1",1);
        HashMap<String, Integer> map2 = new HashMap<>();
        map2.put("2",2);
        HashMap<String, Integer> map3 = new HashMap<>();
        map3.put("3",3);
        HashMap<String, Integer> map4 = new HashMap<>();
        map4.put("4",5);
        HashMap<String, Integer> map5 = new HashMap<>();
        map5.put("5",6);
        HashMap<String, Integer> map6 = new HashMap<>();
        map6.put("6",7);
        HashMap<String, Integer> map7 = new HashMap<>();
        map7.put("7",8);
        HashMap<String, Integer> map8 = new HashMap<>();
        map8.put("8",9);
        HashMap<String, Integer> map9 = new HashMap<>();
        map9.put("9",0);
        
        
        add(map0);
        add(map1);
        add(map2);
        add(map3);
        add(map4);
        add(map5);
        add(map6);
        add(map7);
        add(map8);
        add(map9);
    }} ;

    public List<Map<String, Integer>> getCharacterMapping() {
        return characterMapping;
    }

    public void setCharacterMapping(List<Map<String, Integer>> characterMapping) {
        this.characterMapping = characterMapping;
    }

    public List<Map<String, Integer>> getNumMapping() {
        return numMapping;
    }

    public void setNumMapping(List<Map<String, Integer>> numMapping) {
        this.numMapping = numMapping;
    }
}
