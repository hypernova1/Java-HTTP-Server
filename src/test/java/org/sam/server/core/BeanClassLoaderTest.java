package org.sam.server.core;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by melchor
 * Date: 2020/08/09
 * Time: 10:20 PM
 */
class BeanClassLoaderTest {

    static class TestBean {

        public String foo() {
            return "";
        }
    }

    @Test
    void test() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<?>[] constructors = TestBean.class.getConstructors();
        System.out.println(constructors.length);
        for (Constructor<?> constructor : constructors) {
            Parameter[] parameterTypes = constructor.getParameters();
            for (Parameter parameterType : parameterTypes) {
                System.out.println(parameterType.getName());
            }


            Method[] declaredMethods = TestBean.class.getDeclaredMethods();
            for (Method declaredMethod : declaredMethods) {
                System.out.println(declaredMethod.getName());
            }
        }

        TestBean testBean = TestBean.class.getDeclaredConstructor(null).newInstance();
        System.out.println(testBean);

        System.out.println(TestBean.class.getSimpleName());
    }

}