/*
 * Copyright 2020 Sergey Shadchin (sergei.shadchin@gmail.com)
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
package ru.mecotrade.kidtracker.util;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import net.coobird.thumbnailator.resizers.configurations.ScalingMode;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ThumbUtils {

    private final static String DATA = "data:";

    private final static String BASE_64 = ";base64,";

    public static String resize(String thumb, int size) throws IOException {
        String contentType = getContentType(thumb);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(getBytes(thumb)))
                .crop(Positions.CENTER)
                .size(size, size)
                .scalingMode(ScalingMode.PROGRESSIVE_BILINEAR)
                .outputQuality(1)
                .toOutputStream(output);
        return DATA + contentType + BASE_64 + DatatypeConverter.printBase64Binary(output.toByteArray());
    }

    private static byte[] getBytes(String thumb) {
        return DatatypeConverter.parseBase64Binary(thumb.substring(thumb.indexOf(BASE_64) + BASE_64.length()));
    }

    private static String getContentType(String thumb) {
        return thumb.substring(DATA.length(), thumb.indexOf(BASE_64));
    }
}
