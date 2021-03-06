package com.sunnybear.library.network.callback;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.sunnybear.library.network.util.JsonUtils;
import com.sunnybear.library.util.Logger;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * FastJson解析json(反射方法获取泛型类型)
 * Created by guchenkai on 2016/5/18.
 */
public abstract class SerializableCallback<T extends Serializable> extends JsonCallback {
    private Class<? extends Serializable> clazz;

    public SerializableCallback(Context context) {
        super(context);
        this.clazz = getGenericClass();
    }

    /**
     * 获取泛型类型
     *
     * @return 泛型类型
     */
    private Class<? extends Serializable> getGenericClass() {
        Type genType = this.getClass().getGenericSuperclass();
        Type generic = ((ParameterizedType) genType).getActualTypeArguments()[0];
        if (!(generic instanceof Class))//泛型为array类型
            try {
                Field mArgs = generic.getClass().getDeclaredField("args");
                mArgs.setAccessible(true);
                Object o = mArgs.get(generic);

                Field mTypes = o.getClass().getDeclaredField("types");
                mTypes.setAccessible(true);
                ArrayList list = (ArrayList) mTypes.get(o);

                Object o1 = list.get(0);
                Field mRawType = o1.getClass().getDeclaredField("rawType");
                mRawType.setAccessible(true);
                return (Class<? extends Serializable>) mRawType.get(o1);
            } catch (Exception e) {
                Logger.e("获取泛型类型错误.", e);
                return null;
            }
        else//泛型为object类型
            return (Class<? extends Serializable>) generic;
    }

    /**
     * 网络请求成功
     *
     * @param url    网络地址
     * @param result 请求结果
     */
    @Override
    public final void onSuccess(String url, String result) {
        processData(url, result, true);
    }

    /**
     * 缓存请求成功
     *
     * @param url    网络地址
     * @param result 请求结果
     */
    @Override
    public final void onCacheSuccess(String url, String result) {
        processData(url, result, false);
    }

    /**
     * 处理数据
     *
     * @param url       url
     * @param result    请求返回结果
     * @param isNetwork 是否是网络请求
     */
    private void processData(String url, String result, boolean isNetwork) {
        if (clazz.equals(String.class))//传递的泛型类型为String时,直接输出到结果集
            if (isNetwork)
                onSuccess(url, (T) result);
            else
                onCacheSuccess(url, (T) result);
        JsonUtils.JsonType type = JsonUtils.getJSONType(result);
        if (clazz == null) return;//获取泛型类型错误
        switch (type) {
            case JSON_TYPE_OBJECT:
                if (isNetwork)
                    onSuccess(url, (T) JSON.parseObject(result, clazz));
                else
                    onCacheSuccess(url, (T) JSON.parseObject(result, clazz));
                break;
            case JSON_TYPE_ARRAY:
                if (isNetwork)
                    onSuccess(url, (T) JSON.parseArray(result, clazz));
                else
                    onCacheSuccess(url, (T) JSON.parseArray(result, clazz));
                break;
            case JSON_TYPE_ERROR:
                onFailure(url, -200, "data数据返回错误");
                Logger.e(JsonCallback.TAG, "result=" + result);
                break;
        }
    }

    /**
     * 网络请求成功回调
     *
     * @param url    网络地址
     * @param result 请求结果
     */
    public abstract void onSuccess(String url, T result);

    /**
     * 缓存请求成功回调
     *
     * @param url    网络地址
     * @param result 请求结果
     */
    public void onCacheSuccess(String url, T result) {

    }

    /**
     * 超时链接回调
     */
    @Override
    public void onTimeout() {
        mLoading.dismiss();
    }
}
