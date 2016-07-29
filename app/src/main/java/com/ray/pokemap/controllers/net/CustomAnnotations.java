package com.ray.pokemap.controllers.net;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Raymond on 29/07/2016.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD) //can use in method only.
@interface CustomAnnotations {
}


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD) //can use in method only.
@interface testing{

}