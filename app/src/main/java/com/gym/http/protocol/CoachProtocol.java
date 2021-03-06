package com.gym.http.protocol;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;
import com.gym.bean.CoachBean;
import com.gym.bean.SpaceBean;
import com.gym.utils.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Administrator on 2015/9/8 0008.
 */
public class CoachProtocol extends BaseProtocol<HashMap<String,Object>>{
    private HashMap<String, String> hashMap;
    public CoachProtocol(HashMap<String, String> hashMap) {
        this.hashMap=hashMap;
    }

    @Override
    protected String getParames() {
        return wrapParames(POST,hashMap);
    }

    @Override
    protected HashMap<String,Object> parseFromJson(String json, String url) {
        HashMap<String,Object> hashMap=new HashMap<>();
        if(!TextUtils.isEmpty(json)){
            try {
                JSONObject obj=new JSONObject(json);
                String result=obj.optString("result");
                if("1".equals(result)){
                    String data=obj.optString("data");
                    ArrayList<CoachBean> list=getGson().fromJson(data, new TypeToken<ArrayList<CoachBean>>() {
                    }.getType());
                    String totalPage=obj.optString("totalPageCount");
                    hashMap.put("list",list);
                    hashMap.put("totalPage",totalPage);
                   return hashMap;
                }else {
                    hashMap.put("list",new ArrayList<>());
                    hashMap.put("totalPage", "1");
                    return hashMap;
                }
            } catch (JSONException e) {
                LogUtils.e(e);
                return null;
            }
        }
        return null;
    }
}
