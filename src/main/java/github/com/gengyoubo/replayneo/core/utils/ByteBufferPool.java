package github.com.gengyoubo.replayneo.core.utils;

import com.google.common.collect.Maps;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ByteBufferPool {
    private static final Map<Integer, List<SoftReference<ByteBuffer>>> bufferPool = Maps.newHashMap();

    public static synchronized ByteBuffer allocate(int size) {
        List<SoftReference<ByteBuffer>> available = bufferPool.get(size);
        if (available != null) {
            Iterator<SoftReference<ByteBuffer>> iter = available.iterator();
            try {
                while (iter.hasNext()) {
                    SoftReference<ByteBuffer> reference = iter.next();
                    ByteBuffer buffer = reference.get();
                    iter.remove();
                    if (buffer != null) {
                        return buffer;
                    }
                }
            } finally {
                if (!iter.hasNext()) {
                    bufferPool.remove(size);
                }
            }
        }
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static synchronized void release(ByteBuffer buffer) {
        buffer.clear();
        int size = buffer.capacity();
        List<SoftReference<ByteBuffer>> available = bufferPool.computeIfAbsent(size, k -> new LinkedList<>());
        available.add(new SoftReference<>(buffer));
    }
}
