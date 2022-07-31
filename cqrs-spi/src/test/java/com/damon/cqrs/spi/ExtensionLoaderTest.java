/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.damon.cqrs.spi;

import com.damon.cqrs.spi.fixture.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The type Extension loader test.
 */
public final class ExtensionLoaderTest {

    /**
     * Test spi.
     */
    @Test
    public void testSPI() {
        JdbcSPI jdbcSPI = ExtensionLoader.getExtensionLoader(JdbcSPI.class).getJoin("mysql");
        assertThat(jdbcSPI.getClass().getName(), is(MysqlSPI.class.getName()));
    }

    /**
     * Test spi list.
     */
    @Test
    public void testSPIList() {
        List<ListSPI> joins = ExtensionLoader.getExtensionLoader(ListSPI.class).getJoins();
        assertEquals(joins.size(), 2);
        assertThat(joins.get(0).getClass().getName(), is(ArrayListSPI.class.getName()));
        assertThat(joins.get(1).getClass().getName(), is(LinkedListSPI.class.getName()));
    }

    /**
     * Test spi empty spi.
     */
    @Test
    public void testSPIEmptySPI() {
        List<EmptySPI> joins = ExtensionLoader.getExtensionLoader(EmptySPI.class).getJoins();
        assertEquals(joins.size(), 0);
    }

    /**
     * test SPI has default value case.
     */
    @Test
    public void testSPIGetDefaultJoin() {
        HasDefaultSPI spi = ExtensionLoader.getExtensionLoader(HasDefaultSPI.class).getDefaultJoin();
        assert spi != null;
        assertThat(spi.getClass().getName(), is(SubHasDefaultSPI.class.getName()));
    }

    /**
     * test SPI no default value case.
     */
    @Test
    public void testSPINoDefaultJoin() {
        JdbcSPI jdbcSPI = ExtensionLoader.getExtensionLoader(JdbcSPI.class).getDefaultJoin();
        assertNull(jdbcSPI);
    }

    /**
     * test ExtensionLoader.getJoin() blank name param case.
     */
    @Test
    public void testSPIGetJoinNameIsBlank() {
        try {
            ExtensionLoader.getExtensionLoader(JdbcSPI.class).getJoin("");
            fail();
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), containsString("get join name is null"));
        }
    }

    /**
     * test ExtensionLoader.getExtensionLoader() null param case.
     */
    @Test
    public void testGetExtensionLoaderIsNull() {
        try {
            ExtensionLoader.getExtensionLoader(null);
            fail();
        } catch (NullPointerException expected) {
            assertThat(expected.getMessage(), containsString("extension clazz is null"));
        }
    }

    /**
     * test ExtensionLoader.getExtensionLoader() param is not interface case.
     */
    @Test
    public void testGetExtensionLoaderNotInterface() {
        try {
            ExtensionLoader.getExtensionLoader(ExtensionLoaderTest.class);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    containsString("extension clazz (class com.damon.cqrs.spi.ExtensionLoaderTest) is not interface!"));
        }
    }

    /**
     * test ExtensionLoader.getExtensionLoader() param is not have SPI annotation case.
     */
    @Test
    public void testGetExtensionLoaderNotSpiAnnotation() {
        try {
            ExtensionLoader.getExtensionLoader(NopSPI.class);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    containsString("extension clazz (interface com.damon.cqrs.spi.fixture.NopSPI) without @interface com.damon.cqrs.spi.SPI Annotation"));
        }
    }

    /**
     * test ExtensionLoader.getJoin() param nonentity SPI name case.
     */
    @Test
    public void testGetExtensionLoaderNonentitySPIName() {
        try {
            ExtensionLoader.getExtensionLoader(JdbcSPI.class).getJoin("nonentitySPIName");
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("name is error"));
        }
    }

    /**
     * test ExtensionLoader.getJoin() param name not interface subType case.
     */
    @Test
    public void testGetExtensionLoaderSPISubTypeNotMatchInterface() {
        try {
            ExtensionLoader.getExtensionLoader(NotMatchSPI.class).getJoin("subNoJoinSPI");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                    containsString("load extension resources error,class com.damon.cqrs.spi.fixture.SubNoJoinSPI subtype is not of interface com.damon.cqrs.spi.fixture.NotMatchSPI"));
        }
    }

    /**
     * test ExtensionLoader.getJoin() param name no class match case.
     */
    @Test
    public void testGetExtensionLoaderNoClassMatchSPI() {
        try {
            ExtensionLoader.getExtensionLoader(NoClassMatchSPI.class).getJoin("subNoClassMatchSPI");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("load extension resources error"));
        }
    }

    /**
     * test ExtensionLoader.getJoin() param no join case.
     */
    @Test
    public void testGetExtensionLoaderNoJoinSPI() {
        try {
            ExtensionLoader.getExtensionLoader(NoJoinSPI.class).getJoin("subNoJoinSPI");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(),
                    containsString("load extension resources error,class com.damon.cqrs.spi.fixture.SubNoJoinSPI without @interface com.damon.cqrs.spi.Join annotation"));
        }
    }

    /**
     * test ExtensionLoader.getJoin() param SPI class can not instantiated case.
     */
    @Test
    public void testGetExtensionLoaderCanNotInstantiatedSPI() {
        try {
            ExtensionLoader.getExtensionLoader(JdbcSPI.class).getJoin("canNotInstantiated");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString(
                    "Extension instance(name: canNotInstantiated, class: class com.damon.cqrs.spi.fixture.CanNotInstantiatedSPI)"));
        }
    }

    /**
     * test loadClass duplicate class case.
     *
     * @throws NoSuchMethodException     the no such method exception
     * @throws InvocationTargetException the invocation target exception
     * @throws IllegalAccessException    the illegal access exception
     */
    @Test
    public void testLoadClassDuplicateKey() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method loadClassMethod = getLoadClassMethod();
        ExtensionLoader<JdbcSPI> extensionLoader = ExtensionLoader.getExtensionLoader(JdbcSPI.class);
        Map<String, Class<?>> classes = new HashMap<>();
        loadClassMethod.invoke(extensionLoader, classes, "mysql", "com.damon.cqrs.spi.fixture.MysqlSPI");
        try {
            loadClassMethod.invoke(extensionLoader, classes, "mysql", "com.damon.cqrs.spi.fixture.OracleSPI");
            fail();
        } catch (InvocationTargetException expect) {
            assertThat(expect.getTargetException().getMessage(), containsString(
                    "load extension resources error,Duplicate class com.damon.cqrs.spi.fixture.JdbcSPI name mysql on "
                            + "com.damon.cqrs.spi.fixture.MysqlSPI or com.damon.cqrs.spi.fixture.OracleSPI"));
        }
    }

    /**
     * test loadResources url IO Exception case.
     *
     * @throws NoSuchMethodException  the no such method exception
     * @throws MalformedURLException  the malformed url exception
     * @throws IllegalAccessException the illegal access exception
     */
    @Test
    public void loadResourcesIOException()
            throws NoSuchMethodException, MalformedURLException, IllegalAccessException {
        Method loadResourcesMethod = getLoadResources();
        ExtensionLoader<JdbcSPI> extensionLoader = ExtensionLoader.getExtensionLoader(JdbcSPI.class);
        try {
            loadResourcesMethod.invoke(extensionLoader, new HashMap<>(),
                    new URL("file:/com.damon.cqrs.spi.fixture.NoExistSPI"));
            fail();
        } catch (InvocationTargetException expect) {
            assertThat(expect.getTargetException().getMessage(), containsString("load extension resources error"));
        }
    }

    /**
     * get private loadClass method.
     */
    private Method getLoadClassMethod() throws NoSuchMethodException {
        Method method = ExtensionLoader.class.getDeclaredMethod("loadClass", Map.class, String.class, String.class);
        method.setAccessible(true);
        return method;
    }

    /**
     * get private loadResources method.
     */
    private Method getLoadResources() throws NoSuchMethodException {
        Method method = ExtensionLoader.class.getDeclaredMethod("loadResources", Map.class, URL.class);
        method.setAccessible(true);
        return method;
    }
}
