/*
 * Copyright 2015 John D. Ament
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.utkast.ozark;

import io.undertow.servlet.api.ClassIntrospecter;
import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.util.DefaultClassIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

/**
 * CDI aware Undertow {@link ClassIntrospecter} implementation.
 *
 * @author Libor Kramoliš
 */
class CdiClassIntrospecter implements ClassIntrospecter {

    static final ClassIntrospecter INSTANCE = new CdiClassIntrospecter();
    private static final Logger LOG = LoggerFactory.getLogger(CdiClassIntrospecter.class);

    private CdiClassIntrospecter() {
    }

    @Override
    public <T> InstanceFactory<T> createInstanceFactory(Class<T> clazz) throws NoSuchMethodException {
        LOG.trace("createInstanceFactory: {}", clazz);
        Instance<T> inst = CDI.current().select(clazz);
        if (inst.isUnsatisfied() || inst.isAmbiguous()) {
            return DefaultClassIntrospector.INSTANCE.createInstanceFactory(clazz);
        } else {
            return new CdiInstanceFactory<>(inst);
        }
    }

    //
    // class CDIInstanceFactory
    //

    private static class CdiInstanceFactory<T> implements InstanceFactory<T> {
        private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CdiInstanceFactory.class);

        private final Instance<T> inst;

        CdiInstanceFactory(Instance<T> inst) {
            this.inst = inst;
        }

        @Override
        public InstanceHandle<T> createInstance() throws InstantiationException {
            LOG.trace("createInstance: {}", inst);
            return new CdiInstanceHandler<>(inst);
        }

    } // class CDIInstanceFactory

    //
    // class CDIInstanceHandler
    //

    private static class CdiInstanceHandler<T> implements InstanceHandle<T> {
        private static final Logger LOG = LoggerFactory.getLogger(CdiInstanceHandler.class);

        private final Instance<T> inst;
        private T found = null;

        CdiInstanceHandler(Instance<T> inst) {
            this.inst = inst;
        }

        @Override
        public T getInstance() {
            if (this.found == null) {
                this.found = inst.get();
            }
            LOG.trace("getInstance [ {} ] >> {}", inst, found);
            return this.found;
        }

        @Override
        public void release() {
            inst.destroy(this.found);
        }

    } // class CDIInstanceHandler

}
