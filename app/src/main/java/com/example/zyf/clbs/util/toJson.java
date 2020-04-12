package com.example.zyf.clbs.util;

import com.example.zyf.clbs.entity.Location;
import com.google.gson.Gson;

public class toJson {
    //将location对象转换成json字符串
    public static String locationToJson(Location point){
        return new Gson().toJson(point);
    }
}
