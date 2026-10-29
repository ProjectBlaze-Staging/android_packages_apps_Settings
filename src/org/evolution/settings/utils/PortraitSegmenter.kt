/*
 * Copyright (C) 2025-2026 AxionOS
 * Copyright (C) 2024-2026 Lunaris OS
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

package org.evolution.settings.utils

import android.content.Context
import android.graphics.Bitmap
import android.util.Log

private const val TAG = "PortraitSegmenter"

class PortraitSegmenter(private val context: Context) {

    fun init() {
        Log.d(TAG, "PortraitSegmenter initialized (stub)")
    }

    fun isReady(): Boolean = false

    fun segment(bitmap: Bitmap): Bitmap? = null

    fun release() {
        Log.d(TAG, "PortraitSegmenter released")
    }
}
