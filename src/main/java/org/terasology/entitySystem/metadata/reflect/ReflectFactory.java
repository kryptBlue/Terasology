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
package org.terasology.entitySystem.metadata.reflect;

import java.lang.reflect.Field;

/**
 * @author Immortius
 */
public interface ReflectFactory {

    <T> ObjectConstructor<T> createConstructor(Class<T> type) throws NoSuchMethodException;

    <T> FieldAccessor<T, ?> createFieldAccessor(Class<T> ownerType, Field field);

    <T, U> FieldAccessor<T, U> createFieldAccessor(Class<T> ownerType, Field field, Class<U> fieldType);
}
