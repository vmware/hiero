/*
 * Copyright (c) 2018 VMware Inc. All Rights Reserved.
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

package org.hillview.sketches;

import org.hillview.dataset.api.IJson;
import org.hillview.dataset.api.ISketch;
import org.hillview.storage.IFileLoader;
import org.hillview.utils.Converters;

import javax.annotation.Nullable;

public class FileSizeSketch implements ISketch<IFileLoader, FileSizeSketch.Info> {
    @Override
    public Info create(IFileLoader data) {
        return new Info(1, data.getSizeInBytes());
    }

    @Nullable
    @Override
    public Info zero() {
        return new Info();
    }

    @Nullable
    @Override
    public Info add(@Nullable Info left, @Nullable Info right) {
        Converters.checkNull(left);
        Converters.checkNull(right);
        return new Info(left.fileCount + right.fileCount, left.totalSize + right.totalSize);
    }

    /**
     * Result produced by the FileSizeSketch.
     */
    public static class Info implements IJson {
        /**
         * Total number of files.
         */
        public final int fileCount;
        /**
         * Total bytes in all the files.
         */
        public final long totalSize;

        public Info(int count, long size) {
            this.fileCount = count;
            this.totalSize = size;
        }

        public Info() {
            this(0, 0);
        }
    }
}
