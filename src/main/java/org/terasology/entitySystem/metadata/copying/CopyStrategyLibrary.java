/*
 * Copyright 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.entitySystem.metadata.copying;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.entitySystem.metadata.ClassMetadata;
import org.terasology.entitySystem.metadata.MappedContainer;
import org.terasology.entitySystem.metadata.copying.strategy.Color4fCopyStrategy;
import org.terasology.entitySystem.metadata.copying.strategy.ListCopyStrategy;
import org.terasology.entitySystem.metadata.copying.strategy.MapCopyStrategy;
import org.terasology.entitySystem.metadata.copying.strategy.MappedContainerCopyStrategy;
import org.terasology.entitySystem.metadata.copying.strategy.Quat4fCopyStrategy;
import org.terasology.entitySystem.metadata.copying.strategy.SetCopyStrategy;
import org.terasology.entitySystem.metadata.copying.strategy.Vector2fCopyStrategy;
import org.terasology.entitySystem.metadata.copying.strategy.Vector3fCopyStrategy;
import org.terasology.entitySystem.metadata.copying.strategy.Vector3iCopyStrategy;
import org.terasology.entitySystem.metadata.copying.strategy.Vector4fCopyStrategy;
import org.terasology.entitySystem.metadata.internal.ClassMetadataImpl;
import org.terasology.entitySystem.metadata.reflect.ReflectFactory;
import org.terasology.math.Vector3i;
import org.terasology.utilities.ReflectionUtil;

import javax.vecmath.Color4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Immortius
 */
public class CopyStrategyLibrary {
    private static final Logger logger = LoggerFactory.getLogger(CopyStrategyLibrary.class);

    private Map<Class<?>, CopyStrategy<?>> strategies = Maps.newHashMap();
    private CopyStrategy<?> defaultStrategy = new ReturnAsIsStrategy();
    private ReflectFactory reflectFactory;

    public CopyStrategyLibrary(ReflectFactory reflectFactory) {
        this.reflectFactory = reflectFactory;
    }

    public static CopyStrategyLibrary create(ReflectFactory factory) {
        CopyStrategyLibrary library = new CopyStrategyLibrary(factory);
        library.register(Color4f.class, new Color4fCopyStrategy());
        library.register(Quat4f.class, new Quat4fCopyStrategy());
        library.register(Vector2f.class, new Vector2fCopyStrategy());
        library.register(Vector3f.class, new Vector3fCopyStrategy());
        library.register(Vector4f.class, new Vector4fCopyStrategy());
        library.register(Vector3i.class, new Vector3iCopyStrategy());
        return library;
    }

    public <T> void register(Class<T> type, CopyStrategy<T> strategy) {
        strategies.put(type, strategy);
    }

    // TODO: Consider CopyStrategyFactory system for Collections and similar
    public CopyStrategy<?> getStrategy(Type genericType) {
        Class<?> typeClass = ReflectionUtil.getClassOfType(genericType);
        if (typeClass == null) {
            logger.error("Cannot obtain class for type {}, using default strategy", genericType);
            return defaultStrategy;
        }

        if (List.class.isAssignableFrom(typeClass)) {
            // For lists, create the handler for the contained type and wrap in a list type handler
            Type parameter = ReflectionUtil.getTypeParameter(genericType, 0);
            if (parameter != null) {
                CopyStrategy<?> contentStrategy = getStrategy(parameter);
                return new ListCopyStrategy<>(contentStrategy);
            }
            logger.error("List field is not parametrized - using default strategy");
            return new ListCopyStrategy<>(defaultStrategy);

        } else if (Set.class.isAssignableFrom(typeClass)) {
            // For sets:
            Type parameter = ReflectionUtil.getTypeParameter(genericType, 0);
            if (parameter != null) {
                CopyStrategy<?> contentStrategy = getStrategy(parameter);
                return new SetCopyStrategy<>(contentStrategy);
            }
            logger.error("Set field is not parametrized - using default strategy");
            return new SetCopyStrategy<>(defaultStrategy);

        } else if (Map.class.isAssignableFrom(typeClass)) {
            // For Maps, create the handler for the value type
            Type keyParameter = ReflectionUtil.getTypeParameter(genericType, 0);
            CopyStrategy<?> keyStrategy;
            if (keyParameter != null) {
                keyStrategy = getStrategy(keyParameter);
            } else {
                logger.error("Map field is missing key parameter - using default strategy");
                keyStrategy = defaultStrategy;
            }

            Type valueParameter = ReflectionUtil.getTypeParameter(genericType, 1);
            CopyStrategy<?> valueStrategy;
            if (valueParameter != null) {
                valueStrategy = getStrategy(valueParameter);
            } else {
                logger.error("Map field is missing value parameter - using default strategy");
                valueStrategy = defaultStrategy;
            }
            return new MapCopyStrategy<>(keyStrategy, valueStrategy);

        } else if (strategies.containsKey(typeClass)) {
            // For known types, just use the handler
            return strategies.get(typeClass);

        } else if (typeClass.getAnnotation(MappedContainer.class) != null) {
            if (Modifier.isAbstract(typeClass.getModifiers())
                    || typeClass.isLocalClass()
                    || (typeClass.isMemberClass() && !Modifier.isStatic(typeClass.getModifiers()))) {
                logger.error("Type {} is not a valid mapped class", typeClass);
                return defaultStrategy;
            }

            try {
                ClassMetadata<?> classMetadata = new ClassMetadataImpl<>(typeClass, this, reflectFactory, typeClass.getSimpleName());
                return new MappedContainerCopyStrategy<>(classMetadata);
            } catch (NoSuchMethodException e) {
                logger.error("Unable to create copy strategy for field of type {}: no publicly accessible default constructor", typeClass.getSimpleName());
                return defaultStrategy;
            }
        } else {
            logger.debug("Using default copy strategy for {}", typeClass);
            strategies.put(typeClass, defaultStrategy);
            return defaultStrategy;
        }
    }

    private static class ReturnAsIsStrategy<T> implements CopyStrategy<T> {

        @Override
        public T copy(T value) {
            return value;
        }
    }
}
