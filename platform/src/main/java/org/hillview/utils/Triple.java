/*
 * Copyright (c) 2020 VMware Inc. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hillview.utils;

import org.hillview.dataset.api.IJson;

import javax.annotation.Nullable;
import java.util.Objects;

public class Triple<T, S, V> implements IJson /* Serializable implied by IJSon */ {
    static final long serialVersionUID = 1;

    @Nullable
    public final T first;
    @Nullable
    public final S second;
    @Nullable
    public final V third;

    public Triple(@Nullable final T first, @Nullable final S second, @Nullable final V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;

        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;

        if (!Objects.equals(this.first, triple.first)) return false;
        if (!Objects.equals(this.second, triple.second)) return false;
        return Objects.equals(this.third, triple.third);
    }

    @Override
    public int hashCode() {
        int result = (this.first != null) ? this.first.hashCode() : 0;
        result = (31 * result) + ((this.second != null) ? this.second.hashCode() : 0);
        result = (31 * result) + ((this.third != null) ? this.third.hashCode() : 0);
        return result;
    }
}
