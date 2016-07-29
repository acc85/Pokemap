package com.ray.pokemap.controllers.net;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by Raymond on 29/07/2016.
 */

public class CustomFactory extends Converter.Factory {

    public static CustomFactory create() {
        return new CustomFactory();
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {

//        return null;
        return super.responseBodyConverter(type, annotations, retrofit);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
//        return null;
        return new CustomRequestBodyConverter();
    }

    @Override
    public Converter<?, String> stringConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
//        return null;
        return super.stringConverter(type, annotations, retrofit);
    }


    static class CustomRequestBodyConverter<T> implements Converter<T, RequestBody> {

//        final String method;
//        final Converter<T, RequestBody> delegate;

        CustomRequestBodyConverter() {

        }


        @Override
        public RequestBody convert(T value) throws IOException {
            return null;
        }
    }
}
