/*
 * Copyright (C) 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.pustike.inject;

import org.atinject.tck.Tck;
import org.atinject.tck.auto.Car;
import org.atinject.tck.auto.Convertible;
import org.atinject.tck.auto.Drivers;
import org.atinject.tck.auto.DriversSeat;
import org.atinject.tck.auto.Engine;
import org.atinject.tck.auto.FuelTank;
import org.atinject.tck.auto.Seat;
import org.atinject.tck.auto.Tire;
import org.atinject.tck.auto.V8Engine;
import org.atinject.tck.auto.accessories.Cupholder;
import org.atinject.tck.auto.accessories.SpareTire;
import org.junit.Assert;
import org.junit.Test;

import io.github.pustike.inject.bind.Module;
import junit.framework.TestResult;
import junit.textui.TestRunner;

/**
 * Test that runs <a href="https://javax-inject.github.io/javax-inject" target="_blank"> JSR-330</a>
 * (Dependency Injection for Java) TCK (Technology Compatibility Kit).
 * </p>
 *
 * <p>This class follows
 * <a href="https://github.com/google/guice/blob/master/core/test/com/googlecode/guice/GuiceTck.java" target="_blank">
 * GuiceTCK</a> with modifications to use the Pustike Inject API for module definition and injector creation.
 * </p>
 */
public class TckTest {
    @Test
    public void testAtInjectTck() {
        Module module = binder -> {
            binder.bind(Car.class).to(Convertible.class);
            binder.bind(Seat.class).annotatedWith(Drivers.class).to(DriversSeat.class);
            binder.bind(Seat.class);
            binder.bind(Tire.class);
            binder.bind(Engine.class).to(V8Engine.class);
            binder.bind(Tire.class).named("spare").to(SpareTire.class);
            binder.bind(Cupholder.class);
            binder.bind(SpareTire.class);
            binder.bind(FuelTank.class);
        };
        Injector injector = Injectors.create(module);
        Car car = injector.getInstance(Car.class);
        TestResult testResult = TestRunner.run(Tck.testsFor(car, true, true));
        Assert.assertTrue(testResult.wasSuccessful());
    }
}
